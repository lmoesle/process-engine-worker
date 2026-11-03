package dev.bpmcrafters.processengine.worker.quarkus.deployment.support

import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton

@ApplicationScoped
class TestApiProducer {
  @Produces @Singleton
  fun taskSubscriptionApi() = InMemoryTaskSubscriptionApi()

  @Produces @Singleton
  fun taskCompletionApi() = RecordingServiceTaskCompletionApi()
}
