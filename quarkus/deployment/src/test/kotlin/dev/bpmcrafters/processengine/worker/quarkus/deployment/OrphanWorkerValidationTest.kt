package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class OrphanWorkerValidationTest {
  companion object {
    @JvmField @RegisterExtension val TEST = ValidationTestSupport.expectValidationError("without any concrete subclass", AbstractWorker::class.java, InterfaceWorker::class.java)
  }
  abstract class AbstractWorker { @ProcessEngineWorker("orphan-class") open fun work() {} }
  interface InterfaceWorker { @ProcessEngineWorker("orphan-interface") fun work() {} }
  @Test fun `reports annotated implementations without any concrete worker bean`() { fail<Unit>("Orphan worker definitions must fail build-time validation.") }
}
