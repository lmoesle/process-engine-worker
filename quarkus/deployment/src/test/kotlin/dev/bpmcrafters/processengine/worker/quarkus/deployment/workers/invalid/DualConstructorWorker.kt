package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.RecordingServiceTaskCompletionApi

open class DualConstructorWorker(private val api: RecordingServiceTaskCompletionApi?) {
  constructor() : this(null)

  @ProcessEngineWorker(topic = "dual")
  open fun work() { api!!.completed }
}
