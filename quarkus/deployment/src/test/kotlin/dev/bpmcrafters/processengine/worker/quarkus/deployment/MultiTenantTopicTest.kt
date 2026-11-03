package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.MultiTenantWorker
import dev.bpmcrafters.processengineapi.CommonRestrictions
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MultiTenantTopicTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(MultiTenantWorker::class.java) })
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @Test fun `same topic for different tenants is accepted`() {
    assertThat(subscriptionApi.subscriptions).hasSize(2)
    assertThat(subscriptionApi.subscriptions.map { it.restrictions[CommonRestrictions.TENANT_ID] })
      .containsExactlyInAnyOrder("tenant-a", "tenant-b")
  }
}
