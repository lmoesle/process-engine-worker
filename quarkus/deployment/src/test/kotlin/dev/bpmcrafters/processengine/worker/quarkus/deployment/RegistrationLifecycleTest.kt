package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.quarkus.ProcessEngineWorkerRegistration
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.SimpleWorker
import dev.bpmcrafters.processengineapi.Empty
import dev.bpmcrafters.processengineapi.task.*
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.test.QuarkusExtensionTest
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.Dependent
import jakarta.enterprise.event.Event
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicInteger

class RegistrationLifecycleTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest().setArchiveProducer {
      TestSupport.archiveWithoutAdapters(SimpleWorker::class.java, DependentWorker::class.java, ApiProducer::class.java,
        FailingApi::class.java, ShutdownObserver::class.java, InMemoryTaskSubscriptionApi::class.java, RecordingServiceTaskCompletionApi::class.java)
    })
  }
  @Singleton class ApiProducer {
    @Produces @Singleton fun subscriptions() = FailingApi()
    @Produces @Singleton fun completion() = RecordingServiceTaskCompletionApi()
  }
  open class FailingApi : InMemoryTaskSubscriptionApi() {
    var attempts = 0
    var failAt = -1
    var failCleanup = false

    override fun subscribeForTask(cmd: SubscribeForTaskCmd): CompletableFuture<TaskSubscription> {
      if (++attempts == failAt) return CompletableFuture.failedFuture(IllegalStateException("deliberate subscription failure"))
      return super.subscribeForTask(cmd)
    }
    override fun unsubscribe(cmd: UnsubscribeFromTaskCmd): CompletableFuture<Empty> {
      if (failCleanup) return CompletableFuture.failedFuture(IllegalStateException("deliberate cleanup failure"))
      return super.unsubscribe(cmd)
    }
  }
  @Dependent open class DependentWorker {
    companion object { @JvmField val DESTROYED = AtomicInteger() }
    @ProcessEngineWorker("dependent-lifecycle") open fun work() {}
    @PreDestroy fun destroy() { DESTROYED.incrementAndGet() }
  }
  @Singleton class ShutdownObserver {
    @field:Inject lateinit var api: FailingApi
    var remaining = -1
    fun stop(@Observes event: ShutdownEvent) { remaining = api.subscriptions.size }
  }
  @field:Inject lateinit var registration: ProcessEngineWorkerRegistration
  @field:Inject lateinit var api: FailingApi
  @field:Inject lateinit var shutdown: Event<ShutdownEvent>
  @field:Inject lateinit var observer: ShutdownObserver

  @BeforeEach fun reset() {
    api.failCleanup = false
    registration.onStop(ShutdownEvent())
    api.attempts = 0
    api.failAt = -1
  }

  @Test fun `repeated registration and shutdown are idempotent and ordered`() {
    registration.register(listOf(SimpleWorker::class.java))
    registration.register(listOf(SimpleWorker::class.java, SimpleWorker::class.java))
    assertThat(api.subscriptions).hasSize(4)
    shutdown.fire(ShutdownEvent())
    assertThat(observer.remaining).isZero()
    registration.onStop(ShutdownEvent())
    assertThat(registration.taskSubscriptions).isEmpty()
    registration.register(listOf(SimpleWorker::class.java))
    assertThat(api.subscriptions).hasSize(4)
  }

  @Test fun `ignores requested classes without any worker methods`() {
    registration.register(listOf(String::class.java))
    assertThat(api.subscriptions).isEmpty()
    assertThat(registration.taskSubscriptions).isEmpty()
    assertThat(api.attempts).isZero()
  }

  @Test fun `failure within one class cleans already created subscriptions and allows retry`() {
    api.failAt = 2
    assertThatThrownBy { registration.register(listOf(SimpleWorker::class.java)) }.hasRootCauseMessage("deliberate subscription failure")
    assertThat(api.subscriptions).isEmpty()
    assertThat(registration.taskSubscriptions).isEmpty()
    api.failAt = -1
    registration.register(listOf(SimpleWorker::class.java))
    assertThat(api.subscriptions).hasSize(4)
  }

  @Test fun `failed cleanup remains tracked for retry`() {
    registration.register(listOf(SimpleWorker::class.java))
    api.failCleanup = true
    registration.onStop(ShutdownEvent())
    assertThat(registration.taskSubscriptions).hasSize(4)
    assertThatThrownBy { registration.register(listOf(SimpleWorker::class.java)) }.hasMessageContaining("failed cleanup")
    api.failCleanup = false
    registration.onStop(ShutdownEvent())
    assertThat(api.subscriptions).isEmpty()
    assertThat(registration.taskSubscriptions).isEmpty()
  }

  @Test fun `destroys the exact dependent bean only after successful unsubscription`() {
    val destroyedBefore = DependentWorker.DESTROYED.get()
    registration.register(listOf(DependentWorker::class.java))
    registration.register(listOf(DependentWorker::class.java))
    assertThat(api.subscriptions).hasSize(1)
    api.failCleanup = true
    registration.onStop(ShutdownEvent())
    assertThat(DependentWorker.DESTROYED.get()).isEqualTo(destroyedBefore)
    api.failCleanup = false
    registration.onStop(ShutdownEvent())
    registration.onStop(ShutdownEvent())
    assertThat(DependentWorker.DESTROYED.get()).isEqualTo(destroyedBefore + 1)
  }
}
