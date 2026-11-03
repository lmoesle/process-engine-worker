package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.SimpleWorker
import io.quarkus.test.QuarkusExtensionTest
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MissingCompletionApiTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archiveWithoutAdapters(SimpleWorker::class.java, SubscriptionProducer::class.java, InMemoryTaskSubscriptionApi::class.java) }
      .assertException { t -> assertThat(TestSupport.anyInChain(t) { it.message.orEmpty().contains("Found no ServiceTaskCompletionApi bean") }).isTrue() })
  }
  @Singleton class SubscriptionProducer {
    @Produces @Singleton fun subscriptions() = InMemoryTaskSubscriptionApi()
  }
  @Test fun `reports the missing completion API`() { fail<Unit>("A completion API is required when workers are enabled.") }
}
