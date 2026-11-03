package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.InMemoryTaskSubscriptionApi
import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes

@ApplicationScoped
class StartupObserver {
  @Volatile private var countAtStartup = -1
  val subscriptionsAtStartup: Int get() = countAtStartup

  fun onStart(@Observes event: StartupEvent, api: InMemoryTaskSubscriptionApi) {
    countAtStartup = api.subscriptions.size
  }
}
