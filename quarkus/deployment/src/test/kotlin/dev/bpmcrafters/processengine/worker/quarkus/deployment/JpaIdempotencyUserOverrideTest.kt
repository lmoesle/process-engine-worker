package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.idempotency.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.TestSupport
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.CustomBeans
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.IdempotentWorker
import io.quarkus.arc.Arc
import io.quarkus.arc.ClientProxy
import io.quarkus.test.QuarkusExtensionTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class JpaIdempotencyUserOverrideTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(CustomBeans::class.java, IdempotentWorker::class.java) }, "idempotency-override")
  }
  @Test fun `user registry wins`() {
    assertThat(Arc.container().listAll(IdempotencyRegistry::class.java)).hasSize(1)
    assertThat(ClientProxy.unwrap(Arc.container().instance(IdempotencyRegistry::class.java).get())).isInstanceOf(InMemoryIdempotencyRegistry::class.java)
  }
}
