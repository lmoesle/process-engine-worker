package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.BpmnErrorOccurred
import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengineapi.task.TaskInformation
import jakarta.transaction.Transactional
import java.util.concurrent.CopyOnWriteArrayList

open class IdempotentWorker {
  val invocations = CopyOnWriteArrayList<String>()

  @Transactional @ProcessEngineWorker(topic = "idempotent-tx")
  open fun transactional(taskInformation: TaskInformation): Map<String, Any?> {
    invocations.add(taskInformation.taskId)
    return mapOf("result" to "tx-${taskInformation.taskId}")
  }

  @ProcessEngineWorker(topic = "idempotent-plain")
  open fun plain(taskInformation: TaskInformation): Map<String, Any?> {
    invocations.add(taskInformation.taskId)
    return mapOf("result" to "plain-${taskInformation.taskId}")
  }

  @Transactional @ProcessEngineWorker(topic = "idempotent-fail-first")
  open fun failFirst(taskInformation: TaskInformation): Map<String, Any?> {
    invocations.add(taskInformation.taskId)
    check(invocationsOf(taskInformation.taskId) != 1L) { "first invocation fails" }
    return mapOf("result" to "second")
  }

  @Transactional @ProcessEngineWorker(topic = "idempotent-bpmn-error")
  @Throws(BpmnErrorOccurred::class)
  open fun bpmnError(taskInformation: TaskInformation) {
    invocations.add(taskInformation.taskId)
    throw BpmnErrorOccurred("error", "IDEMPOTENT_ERROR", emptyMap())
  }

  fun invocationsOf(taskId: String): Long = invocations.count { it == taskId }.toLong()
}
