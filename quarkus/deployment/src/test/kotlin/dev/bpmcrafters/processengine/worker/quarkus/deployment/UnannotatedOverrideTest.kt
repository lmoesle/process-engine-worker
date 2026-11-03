package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class UnannotatedOverrideTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest().setArchiveProducer { TestSupport.archive(Base::class.java, Child::class.java) })
  }
  abstract class Base { @ProcessEngineWorker("suppressed") open fun work() {} }
  open class Child : Base() { override fun work() {} }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @Test fun `an unannotated override suppresses the worker`() { assertThat(subscriptionApi.subscriptions).isEmpty() }
}
