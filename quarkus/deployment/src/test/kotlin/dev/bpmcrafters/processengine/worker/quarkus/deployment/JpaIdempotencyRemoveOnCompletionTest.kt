package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.idempotency.TaskLogEntry
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.IdempotentWorker
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class JpaIdempotencyRemoveOnCompletionTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(IdempotentWorker::class.java) }, "idempotency-remove")
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completionApi: RecordingServiceTaskCompletionApi
  @field:Inject lateinit var worker: IdempotentWorker
  @field:Inject lateinit var entityManager: EntityManager

  @Test fun `result is removed after completion`() {
    subscriptionApi.deliver("idempotent-tx", "task-1", emptyMap())
    subscriptionApi.deliver("idempotent-plain", "task-2", emptyMap())
    assertThat(completionApi.completed).hasSize(2)
    assertThat(worker.invocations).containsExactly("task-1", "task-2")
    val entries = QuarkusTransaction.requiringNew().call { entityManager.createQuery("select e from TaskLogEntry e", TaskLogEntry::class.java).resultList }
    assertThat(entries).isEmpty()
  }
}
