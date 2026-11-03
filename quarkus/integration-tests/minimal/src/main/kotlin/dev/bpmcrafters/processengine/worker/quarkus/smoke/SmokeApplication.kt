package dev.bpmcrafters.processengine.worker.quarkus.smoke

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.quarkus.ProcessEngineWorkerRegistration
import dev.bpmcrafters.processengine.worker.idempotency.IdempotencyRegistry
import dev.bpmcrafters.processengine.worker.idempotency.NoOpIdempotencyRegistry
import dev.bpmcrafters.processengine.worker.registrar.ProcessEngineWorkerMetrics
import dev.bpmcrafters.processengine.worker.registrar.metrics.ProcessEngineWorkerMetricsNoOp
import dev.bpmcrafters.processengine.worker.transaction.TransactionalExecutor
import io.quarkus.arc.ClientProxy
import io.quarkus.runtime.Quarkus
import io.quarkus.runtime.QuarkusApplication
import io.quarkus.runtime.annotations.QuarkusMain
import jakarta.inject.Inject

@QuarkusMain
class SmokeApplication : QuarkusApplication {
  @field:Inject lateinit var registration: ProcessEngineWorkerRegistration
  @field:Inject lateinit var executor: TransactionalExecutor
  @field:Inject lateinit var registry: IdempotencyRegistry
  @field:Inject lateinit var metrics: ProcessEngineWorkerMetrics
  @field:Inject lateinit var subscriptions: SmokeApis.Subscriptions
  @field:Inject lateinit var completion: SmokeApis.Completion

  override fun run(vararg args: String): Int {
    requireAbsent("io.quarkus.narayana.jta.QuarkusTransaction")
    requireAbsent("io.micrometer.core.instrument.MeterRegistry")
    requireAbsent("jakarta.persistence.EntityManager")
    requireAbsent("dev.bpmcrafters.processengine.worker.idempotency.TaskLogEntry")
    requireAbsent("org.hibernate.Session")
    verifyFallbacks()
    check(registration.taskSubscriptions.size == 1) { "Expected one registered smoke worker" }
    subscriptions.deliver()
    check(!completion.failed && completion.payload == mapOf("smoke" to "completed")) { "Smoke worker invocation/completion failed" }
    println("PROCESS-ENGINE-WORKER: packaged JVM smoke passed")
    return 0
  }

  private fun verifyFallbacks() {
    check(ClientProxy.unwrap(executor) === TransactionalExecutor.NONE &&
      ClientProxy.unwrap(registry) is NoOpIdempotencyRegistry &&
      ClientProxy.unwrap(metrics) === ProcessEngineWorkerMetricsNoOp) {
      "Absent optional capabilities did not select the default fallback beans"
    }
  }

  private fun requireAbsent(className: String) {
    try {
      Class.forName(className)
      error("Unexpected optional dependency: $className")
    } catch (expected: ClassNotFoundException) {
      // Check absence in the packaged JVM, not an extension test-runner parent classloader.
    }
  }

  open class Worker {
    @ProcessEngineWorker("enabled-smoke")
    open fun work(): Map<String, Any?> = mapOf("smoke" to "completed")
  }

  companion object {
    @JvmStatic fun main(args: Array<String>) { Quarkus.run(SmokeApplication::class.java, *args) }
  }
}
