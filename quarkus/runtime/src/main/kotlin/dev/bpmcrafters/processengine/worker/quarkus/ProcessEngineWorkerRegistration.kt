package dev.bpmcrafters.processengine.worker.quarkus

import dev.bpmcrafters.processengine.worker.idempotency.IdempotencyRegistry
import dev.bpmcrafters.processengine.worker.registrar.ParameterResolver
import dev.bpmcrafters.processengine.worker.registrar.ProcessEngineWorkerMetrics
import dev.bpmcrafters.processengine.worker.registrar.ProcessEngineWorkerRegistrar
import dev.bpmcrafters.processengine.worker.registrar.ResultResolver
import dev.bpmcrafters.processengine.worker.registrar.VariableConverter
import dev.bpmcrafters.processengine.worker.registrar.getAnnotatedWorkers
import dev.bpmcrafters.processengine.worker.registrar.getTenantId
import dev.bpmcrafters.processengine.worker.registrar.getTopic
import dev.bpmcrafters.processengine.worker.transaction.AfterCommitHookAware
import dev.bpmcrafters.processengine.worker.transaction.TransactionalExecutor
import dev.bpmcrafters.processengine.worker.transaction.TransactionalMethodDetector
import dev.bpmcrafters.processengineapi.task.ServiceTaskCompletionApi
import dev.bpmcrafters.processengineapi.task.TaskSubscription
import dev.bpmcrafters.processengineapi.task.TaskSubscriptionApi
import dev.bpmcrafters.processengineapi.task.UnsubscribeFromTaskCmd
import io.github.oshai.kotlinlogging.KotlinLogging
import io.quarkus.arc.Arc
import io.quarkus.arc.ClientProxy
import io.quarkus.arc.InstanceHandle
import io.quarkus.arc.InjectableBean
import io.quarkus.runtime.ShutdownEvent
import jakarta.annotation.Priority
import jakarta.enterprise.event.Observes
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.Any as AnyQualifier
import jakarta.inject.Singleton
import jakarta.interceptor.Interceptor
import java.util.concurrent.CopyOnWriteArrayList

private val logger = KotlinLogging.logger {}

/**
 * Registers the process engine workers detected at build time with the core registrar during runtime initialization
 * (before any `StartupEvent` observer runs) and unsubscribes them on shutdown.
 *
 * All collaborators are resolved lazily via [Instance], so the application starts even if the process engine adapter is
 * not present, as long as the worker registration is disabled.
 * @since 0.8.6
 */
