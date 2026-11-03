package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.ProcessEngineWorkerRegistration
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.TestSupport
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.SimpleWorker
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class DisabledWithoutAdapterTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archiveWithoutAdapters(SimpleWorker::class.java) }
      .overrideConfigKey("dev.bpm-crafters.process-api.worker.enabled", "false"))
  }
  @field:Inject lateinit var registration: ProcessEngineWorkerRegistration
  @Test fun `starts without adapters when registration is disabled`() {
    assertThat(registration.taskSubscriptions).isEmpty()
  }
}
