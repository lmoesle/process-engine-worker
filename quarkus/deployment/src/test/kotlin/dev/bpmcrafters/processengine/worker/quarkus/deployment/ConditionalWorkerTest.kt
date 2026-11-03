package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.*
import io.quarkus.arc.Arc
import io.quarkus.arc.profile.IfBuildProfile
import io.quarkus.arc.profile.UnlessBuildProfile
import io.quarkus.arc.properties.IfBuildProperty
import io.quarkus.arc.properties.UnlessBuildProperty
import io.quarkus.test.QuarkusExtensionTest
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class ConditionalWorkerTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withoutJpa(QuarkusExtensionTest().setArchiveProducer {
      TestSupport.archive(TestProfileWorker::class.java, OtherProfileWorker::class.java, EnabledWorker::class.java,
        DisabledWorker::class.java, DisabledProducer::class.java, Producer::class.java, ProducedByMethod::class.java,
        ProducedByClass::class.java, InheritedBase::class.java, InactiveChild::class.java)
    }.overrideConfigKey("test.worker.active", "true"))
  }
  @IfBuildProfile("test") open class TestProfileWorker { @ProcessEngineWorker("profile-worker") open fun work() {} }
  @UnlessBuildProfile("test") open class OtherProfileWorker { @ProcessEngineWorker("profile-worker") open fun work() {} }
  @IfBuildProperty(name = "test.worker.active", stringValue = "true")
  open class EnabledWorker { @ProcessEngineWorker("property-worker") open fun work() {} }
  @UnlessBuildProperty(name = "test.worker.active", stringValue = "true")
  open class DisabledWorker { @ProcessEngineWorker("property-worker") private fun work() {} }

  @Singleton class Producer {
    @Produces @IfBuildProperty(name = "test.worker.active", stringValue = "false")
    fun worker() = ProducedByMethod()
  }
  @Singleton @UnlessBuildProfile("test") class DisabledProducer {
    @Produces fun worker() = ProducedByClass()
  }
  open class ProducedByMethod { @ProcessEngineWorker("property-worker") open fun work() {} }
  open class ProducedByClass { @ProcessEngineWorker("profile-worker") open fun work() {} }
  abstract class InheritedBase { @ProcessEngineWorker("inactive-inherited") open fun inherited() {} }
  @UnlessBuildProfile("test") open class InactiveChild : InheritedBase()
  @field:Inject lateinit var api: InMemoryTaskSubscriptionApi

  @Test fun `validates and registers only active class beans`() {
    assertThat(api.subscriptions.map { it.taskDescriptionKey }).containsExactlyInAnyOrder("profile-worker", "property-worker")
    assertThat(Arc.container().instance(ProducedByMethod::class.java).isAvailable).isFalse()
    assertThat(Arc.container().instance(ProducedByClass::class.java).isAvailable).isFalse()
  }
}
