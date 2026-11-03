package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton

@ApplicationScoped
class ProducedWorkerProducer {
  @Produces @Singleton
  fun producedWorker() = ProducedWorker("https://example.org")
}
