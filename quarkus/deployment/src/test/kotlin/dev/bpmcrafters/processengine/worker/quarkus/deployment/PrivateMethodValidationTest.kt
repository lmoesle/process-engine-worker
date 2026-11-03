package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid.PrivateMethodWorker
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class PrivateMethodValidationTest {
  companion object {
    @JvmField @RegisterExtension val TEST = ValidationTestSupport.expectValidationError("must be public", PrivateMethodWorker::class.java)
  }
  @Test fun `build fails`() { fail<Unit>("The build should fail.") }
}
