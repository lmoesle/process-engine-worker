package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid.ProducedWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid.ProducedWorkerProducer
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ProducedWorkerValidationTest {
  companion object {
    @JvmField @RegisterExtension val TEST = ValidationTestSupport.expectValidationError(
      "is exposed via the producer", ProducedWorker::class.java, ProducedWorkerProducer::class.java)
  }
  @Test fun `build fails`() { fail<Unit>("The build should fail.") }
}
