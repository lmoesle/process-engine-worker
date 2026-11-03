package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker

abstract class AbstractMethodWorker {
  @ProcessEngineWorker(topic = "abstract")
  abstract fun work()

  class Impl : AbstractMethodWorker() {
    override fun work() {}
  }
}
