package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengineapi.task.TaskInformation
import java.util.concurrent.CopyOnWriteArrayList

abstract class AbstractBaseWorker {
  val invocations = CopyOnWriteArrayList<String>()

  @ProcessEngineWorker(topic = "inherited")
  open fun inherited(taskInformation: TaskInformation): Map<String, Any?> {
    invocations.add("inherited:${taskInformation.taskId}")
    return mapOf("from" to "base")
  }

  abstract fun overridden(taskInformation: TaskInformation): Map<String, Any?>
}
