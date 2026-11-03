package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker

open class DuplicateTopicWorker {
  @ProcessEngineWorker(topic = "duplicate")
  open fun first() {}
  @ProcessEngineWorker(value = "duplicate")
  open fun second() {}
}
