package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.TransactionalWorker
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import jakarta.transaction.Status
import jakarta.transaction.TransactionSynchronizationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.CopyOnWriteArrayList

class CompleteBeforeCommitConfigTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(TransactionalWorker::class.java) }
      .overrideConfigKey("dev.bpm-crafters.process-api.worker.complete-tasks-before-commit", "true"))
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completionApi: RecordingServiceTaskCompletionApi
  @field:Inject lateinit var synchronizationRegistry: TransactionSynchronizationRegistry

  @Test fun `completes before commit`() {
    val statusAtCompletion = CopyOnWriteArrayList<Int>()
    completionApi.setListener { statusAtCompletion.add(synchronizationRegistry.transactionStatus) }
    subscriptionApi.deliver("tx-after-commit", "task-1", emptyMap())
    assertThat(completionApi.completed).hasSize(1)
    assertThat(statusAtCompletion).containsExactly(Status.STATUS_ACTIVE)
  }
}