@Singleton
open class ProcessEngineWorkerRegistration(
  private val config: ProcessEngineWorkerConfig,
  private val taskSubscriptionApi: Instance<TaskSubscriptionApi>,
  private val taskCompletionApi: Instance<ServiceTaskCompletionApi>,
  private val variableConverter: Instance<VariableConverter>,
  private val parameterResolver: Instance<ParameterResolver>,
  private val resultResolver: Instance<ResultResolver>,
  private val transactionalExecutor: Instance<TransactionalExecutor>,
  private val metrics: Instance<ProcessEngineWorkerMetrics>,
  private val idempotencyRegistry: Instance<IdempotencyRegistry>,
) {

  private val subscriptions: MutableList<TaskSubscription> = CopyOnWriteArrayList()
  private val registeredClasses = mutableSetOf<Class<*>>()
  private val dependentHandles = mutableListOf<InstanceHandle<*>>()
  private var subscriptionApi: TaskSubscriptionApi? = null
  private var registrationFailed = false

  /**
   * Subscriptions created by the registration.
   */
  open val taskSubscriptions: List<TaskSubscription>
    get() = subscriptions.toList()

  /**
   * Registers the workers of the given bean classes.
   * @param workerClasses bean classes declaring worker methods.
   */
  @Synchronized
  open fun register(workerClasses: List<Class<*>>) {
    if (!config.enabled()) {
      logger.debug { "PROCESS-ENGINE-WORKER-020: Registration of process engine workers is disabled, skipping ${workerClasses.size} worker classes." }
      return
    }
    val pendingClasses = workerClasses.distinct().filter { it !in registeredClasses && it.getAnnotatedWorkers().isNotEmpty() }
    if (pendingClasses.isEmpty()) {
      logger.debug { "PROCESS-ENGINE-WORKER-020: No process engine workers detected." }
      return
    }
    check(!registrationFailed || subscriptions.isEmpty()) {
      "PROCESS-ENGINE-WORKER-024: Cannot retry registration while failed cleanup leaves subscriptions active."
    }
    validateEffectiveTopics(registeredClasses + pendingClasses)
    val registrar = createRegistrar()
    warnIfTransactionalWorkersWithoutTransactionManagement(pendingClasses)
    try {
      pendingClasses.forEach { workerClass -> registerClass(registrar, workerClass) }
      registrationFailed = false
    } catch (failure: Throwable) {
      registrationFailed = true
      unsubscribeAll()
      registeredClasses.clear()
      throw failure
    }
    logger.info { "PROCESS-ENGINE-WORKER-021: Registered ${subscriptions.size} process engine workers of ${workerClasses.size} worker classes." }
  }

  private fun registerClass(registrar: ProcessEngineWorkerRegistrar, workerClass: Class<*>) {
    val handles = Arc.container().listAll(workerClass, AnyQualifier.Literal.INSTANCE)
      .filter { it.bean.kind == InjectableBean.Kind.CLASS && it.bean.beanClass == workerClass }
    check(handles.size == 1) {
      "PROCESS-ENGINE-WORKER-022: Expected one exact class bean for worker ${workerClass.name}, found ${handles.size}."
    }
    val handle = handles.single()
    if (handle.bean.scope == Dependent::class.java) {
      dependentHandles.add(handle)
    }
    // Record each subscription immediately: registerWorkers returns only after every method succeeds.
    workerClass.getAnnotatedWorkers().forEach { method ->
      subscriptions.add(registrar.registerWorker(handle.get(), workerClass.simpleName, method))
    }
    registeredClasses.add(workerClass)
  }

  private fun validateEffectiveTopics(workerClasses: Collection<Class<*>>) {
    val defaultTenant = config.toConfiguration().tenantId
    val duplicates = workerClasses.flatMap { it.getAnnotatedWorkers() }
      .groupBy { it.getTopic() to (it.getTenantId() ?: defaultTenant) }
      .filterValues { it.size > 1 }
    check(duplicates.isEmpty()) {
      "PROCESS-ENGINE-WORKER-032: Multiple worker methods use the same effective topic and tenant: " +
        duplicates.entries.joinToString { (key, methods) -> "$key: ${methods.joinToString { "${it.declaringClass.name}#${it.name}" }}" }
    }
  }

  private fun createRegistrar(): ProcessEngineWorkerRegistrar {
    val subscriptionApi = resolve(taskSubscriptionApi, "TaskSubscriptionApi")
    val completionApi = resolve(taskCompletionApi, "ServiceTaskCompletionApi")
    this.subscriptionApi = subscriptionApi
    val executor = transactionalExecutor.get()
    val registry = idempotencyRegistry.get()
    // install the after-commit hook on the raw instance, the client proxy of an interface-typed producer does not expose it
    val rawRegistry = ClientProxy.unwrap(registry)
    if (rawRegistry is AfterCommitHookAware) {
      rawRegistry.afterCommitHook = executor
    }
    return ProcessEngineWorkerRegistrar(
      configuration = config.toConfiguration(),
      taskSubscriptionApi = subscriptionApi,
      taskCompletionApi = completionApi,
      variableConverter = variableConverter.get(),
      parameterResolver = parameterResolver.get(),
      resultResolver = resultResolver.get(),
      transactionalExecutor = executor,
      processEngineWorkerMetrics = metrics.get(),
      idempotencyRegistry = registry,
      transactionalMethodDetector = TransactionalMethodDetector.DEFAULT,
    )
  }

  private fun warnIfTransactionalWorkersWithoutTransactionManagement(workerClasses: List<Class<*>>) {
    if (ClientProxy.unwrap(transactionalExecutor.get()) !== TransactionalExecutor.NONE) {
      return
    }
    val transactionalWorkers = workerClasses
      .flatMap { it.getAnnotatedWorkers() }
      .filter { TransactionalMethodDetector.DEFAULT.isTransactional(it) }
    if (transactionalWorkers.isNotEmpty()) {
      logger.warn {
        "PROCESS-ENGINE-WORKER-025: Found ${transactionalWorkers.size} transactional process engine workers " +
          "(${transactionalWorkers.joinToString { "${it.declaringClass.simpleName}#${it.name}" }}), but no transaction management is available. " +
          "The registrar does not manage transactions; CDI transaction interceptors may still apply. " +
          "Add the 'quarkus-narayana-jta' extension (and do not replace the transactional executor) to enable registrar-managed transactions."
      }
    }
  }

  private fun <T : Any> resolve(instance: Instance<T>, name: String): T {
    check(instance.isResolvable) {
      if (instance.isAmbiguous) {
        "PROCESS-ENGINE-WORKER-023: Found multiple $name beans, but exactly one is required to register process engine workers."
      } else {
        "PROCESS-ENGINE-WORKER-023: Found no $name bean. Provide a process engine adapter implementing this API " +
          "and enable it, or disable the worker registration via '${dev.bpmcrafters.processengine.worker.configuration.ProcessEngineWorkerConfiguration.ENABLED_PROPERTY}=false'."
      }
    }
    return try {
      instance.get()
    } catch (e: RuntimeException) {
      throw IllegalStateException("PROCESS-ENGINE-WORKER-023: Could not resolve $name required to register process engine workers: ${e.message}", e)
    }
  }

  /**
   * Unsubscribes all registered workers on shutdown. Runs with an explicit priority before default-priority observers
   * of the process engine adapters, so the subscriptions are removed before the deliveries are closed.
   * @param event shutdown event.
   */
  @Synchronized
  open fun onStop(@Observes @Priority(Interceptor.Priority.APPLICATION + 100) event: ShutdownEvent) {
    unsubscribeAll()
    registeredClasses.clear()
    registrationFailed = subscriptions.isNotEmpty()
  }

  private fun unsubscribeAll() {
    if (subscriptions.isEmpty()) {
      destroyDependentWorkers()
      return
    }
    val api = subscriptionApi ?: return
    subscriptions.toList().asReversed().forEach { subscription ->
      try {
        api.unsubscribe(UnsubscribeFromTaskCmd(subscription)).get()
        subscriptions.remove(subscription)
      } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        logger.warn(e) { "PROCESS-ENGINE-WORKER-024: Interrupted while unsubscribing $subscription." }
      } catch (e: Exception) {
        logger.warn(e) { "PROCESS-ENGINE-WORKER-024: Could not unsubscribe $subscription on shutdown." }
      }
    }
    logger.debug { "PROCESS-ENGINE-WORKER-024: Worker cleanup finished; ${subscriptions.size} subscriptions remain." }
    if (subscriptions.isEmpty()) {
      destroyDependentWorkers()
    }
  }

  private fun destroyDependentWorkers() {
    dependentHandles.toList().asReversed().forEach { handle ->
      try {
        handle.destroy()
      } catch (e: RuntimeException) {
        logger.warn(e) { "PROCESS-ENGINE-WORKER-024: Could not destroy dependent worker bean." }
      }
    }
    dependentHandles.clear()
  }
}
