package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import jakarta.enterprise.context.ApplicationScoped

data class KotlinOrder(val name: String, val amount: Int)

@ApplicationScoped
class KotlinWorker {
  @ProcessEngineWorker("kotlin-order")
  fun work(@Variable("order") order: KotlinOrder): Map<String, Any?> =
    mapOf("description" to "${order.name}:${order.amount}")
}
