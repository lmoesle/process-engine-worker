package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.TestSupport
import io.quarkus.test.QuarkusExtensionTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class EffectiveTenantCollisionTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(Workers::class.java) }
      .overrideConfigKey("dev.bpm-crafters.process-api.worker.tenant-id", "tenant-a")
      .assertException { t -> assertThat(TestSupport.anyInChain(t) { it.message.orEmpty().contains("same effective topic and tenant") }).isTrue() })
  }
  open class Workers {
    @ProcessEngineWorker(topic = "same") open fun defaultTenant() {}
    @ProcessEngineWorker(topic = "same", tenantId = "tenant-a") open fun explicitTenant() {}
  }
  @Test fun `runtime default must not introduce duplicate subscriptions`() { fail<Unit>("Runtime default tenant collides with the explicit tenant.") }
}
