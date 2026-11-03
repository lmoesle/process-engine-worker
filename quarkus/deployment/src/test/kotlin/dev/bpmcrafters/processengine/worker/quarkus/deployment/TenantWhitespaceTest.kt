package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengineapi.CommonRestrictions
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class TenantWhitespaceTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest().setArchiveProducer { TestSupport.archive(Workers::class.java) })
  }
  open class Workers {
    @ProcessEngineWorker(topic = "same", tenantId = "tenant-a") open fun plain() {}
    @ProcessEngineWorker(topic = "same", tenantId = " tenant-a ") open fun padded() {}
  }
  @field:Inject lateinit var api: InMemoryTaskSubscriptionApi
  @Test fun `does not trim nonblank annotation tenants during duplicate validation`() {
    assertThat(api.subscriptions.map { it.restrictions[CommonRestrictions.TENANT_ID] })
      .containsExactlyInAnyOrder("tenant-a", " tenant-a ")
  }
}
