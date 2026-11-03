package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import jakarta.enterprise.inject.Vetoed

@Vetoed
open class VetoedWorker {
  @ProcessEngineWorker(topic = "vetoed")
  open fun work() {}
}
