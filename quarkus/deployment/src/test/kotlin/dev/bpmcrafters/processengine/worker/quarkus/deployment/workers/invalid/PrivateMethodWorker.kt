package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker

open class PrivateMethodWorker {
  @ProcessEngineWorker(topic = "private")
  private fun work() {}
}
