package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import io.quarkus.test.QuarkusExtensionTest
import jakarta.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset

class InterfaceWorkerDiscoveryTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest().setArchiveProducer {
      val name = InterfaceWorkerDiscoveryTest::class.java.name.replace('.', '/') + "\$SuperclassOverride"
      TestSupport.archive(Root::class.java, Other::class.java, AnnotatedOverride::class.java, UnannotatedOverride::class.java,
        Direct::class.java, AnnotatedChild::class.java, SuppressedChild::class.java, PlainSuperclass::class.java, JvmWorkerFixtures::class.java)
        .add(ByteArrayAsset(JvmWorkerFixtures.subclass(name, PlainSuperclass::class.java, Root::class.java)), "$name.class")
    })
  }
  interface Root { @ProcessEngineWorker("interface-root") fun work(): Map<String, Any?> = mapOf("from" to "root") }
  interface Other
  interface AnnotatedOverride : Root {
    @ProcessEngineWorker("interface-child") override fun work(): Map<String, Any?> = mapOf("from" to "child")
  }
  interface UnannotatedOverride : Root { override fun work(): Map<String, Any?> = mapOf("from" to "suppressed") }
  open class Direct : Root, Other
  open class AnnotatedChild : AnnotatedOverride
  open class SuppressedChild : UnannotatedOverride
  open class PlainSuperclass { open fun work(): Map<String, Any?> = mapOf("from" to "class") }
  @field:Inject lateinit var api: InMemoryTaskSubscriptionApi
  @field:Inject lateinit var completion: RecordingServiceTaskCompletionApi

  @Test fun `only effective annotated default methods are registered and invoked`() {
    assertThat(api.subscriptions.map { it.taskDescriptionKey }).containsExactlyInAnyOrder("interface-root", "interface-child")
    api.deliver("interface-root", "root-task", emptyMap())
    api.deliver("interface-child", "child-task", emptyMap())
    assertThat(completion.failed).isEmpty()
    assertThat(completion.completed.map { it.get() }).containsExactly(mapOf("from" to "root"), mapOf("from" to "child"))
  }
}
