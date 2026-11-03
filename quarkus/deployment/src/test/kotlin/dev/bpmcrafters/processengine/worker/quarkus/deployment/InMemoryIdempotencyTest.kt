package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.idempotency.*
import dev.bpmcrafters.processengine.worker.quarkus.JtaTransactionalExecutor
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.CustomBeans
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.IdempotentWorker
import io.quarkus.arc.Arc
import io.quarkus.arc.ClientProxy
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class InMemoryIdempotencyTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(CustomBeans::class.java, IdempotentWorker::class.java) }
      .overrideConfigKey("dev.bpm-crafters.process-api.worker.remove-task-result-on-completion", "false"))
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completionApi: RecordingServiceTaskCompletionApi
  @field:Inject lateinit var worker: IdempotentWorker
  @BeforeEach fun reset() { completionApi.reset() }

  @Test fun `after commit hook is installed`() {
    val registry = ClientProxy.unwrap(Arc.container().instance(IdempotencyRegistry::class.java).get()) as InMemoryIdempotencyRegistry
    assertThat(registry.afterCommitHook).isInstanceOf(JtaTransactionalExecutor::class.java)
  }

  @Test fun `second delivery does not invoke worker again`() {
    subscriptionApi.deliver("idempotent-tx", "task-1", emptyMap())
    subscriptionApi.deliver("idempotent-tx", "task-1", emptyMap())
    assertThat(worker.invocationsOf("task-1")).isEqualTo(1)
    assertThat(completionApi.completed).hasSize(2)
    assertThat(completionApi.completed[1].get()).isEqualTo(mapOf("result" to "tx-task-1"))
  }

  @Test fun `rollback does not register result`() {
    subscriptionApi.deliver("idempotent-fail-first", "task-2", emptyMap())
    assertThat(completionApi.failed).hasSize(1)
    assertThat(Arc.container().instance(IdempotencyRegistry::class.java).get().getTaskResult(InMemoryTaskSubscriptionApi.taskInformation("task-2"))).isNull()
    subscriptionApi.deliver("idempotent-fail-first", "task-2", emptyMap())
    assertThat(worker.invocationsOf("task-2")).isEqualTo(2)
    assertThat(completionApi.completed).hasSize(1)
  }
}
