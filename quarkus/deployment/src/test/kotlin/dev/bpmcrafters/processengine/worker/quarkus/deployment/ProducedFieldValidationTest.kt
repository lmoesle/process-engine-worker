package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ProducedFieldValidationTest {
  companion object {
    @JvmField @RegisterExtension val TEST = ValidationTestSupport.expectValidationError("Workers must be class beans", GenericWorker::class.java, FieldProducer::class.java)
  }
  open class GenericWorker<T> { @ProcessEngineWorker("produced-field") open fun work() {} }
  @Singleton class FieldProducer {
    @JvmField @field:Produces var worker = GenericWorker<String>()
    @JvmField @field:Produces var retries: Int = 3
  }
  @Test fun `rejects a parameterized worker produced by a field`() { fail<Unit>("Producer fields are unsupported for worker instances, just like producer methods.") }
}
