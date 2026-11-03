package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.ProcessEngineWorkerRegistration
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.TestSupport
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class EmptyWithoutAdapterTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archiveWithoutAdapters() })
  }
  @field:Inject lateinit var registration: ProcessEngineWorkerRegistration
  @Test fun `starts without adapters when there are no workers`() {
    assertThat(registration.taskSubscriptions).isEmpty()
  }
}
