package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.BpmnErrorOccurred
import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengineapi.task.TaskInformation
import jakarta.inject.Inject
import jakarta.transaction.Synchronization
import jakarta.transaction.TransactionSynchronizationRegistry
import jakarta.transaction.Transactional
import java.util.concurrent.CopyOnWriteArrayList

open class TransactionalWorker {
  @field:Inject lateinit var synchronizationRegistry: TransactionSynchronizationRegistry
  val statusAtInvocation = CopyOnWriteArrayList<Int>()
  val completionStatus = CopyOnWriteArrayList<Int>()
  val invokedTasks = CopyOnWriteArrayList<String>()

  @Transactional @ProcessEngineWorker(topic = "tx-after-commit")
  open fun afterCommit(taskInformation: TaskInformation): Map<String, Any?> {
    probe(taskInformation)
    return mapOf("tx" to "after")
  }

  @Transactional @ProcessEngineWorker(topic = "tx-before-commit", completion = ProcessEngineWorker.Completion.BEFORE_COMMIT)
  open fun beforeCommit(taskInformation: TaskInformation): Map<String, Any?> {
    probe(taskInformation)
    return mapOf("tx" to "before")
  }

  @Transactional @ProcessEngineWorker(topic = "tx-bpmn-error")
  @Throws(BpmnErrorOccurred::class)
  open fun bpmnError(taskInformation: TaskInformation) {
    probe(taskInformation)
    throw BpmnErrorOccurred("business error in tx", "TX_ERROR", emptyMap())
  }

  @Transactional @ProcessEngineWorker(topic = "tx-runtime-error")
  open fun runtimeError(taskInformation: TaskInformation) {
    probe(taskInformation)
    throw IllegalStateException("runtime error in tx")
  }

  private fun probe(taskInformation: TaskInformation) {
    invokedTasks.add(taskInformation.taskId)
    statusAtInvocation.add(synchronizationRegistry.transactionStatus)
    synchronizationRegistry.registerInterposedSynchronization(CompletionProbe(completionStatus))
  }

  class CompletionProbe(private val statuses: MutableList<Int>) : Synchronization {
    override fun beforeCompletion() {}
    override fun afterCompletion(status: Int) { statuses.add(status) }
  }

  fun reset() {
    statusAtInvocation.clear()
    completionStatus.clear()
    invokedTasks.clear()
  }
}
