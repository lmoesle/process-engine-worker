package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.idempotency.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.CustomBeans
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.CustomBeansWorker
import dev.bpmcrafters.processengine.worker.registrar.ParameterResolver
import dev.bpmcrafters.processengine.worker.registrar.ResultResolver
import io.quarkus.arc.Arc
import io.quarkus.arc.ClientProxy
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class CustomBeansTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(CustomBeans::class.java, CustomBeansWorker::class.java) })
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completionApi: RecordingServiceTaskCompletionApi

  @Test fun `custom beans replace defaults`() {
    assertThat(Arc.container().listAll(IdempotencyRegistry::class.java)).hasSize(1)
    assertThat(ClientProxy.unwrap(Arc.container().instance(IdempotencyRegistry::class.java).get())).isInstanceOf(InMemoryIdempotencyRegistry::class.java)
    assertThat(Arc.container().listAll(ParameterResolver::class.java)).hasSize(1)
    assertThat(Arc.container().listAll(ResultResolver::class.java)).hasSize(1)
    subscriptionApi.deliver("custom", "task-1", emptyMap())
    assertThat(completionApi.completed).hasSize(1)
    assertThat(completionApi.completed[0].get()).isEqualTo(mapOf("text" to "hello task-1"))
  }
}
