package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import io.quarkus.test.QuarkusExtensionTest
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.inject.Qualifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ExactWorkerBeanTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(Base::class.java, Child::class.java, QualifiedWorker::class.java, Special::class.java) })
  }
  open class Base {
    @ProcessEngineWorker("base-worker") open fun work(): Map<String, Any?> = mapOf("worker" to "base")
  }
  @ApplicationScoped class Child : Base() {
    @ProcessEngineWorker("child-worker") override fun work(): Map<String, Any?> = mapOf("worker" to "child")
  }
  @Qualifier @Retention(AnnotationRetention.RUNTIME)
  annotation class Special
  @Special @ApplicationScoped class QualifiedWorker {
    @ProcessEngineWorker("qualified-worker") fun work(): Map<String, Any?> = mapOf("worker" to "qualified")
  }
  @field:Inject lateinit var api: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completion: RecordingServiceTaskCompletionApi
  @Test fun `invokes each exact class bean including qualified and proxied workers`() {
    api.deliver("base-worker", "base-task", emptyMap())
    api.deliver("child-worker", "child-task", emptyMap())
    api.deliver("qualified-worker", "qualified-task", emptyMap())
    assertThat(completion.failed).isEmpty()
    assertThat(completion.completed.map { it.get() }).containsExactly(mapOf("worker" to "base"), mapOf("worker" to "child"), mapOf("worker" to "qualified"))
  }
}
