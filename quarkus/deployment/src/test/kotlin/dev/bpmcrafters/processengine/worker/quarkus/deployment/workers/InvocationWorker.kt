package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import dev.bpmcrafters.processengine.worker.registrar.VariableConverter
import dev.bpmcrafters.processengineapi.task.CompleteTaskCmd
import dev.bpmcrafters.processengineapi.task.ServiceTaskCompletionApi
import dev.bpmcrafters.processengineapi.task.TaskInformation
import java.util.concurrent.CopyOnWriteArrayList

open class InvocationWorker {
  data class Order(val id: String, val amount: Int)
  val invocations = CopyOnWriteArrayList<Map<String, Any?>>()

  @ProcessEngineWorker(topic = "invoke")
  open fun invoke(@Variable("orderId") orderId: String, @Variable(name = "order") order: Order,
    @Variable(name = "missing", mandatory = false) missing: String?, taskInformation: TaskInformation,
    payload: Map<String, Any?>, variableConverter: VariableConverter, completionApi: ServiceTaskCompletionApi): Map<String, Any?> {
    invocations.add(mapOf("orderId" to orderId, "order" to order, "missing" to missing, "taskId" to taskInformation.taskId,
      "payload" to payload, "converter" to variableConverter, "completionApi" to completionApi))
    return mapOf("result" to "ok-$orderId")
  }

  @ProcessEngineWorker(topic = "manual", autoComplete = false)
  open fun manual(taskInformation: TaskInformation, completionApi: ServiceTaskCompletionApi) {
    invocations.add(mapOf("taskId" to taskInformation.taskId))
    completionApi.completeTask(CompleteTaskCmd(taskInformation.taskId) { mapOf("manual" to true) })
  }

  @ProcessEngineWorker(topic = "void-no-complete", autoComplete = false)
  open fun noCompletion(taskInformation: TaskInformation) { invocations.add(mapOf("taskId" to taskInformation.taskId)) }
}
