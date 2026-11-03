package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker

open class MultiTenantWorker {
  @ProcessEngineWorker(topic = "shared-topic", tenantId = "tenant-a")
  open fun forTenantA() {}

  @ProcessEngineWorker(topic = "shared-topic", tenantId = "tenant-b")
  open fun forTenantB() {}
}
