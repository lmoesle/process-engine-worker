package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid.ConflictingTopicWorker
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ConflictingTopicValidationTest {
  companion object {
    @JvmField @RegisterExtension val TEST = ValidationTestSupport.expectValidationError(
      "are aliases and must not be set to different values", ConflictingTopicWorker::class.java)
  }
  @Test fun `build fails`() { fail<Unit>("The build should fail.") }
}
