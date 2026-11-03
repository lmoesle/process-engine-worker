package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengineapi.task.TaskInformation

open class ConcreteWorker : AbstractBaseWorker() {
  @ProcessEngineWorker(topic = "overridden")
  override fun overridden(taskInformation: TaskInformation): Map<String, Any?> {
    invocations.add("overridden:${taskInformation.taskId}")
    return mapOf("from" to "concrete")
  }
}
