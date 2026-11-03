package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker

open class OverridingTopicWorker : OverridingTopicWorkerBase() {
  @ProcessEngineWorker(topic = "child-topic")
  override fun run() {}
}
