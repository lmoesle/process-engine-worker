package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker

open class ConflictingTopicWorker {
  @ProcessEngineWorker(topic = "one", value = "two")
  open fun work() {}
}
