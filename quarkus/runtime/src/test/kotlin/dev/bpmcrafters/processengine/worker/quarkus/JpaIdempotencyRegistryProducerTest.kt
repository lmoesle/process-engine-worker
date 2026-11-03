package dev.bpmcrafters.processengine.worker.quarkus

import dev.bpmcrafters.processengine.worker.transaction.TransactionalExecutor
import jakarta.enterprise.inject.Instance
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

class JpaIdempotencyRegistryProducerTest {
  @Test
  fun `reports an unavailable default persistence unit before requesting an entity manager`() {
    val entityManager = mock<Instance<EntityManager>>()

    assertThatThrownBy {
      JpaIdempotencyRegistryProducer().jpaIdempotencyRegistry(entityManager, TransactionalExecutor.NONE)
    }.isInstanceOf(IllegalStateException::class.java)
      .hasMessageContaining("PROCESS-ENGINE-WORKER-036")
      .hasMessageContaining("default persistence unit")
    verify(entityManager, never()).get()
  }
}
