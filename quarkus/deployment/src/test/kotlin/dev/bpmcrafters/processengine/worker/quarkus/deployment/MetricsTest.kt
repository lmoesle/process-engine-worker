package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.MicrometerProcessEngineWorkerMetrics
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.SimpleWorker
import dev.bpmcrafters.processengine.worker.registrar.ProcessEngineWorkerMetrics
import dev.bpmcrafters.processengine.worker.registrar.metrics.ProcessEngineWorkerMetricsMicrometer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.quarkus.arc.Arc
import io.quarkus.arc.ClientProxy
import io.quarkus.test.QuarkusExtensionTest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class MetricsTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(SimpleWorker::class.java, SimpleRegistryProducer::class.java) })
  }
  @ApplicationScoped class SimpleRegistryProducer {
    @Produces @Singleton fun simpleMeterRegistry() = SimpleMeterRegistry()
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var meterRegistry: SimpleMeterRegistry

  @Test fun `Micrometer metrics are recorded`() {
    assertThat(ClientProxy.unwrap(Arc.container().instance(ProcessEngineWorkerMetrics::class.java).get())).isInstanceOf(MicrometerProcessEngineWorkerMetrics::class.java)
    subscriptionApi.deliver("workerC", "task-1", emptyMap())
    assertThat(meterRegistry.counter(ProcessEngineWorkerMetricsMicrometer.PREFIX + ".received", "topic", "workerC").count()).isEqualTo(1.0)
    assertThat(meterRegistry.counter(ProcessEngineWorkerMetricsMicrometer.PREFIX + ".completed", "topic", "workerC").count()).isEqualTo(1.0)
  }
}
