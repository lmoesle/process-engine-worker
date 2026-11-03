package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Singleton

@Singleton
class KotlinCollaborator {
  fun text() = "injected"
}

@Singleton
class KotlinMarker {
  fun text() = "marker"
}

@ApplicationScoped
class InjectedKotlinWorker @Inject constructor(
  private val collaborator: KotlinCollaborator,
  private val marker: KotlinMarker = KotlinMarker()
) {
  @ProcessEngineWorker("kotlin-injected")
  fun work(): Map<String, Any?> = mapOf("from" to collaborator.text(), "marker" to marker.text())
}
