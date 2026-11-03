package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker

open class ProducedWorker(val url: String) {
  @ProcessEngineWorker(topic = "produced")
  open fun work() {}
}
