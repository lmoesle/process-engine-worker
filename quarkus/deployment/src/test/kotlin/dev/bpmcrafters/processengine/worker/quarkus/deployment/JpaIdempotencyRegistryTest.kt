package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.idempotency.EntityManagerJpaIdempotencyRegistry
import dev.bpmcrafters.processengine.worker.idempotency.IdempotencyRegistry
import dev.bpmcrafters.processengine.worker.idempotency.TaskLogEntry
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.IdempotentWorker
import dev.bpmcrafters.processengineapi.task.TaskInformation
import io.quarkus.arc.Arc
import io.quarkus.arc.ClientProxy
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class JpaIdempotencyRegistryTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(IdempotentWorker::class.java) }
      .overrideConfigKey("dev.bpm-crafters.process-api.worker.remove-task-result-on-completion", "false"), "idempotency")
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completionApi: RecordingServiceTaskCompletionApi
  @field:Inject lateinit var worker: IdempotentWorker
  @field:Inject lateinit var entityManager: EntityManager
  @field:Inject lateinit var registry: IdempotencyRegistry
  @BeforeEach fun reset() { completionApi.reset() }

  @Test fun `JPA registry is active`() {
    assertThat(Arc.container().listAll(IdempotencyRegistry::class.java)).hasSize(1)
    assertThat(ClientProxy.unwrap(Arc.container().instance(IdempotencyRegistry::class.java).get())).isInstanceOf(EntityManagerJpaIdempotencyRegistry::class.java)
  }

  @Test fun `transactional worker persists result and is not invoked twice`() {
    subscriptionApi.deliver("idempotent-tx", "task-1", emptyMap())
    val entry = find("task-1")
    assertThat(entry).isNotNull()
    assertThat(entry!!.processInstanceId).isEqualTo("instance-task-1")
    assertThat(entry.result).isEqualTo(mapOf("result" to "tx-task-1"))
    subscriptionApi.deliver("idempotent-tx", "task-1", emptyMap())
    assertThat(worker.invocationsOf("task-1")).isEqualTo(1)
    assertThat(completionApi.completed).hasSize(2)
    assertThat(completionApi.completed[1].get()).isEqualTo(mapOf("result" to "tx-task-1"))
  }

  @Test fun `nontransactional worker persists result in own transaction`() {
    subscriptionApi.deliver("idempotent-plain", "task-2", emptyMap())
    assertThat(find("task-2")).isNotNull()
    assertThat(find("task-2")!!.result).isEqualTo(mapOf("result" to "plain-task-2"))
    subscriptionApi.deliver("idempotent-plain", "task-2", emptyMap())
    assertThat(worker.invocationsOf("task-2")).isEqualTo(1)
    assertThat(completionApi.completed).hasSize(2)
  }

  @Test fun `rollback leaves no row and redelivery invokes again`() {
    subscriptionApi.deliver("idempotent-fail-first", "task-3", emptyMap())
    assertThat(completionApi.failed).hasSize(1)
    assertThat(find("task-3")).isNull()
    subscriptionApi.deliver("idempotent-fail-first", "task-3", emptyMap())
    assertThat(worker.invocationsOf("task-3")).isEqualTo(2)
    assertThat(completionApi.completed).hasSize(1)
    assertThat(find("task-3")).isNotNull()
  }

  @Test fun `BPMN error is reported and leaves no row`() {
    subscriptionApi.deliver("idempotent-bpmn-error", "task-4", emptyMap())
    assertThat(completionApi.completedByError).hasSize(1)
    assertThat(completionApi.completedByError[0].errorCode).isEqualTo("IDEMPOTENT_ERROR")
    assertThat(find("task-4")).isNull()
  }

  private fun find(taskId: String): TaskLogEntry? = QuarkusTransaction.requiringNew().call { entityManager.find(TaskLogEntry::class.java, taskId) }

  @Test fun `rejects task information without a process instance ID without persisting a row`() {
    val task = TaskInformation("missing-instance", emptyMap())
    assertThatThrownBy { registry.register(task, mapOf("result" to "unused")) }
      .isInstanceOf(IllegalArgumentException::class.java).hasMessageContaining("must contain the process instance id")
    assertThat(find(task.taskId)).isNull()
  }

  @Test fun `replaces an existing result and removes only the requested process instance`() {
    val first = InMemoryTaskSubscriptionApi.taskInformation("registry-first")
    val second = InMemoryTaskSubscriptionApi.taskInformation("registry-second")
    registry.register(first, mapOf("value" to "old"))
    registry.register(first, mapOf("value" to "new"))
    registry.register(second, emptyMap())
    assertThat(registry.getTaskResult(first)).isEqualTo(mapOf("value" to "new"))
    registry.removeAllTaskResults("instance-registry-first")
    assertThat(registry.getTaskResult(first)).isNull()
    assertThat(registry.getTaskResult(second)).isEmpty()
    registry.removeTaskResult(second.taskId)
    assertThat(registry.getTaskResult(second)).isNull()
  }
}
