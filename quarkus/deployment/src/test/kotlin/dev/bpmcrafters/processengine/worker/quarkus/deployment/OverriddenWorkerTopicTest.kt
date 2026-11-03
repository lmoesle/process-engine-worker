package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.OverridingTopicWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.OverridingTopicWorkerBase
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class OverriddenWorkerTopicTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(OverridingTopicWorker::class.java, OverridingTopicWorkerBase::class.java) })
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @Test fun `only the overriding topic is registered`() {
    assertThat(subscriptionApi.subscriptions.map { it.taskDescriptionKey }).containsExactly("child-topic")
  }
}
