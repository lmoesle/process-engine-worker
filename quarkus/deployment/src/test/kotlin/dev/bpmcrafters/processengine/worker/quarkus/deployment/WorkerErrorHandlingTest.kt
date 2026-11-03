package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.ErrorWorker
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.Duration

class WorkerErrorHandlingTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest().setArchiveProducer { TestSupport.archive(ErrorWorker::class.java) })
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completionApi: RecordingServiceTaskCompletionApi
  @BeforeEach fun reset() { completionApi.reset() }

  @Test fun `BPMN error completes task by error`() {
    subscriptionApi.deliver("bpmn-error", "task-1", emptyMap())
    assertThat(completionApi.completedByError).hasSize(1)
    val cmd = completionApi.completedByError[0]
    assertThat(cmd.taskId).isEqualTo("task-1")
    assertThat(cmd.errorCode).isEqualTo("BUSINESS_ERROR")
    assertThat(cmd.errorMessage).isEqualTo("business error happened")
    assertThat(cmd.get()).isEqualTo(mapOf("reason" to "test"))
    assertThat(completionApi.completed).isEmpty()
    assertThat(completionApi.failed).isEmpty()
  }

  @Test fun `runtime exception fails task with decremented retries`() {
    subscriptionApi.deliver("runtime-error", "task-2", emptyMap())
    assertThat(completionApi.failed).hasSize(1)
    val cmd = completionApi.failed[0]
    assertThat(cmd.taskId).isEqualTo("task-2")
    assertThat(cmd.reason).isEqualTo("boom")
    assertThat(cmd.retryCount).isEqualTo(2)
    assertThat(cmd.retryBackoff).isNull()
    assertThat(cmd.errorDetails).contains("IllegalStateException")
  }

  @Test fun `FailJobException fails task with given retries`() {
    subscriptionApi.deliver("fail-job", "task-3", emptyMap())
    assertThat(completionApi.failed).hasSize(1)
    val cmd = completionApi.failed[0]
    assertThat(cmd.reason).isEqualTo("fail with retries")
    assertThat(cmd.retryCount).isEqualTo(7)
    assertThat(cmd.retryBackoff).isEqualTo(Duration.ofSeconds(30))
  }
}
