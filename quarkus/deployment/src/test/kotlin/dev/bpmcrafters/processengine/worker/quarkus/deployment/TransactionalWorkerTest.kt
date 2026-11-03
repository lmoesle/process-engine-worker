package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.JtaTransactionalExecutor
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.TransactionalWorker
import dev.bpmcrafters.processengine.worker.transaction.TransactionalExecutor
import io.quarkus.arc.Arc
import io.quarkus.arc.ClientProxy
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import jakarta.transaction.Status
import jakarta.transaction.TransactionSynchronizationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.CopyOnWriteArrayList

class TransactionalWorkerTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest().setArchiveProducer { TestSupport.archive(TransactionalWorker::class.java) })
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completionApi: RecordingServiceTaskCompletionApi
  @field:Inject lateinit var worker: TransactionalWorker
  @field:Inject lateinit var synchronizationRegistry: TransactionSynchronizationRegistry
  private val statusAtCompletion = CopyOnWriteArrayList<Int>()

  @BeforeEach fun reset() {
    completionApi.reset()
    worker.reset()
    statusAtCompletion.clear()
    completionApi.setListener { statusAtCompletion.add(synchronizationRegistry.transactionStatus) }
  }

  @Test fun `JTA executor is active`() {
    assertThat(ClientProxy.unwrap(Arc.container().instance(TransactionalExecutor::class.java).get())).isInstanceOf(JtaTransactionalExecutor::class.java)
  }

  @Test fun `completes after commit by default`() {
    subscriptionApi.deliver("tx-after-commit", "task-1", emptyMap())
    assertThat(worker.statusAtInvocation).containsExactly(Status.STATUS_ACTIVE)
    assertThat(worker.completionStatus).containsExactly(Status.STATUS_COMMITTED)
    assertThat(completionApi.completed).hasSize(1)
    assertThat(completionApi.completed[0].get()).isEqualTo(mapOf("tx" to "after"))
    assertThat(statusAtCompletion).containsExactly(Status.STATUS_NO_TRANSACTION)
  }

  @Test fun `completes before commit if configured`() {
    subscriptionApi.deliver("tx-before-commit", "task-2", emptyMap())
    assertThat(worker.statusAtInvocation).containsExactly(Status.STATUS_ACTIVE)
    assertThat(worker.completionStatus).containsExactly(Status.STATUS_COMMITTED)
    assertThat(completionApi.completed).hasSize(1)
    assertThat(statusAtCompletion).containsExactly(Status.STATUS_ACTIVE)
  }

  @Test fun `BPMN error rolls back and reports error`() {
    subscriptionApi.deliver("tx-bpmn-error", "task-3", emptyMap())
    assertThat(worker.completionStatus).containsExactly(Status.STATUS_ROLLEDBACK)
    assertThat(completionApi.completedByError).hasSize(1)
    assertThat(completionApi.completedByError[0].errorCode).isEqualTo("TX_ERROR")
    assertThat(completionApi.completed).isEmpty()
    assertThat(completionApi.failed).isEmpty()
    assertThat(statusAtCompletion).containsExactly(Status.STATUS_NO_TRANSACTION)
  }

  @Test fun `runtime exception rolls back and fails task`() {
    subscriptionApi.deliver("tx-runtime-error", "task-4", emptyMap())
    assertThat(worker.completionStatus).containsExactly(Status.STATUS_ROLLEDBACK)
    assertThat(completionApi.failed).hasSize(1)
    assertThat(completionApi.failed[0].reason).isEqualTo("runtime error in tx")
    assertThat(completionApi.completed).isEmpty()
  }
}
