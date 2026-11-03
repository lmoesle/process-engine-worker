package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid.AbstractMethodWorker
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class AbstractMethodValidationTest {
  companion object {
    @JvmField @RegisterExtension val TEST = ValidationTestSupport.expectValidationError(
      "must not be abstract", AbstractMethodWorker::class.java, AbstractMethodWorker.Impl::class.java)
  }
  @Test fun `build fails`() { fail<Unit>("The build should fail.") }
}
