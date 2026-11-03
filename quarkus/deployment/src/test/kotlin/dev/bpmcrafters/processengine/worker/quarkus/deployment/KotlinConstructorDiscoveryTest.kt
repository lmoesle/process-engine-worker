package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.InjectedKotlinWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.KotlinCollaborator
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.KotlinMarker
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class KotlinConstructorDiscoveryTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(InjectedKotlinWorker::class.java, KotlinCollaborator::class.java, KotlinMarker::class.java, JavaWorker::class.java) })
  }
  /** Keeps the original non-synthetic dual-constructor JVM shape, now authored in Kotlin. */
  open class JavaWorker @Inject constructor(private val collaborator: KotlinCollaborator) {
    constructor() : this(throw AssertionError("CDI must use the explicitly injected constructor"))

    @ProcessEngineWorker("java-injected")
    open fun work(): Map<String, Any?> = mapOf("from" to collaborator.text())
  }
  @field:Inject lateinit var api: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completion: RecordingServiceTaskCompletionApi
  @Test fun `uses the injected constructor and ignores the synthetic default argument constructor`() {
    assertThat(InjectedKotlinWorker::class.java.declaredConstructors).anyMatch { it.isSynthetic }
    api.deliver("kotlin-injected", "injected-task", emptyMap())
    api.deliver("java-injected", "java-task", emptyMap())
    assertThat(completion.failed).isEmpty()
    assertThat(completion.completed.map { it.get() }).containsExactly(mapOf("from" to "injected", "marker" to "marker"), mapOf("from" to "injected"))
  }
}
