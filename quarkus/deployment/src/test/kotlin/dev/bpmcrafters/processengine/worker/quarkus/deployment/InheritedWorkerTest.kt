package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.AbstractBaseWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.ConcreteWorker
import io.quarkus.arc.Arc
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class InheritedWorkerTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(AbstractBaseWorker::class.java, ConcreteWorker::class.java) })
  }
  @field:Inject lateinit var subscriptionApi: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completionApi: RecordingServiceTaskCompletionApi
  @field:Inject lateinit var worker: ConcreteWorker

  @Test fun `inherited and overridden workers are registered on the concrete bean`() {
    assertThat(Arc.container().instance(ConcreteWorker::class.java).isAvailable).isTrue()
    assertThat(subscriptionApi.subscriptions).hasSize(2)
    subscriptionApi.deliver("inherited", "task-1", emptyMap())
    subscriptionApi.deliver("overridden", "task-2", emptyMap())
    assertThat(worker.invocations).containsExactly("inherited:task-1", "overridden:task-2")
    assertThat(completionApi.completed).hasSize(2)
    assertThat(completionApi.completed[1].get()).isEqualTo(mapOf("from" to "concrete"))
  }
}
