package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.ProcessEngineWorkerRegistration
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.SimpleWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.StartupObserver
import dev.bpmcrafters.processengineapi.CommonRestrictions
import dev.bpmcrafters.processengineapi.task.TaskType
import io.quarkus.arc.Arc
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class WorkerRegistrationTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(SimpleWorker::class.java, StartupObserver::class.java) }
      .overrideConfigKey("dev.bpm-crafters.process-api.worker.tenant-id", "default-tenant"))
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var startupObserver: StartupObserver
  @field:Inject lateinit var registration: ProcessEngineWorkerRegistration

  @Test fun `unannotated worker class is a bean`() {
    assertThat(Arc.container().instance(SimpleWorker::class.java).isAvailable).isTrue()
  }

  @Test fun `subscriptions are created with topic payload and restrictions`() {
    assertThat(subscriptionApi.subscriptions).hasSize(4)
    assertThat(registration.taskSubscriptions).hasSize(4)
    val a = subscriptionApi.getSubscription("topic-a")
    assertThat(a.taskType).isEqualTo(TaskType.EXTERNAL)
    assertThat(a.payloadDescription).isEqualTo(setOf("orderId", "amount"))
    assertThat(a.restrictions).containsEntry(CommonRestrictions.WORKER_LOCK_DURATION_IN_MILLISECONDS, "5000")
    assertThat(a.restrictions).containsEntry(CommonRestrictions.TENANT_ID, "tenant-a")
    val b = subscriptionApi.getSubscription("topic-b")
    assertThat(b.payloadDescription).isNull()
    assertThat(b.restrictions).containsEntry(CommonRestrictions.TENANT_ID, "default-tenant")
    assertThat(b.restrictions).doesNotContainKey(CommonRestrictions.WORKER_LOCK_DURATION_IN_MILLISECONDS)
    assertThat(subscriptionApi.getSubscription("workerC").restrictions).containsEntry(CommonRestrictions.TENANT_ID, "default-tenant")
    assertThat(subscriptionApi.getSubscription("topic-d")).isNotNull()
  }

  @Test fun `workers are registered before StartupEvent`() {
    assertThat(startupObserver.subscriptionsAtStartup).isEqualTo(4)
  }
}
