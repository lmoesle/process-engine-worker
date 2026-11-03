package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid.DuplicateTopicWorker
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class DuplicateTopicValidationTest {
  companion object {
    @JvmField @RegisterExtension val TEST = ValidationTestSupport.expectValidationError(
      "Topic 'duplicate' is used by multiple worker methods", DuplicateTopicWorker::class.java)
  }
  @Test fun `build fails`() { fail<Unit>("The build should fail.") }
}
