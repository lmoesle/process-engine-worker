package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.SimpleWorker
import dev.bpmcrafters.processengine.worker.transaction.TransactionalExecutor
import io.quarkus.arc.ClientProxy
import io.quarkus.test.QuarkusExtensionTest
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class NoneExecutorPlainWorkerTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(SimpleWorker::class.java, ExecutorProducer::class.java) }
      .setLogRecordPredicate { it.message.orEmpty().contains("PROCESS-ENGINE-WORKER-025") }
      .assertLogRecords { assertThat(it).isEmpty() })
  }
  @Singleton class ExecutorProducer {
    @Produces @Singleton fun executor(): TransactionalExecutor = TransactionalExecutor.NONE
  }
  @field:Inject lateinit var executor: TransactionalExecutor
  @field:Inject lateinit var api: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completion: RecordingServiceTaskCompletionApi
  @Test fun `runs plain workers without warning about transactional methods`() {
    assertThat(ClientProxy.unwrap(executor)).isSameAs(TransactionalExecutor.NONE)
    api.deliver("topic-b", "plain-task", emptyMap())
    assertThat(completion.failed).isEmpty()
    assertThat(completion.completed).hasSize(1)
    assertThat(completion.completed.single().get()).isEqualTo(mapOf("done" to true))
  }
}
