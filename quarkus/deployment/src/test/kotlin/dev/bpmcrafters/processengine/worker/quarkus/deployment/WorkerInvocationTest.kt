package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.InvocationWorker
import dev.bpmcrafters.processengine.worker.registrar.JacksonVariableConverter
import dev.bpmcrafters.processengine.worker.registrar.VariableConverter
import dev.bpmcrafters.processengineapi.task.ServiceTaskCompletionApi
import io.quarkus.arc.ClientProxy
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class WorkerInvocationTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(InvocationWorker::class.java) })
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completionApi: RecordingServiceTaskCompletionApi
  @field:Inject lateinit var worker: InvocationWorker

  @BeforeEach fun reset() {
    completionApi.reset()
    worker.invocations.clear()
  }

  @Test fun `invokes worker with resolved parameters and completes with result`() {
    val payload = mapOf("orderId" to "order-1", "order" to mapOf("id" to "order-1", "amount" to 42), "other" to "ignored")
    subscriptionApi.deliver("invoke", "task-1", payload)
    assertThat(worker.invocations).hasSize(1)
    val invocation = worker.invocations[0]
    assertThat(invocation["orderId"]).isEqualTo("order-1")
    assertThat(invocation["order"]).isEqualTo(InvocationWorker.Order("order-1", 42))
    assertThat(invocation["missing"]).isNull()
    assertThat(invocation["taskId"]).isEqualTo("task-1")
    assertThat(invocation["payload"]).isEqualTo(payload)
    assertThat(ClientProxy.unwrap(invocation["converter"] as VariableConverter)).isInstanceOf(JacksonVariableConverter::class.java)
    assertThat(invocation["completionApi"] as ServiceTaskCompletionApi).isSameAs(completionApi)
    assertThat(completionApi.completed).hasSize(1)
    assertThat(completionApi.completed[0].taskId).isEqualTo("task-1")
    assertThat(completionApi.completed[0].get()).isEqualTo(mapOf("result" to "ok-order-1"))
    assertThat(completionApi.failed).isEmpty()
  }

  @Test fun `does not auto complete if disabled`() {
    subscriptionApi.deliver("manual", "task-2", emptyMap())
    assertThat(worker.invocations).hasSize(1)
    assertThat(completionApi.completed).hasSize(1)
    assertThat(completionApi.completed[0].get()).isEqualTo(mapOf("manual" to true))
    completionApi.reset()
    subscriptionApi.deliver("void-no-complete", "task-3", emptyMap())
    assertThat(completionApi.completed).isEmpty()
    assertThat(completionApi.failed).isEmpty()
  }

  @Test fun `fails task if mandatory variable is missing`() {
    subscriptionApi.deliver("invoke", "task-4", mapOf("order" to mapOf("id" to "x", "amount" to 1)))
    assertThat(worker.invocations).isEmpty()
    assertThat(completionApi.failed).hasSize(1)
    assertThat(completionApi.failed[0].taskId).isEqualTo("task-4")
  }
}
