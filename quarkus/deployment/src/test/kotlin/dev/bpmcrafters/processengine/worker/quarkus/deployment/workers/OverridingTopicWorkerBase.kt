package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker

abstract class OverridingTopicWorkerBase {
  @ProcessEngineWorker(topic = "parent-topic")
  open fun run() {}
}
