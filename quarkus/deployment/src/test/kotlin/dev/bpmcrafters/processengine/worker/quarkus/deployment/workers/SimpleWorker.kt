package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable

open class SimpleWorker {
  @ProcessEngineWorker(topic = "topic-a", lockDuration = 5000, tenantId = "tenant-a")
  open fun workerA(@Variable("orderId") orderId: String, @Variable(name = "amount", mandatory = false) amount: Int?) {}

  @ProcessEngineWorker(value = "topic-b")
  open fun workerB(payload: Map<String, Any?>): Map<String, Any?> = mapOf("done" to true)

  @ProcessEngineWorker
  open fun workerC() {}

  @ProcessEngineWorker("topic-d")
  open fun workerD() {}
}
