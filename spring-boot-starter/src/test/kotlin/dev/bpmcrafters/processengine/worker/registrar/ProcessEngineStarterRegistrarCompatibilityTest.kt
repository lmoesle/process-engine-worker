package dev.bpmcrafters.processengine.worker.registrar

import dev.bpmcrafters.processengine.worker.FailJobException
import dev.bpmcrafters.processengine.worker.configuration.ProcessEngineWorkerProperties
import dev.bpmcrafters.processengine.worker.idempotency.NoOpIdempotencyRegistry
import dev.bpmcrafters.processengineapi.task.TaskInformation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.Duration

internal class ProcessEngineStarterRegistrarCompatibilityTest {

  private val registrar = ProcessEngineStarterRegistrar(
    ProcessEngineWorkerProperties(), mock(), mock(), mock(), mock(), mock(), mock(), mock(), NoOpIdempotencyRegistry()
  )

  @Test
  fun `retains task handler at the original nested type`() {
    val handler = ProcessEngineStarterRegistrar.TaskHandlerWithResult { task, payload ->
      payload + ("taskId" to task.taskId)
    }

    assertThat(handler(TaskInformation("task", emptyMap()), mapOf("done" to true)))
      .isEqualTo(mapOf("done" to true, "taskId" to "task"))
  }

  @Test
  fun `returns the original retry type while delegating calculation`() {
    val backoff = Duration.ofSeconds(5)
    val retry: ProcessEngineStarterRegistrar.FailureRetry = registrar.calculateRetry(
      TaskInformation("task", emptyMap()),
      FailJobException("retry", null, 3, backoff)
    )

    assertThat(retry).isEqualTo(ProcessEngineStarterRegistrar.FailureRetry(3, backoff))
    assertThat(retry.copy(retryCount = 2).component1()).isEqualTo(2)
    assertThat(retry.component2()).isEqualTo(backoff)
  }

  @Test
  fun `retains default retry calculation in the Spring facade`() {
    val retry = registrar.calculateRetry(
      TaskInformation("task", mapOf(TaskInformation.RETRIES to "4")), IllegalStateException("failed")
    )

    assertThat(retry).isEqualTo(ProcessEngineStarterRegistrar.FailureRetry(3, null))
  }
}
