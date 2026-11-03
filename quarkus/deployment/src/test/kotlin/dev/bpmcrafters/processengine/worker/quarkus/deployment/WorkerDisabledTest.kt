package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.ProcessEngineWorkerRegistration
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.SimpleWorker
import io.quarkus.arc.Arc
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class WorkerDisabledTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(SimpleWorker::class.java) }
      .overrideConfigKey("dev.bpm-crafters.process-api.worker.enabled", "false"))
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var registration: ProcessEngineWorkerRegistration

  @Test fun `nothing is registered but the worker remains a bean`() {
    assertThat(subscriptionApi.subscriptions).isEmpty()
    assertThat(registration.taskSubscriptions).isEmpty()
    assertThat(Arc.container().instance(SimpleWorker::class.java).isAvailable).isTrue()
  }
}
