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

class AmbiguousAdapterApiTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(SimpleWorker::class.java, ExtraAdapter::class.java) }
      .assertException { t -> assertThat(TestSupport.anyInChain(t) { it.message.orEmpty().contains("Found multiple TaskSubscriptionApi beans") }).isTrue() })
  }
  @Singleton class ExtraAdapter {
    @Produces @Singleton fun extraSubscriptionApi() = InMemoryTaskSubscriptionApi()
  }
  @Test fun `reports ambiguous subscription APIs`() { fail<Unit>("Exactly one subscription API must be selected.") }
}
