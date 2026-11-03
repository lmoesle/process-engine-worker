package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.TestSupport
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.SimpleWorker
import io.quarkus.test.QuarkusExtensionTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class WorkerWithoutEngineAdapterTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archiveWithoutAdapters(SimpleWorker::class.java) }
      .assertException { assertThat(it).hasMessageContaining("PROCESS-ENGINE-WORKER-023").hasMessageContaining("TaskSubscriptionApi") })
  }
  @Test fun `start fails`() { fail<Unit>("The application should not start.") }
}
