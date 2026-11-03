package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ConcreteBaseDiscoveryTest {
  companion object {
    @JvmField @RegisterExtension val TEST = ValidationTestSupport.expectValidationError("multiple worker methods", Base::class.java, Child::class.java)
  }
  open class Base { @ProcessEngineWorker("inherited-concrete") open fun work() {} }
  open class Child : Base()
  @Test fun `both concrete beans must be considered for duplicate topics`() {
    fail<Unit>("Both the concrete base and its child expose the worker and must be validated.")
  }
}
