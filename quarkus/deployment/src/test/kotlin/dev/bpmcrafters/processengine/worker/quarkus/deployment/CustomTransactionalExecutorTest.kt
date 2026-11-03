package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.transaction.TransactionalExecutor
import io.quarkus.arc.Arc
import io.quarkus.arc.ClientProxy
import io.quarkus.test.QuarkusExtensionTest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Singleton
import jakarta.transaction.Transactional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.atomic.AtomicInteger

/** NONE replaces the registrar executor, not CDI's independent transaction interceptor. */
class CustomTransactionalExecutorTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(NoneExecutorProducer::class.java, TransactionalWorkerWithoutTx::class.java) }
      .setLogRecordPredicate { it.message.orEmpty().contains("PROCESS-ENGINE-WORKER-025") }
      .assertLogRecords { assertThat(it).hasSize(1) })
  }
  @ApplicationScoped class NoneExecutorProducer {
    @Produces @Singleton fun executor(): TransactionalExecutor = TransactionalExecutor.NONE
  }
  open class TransactionalWorkerWithoutTx {
    companion object { @JvmField val INVOCATIONS = AtomicInteger() }
    @Transactional @ProcessEngineWorker(topic = "tx")
    open fun work(): Map<String, Any?> {
      INVOCATIONS.incrementAndGet()
      return mapOf("ok" to true)
    }
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completionApi: RecordingServiceTaskCompletionApi

  @Test fun `uses the custom executor`() {
    assertThat(Arc.container().listAll(TransactionalExecutor::class.java)).hasSize(1)
    assertThat(ClientProxy.unwrap(Arc.container().instance(TransactionalExecutor::class.java).get())).isSameAs(TransactionalExecutor.NONE)
    subscriptionApi.deliver("tx", "task-1", emptyMap())
    assertThat(TransactionalWorkerWithoutTx.INVOCATIONS.get()).isEqualTo(1)
    assertThat(completionApi.completed).hasSize(1)
  }
}
