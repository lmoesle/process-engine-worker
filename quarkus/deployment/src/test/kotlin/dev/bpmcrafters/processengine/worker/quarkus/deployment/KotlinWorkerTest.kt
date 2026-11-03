package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.KotlinWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.KotlinOrder
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class KotlinWorkerTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(KotlinWorker::class.java, KotlinOrder::class.java) })
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completionApi: RecordingServiceTaskCompletionApi
  @Test fun `invokes application scoped Kotlin proxy with DTO converted by Jackson`() {
    subscriptionApi.deliver("kotlin-order", "kotlin-task", mapOf("order" to mapOf("name" to "book", "amount" to 2)))
    assertThat(completionApi.failed).isEmpty()
    assertThat(completionApi.completed).hasSize(1)
    assertThat(completionApi.completed[0].get()).isEqualTo(mapOf("description" to "book:2"))
  }
}
