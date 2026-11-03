package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import dev.bpmcrafters.processengine.worker.Variable
import dev.bpmcrafters.processengine.worker.quarkus.*
import dev.bpmcrafters.processengine.worker.registrar.VariableConverter
import io.quarkus.arc.deployment.AdditionalBeanBuildItem
import io.quarkus.arc.deployment.ExcludedTypeBuildItem
import io.quarkus.deployment.Capabilities
import io.quarkus.deployment.Capability
import io.quarkus.deployment.builditem.CombinedIndexBuildItem
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem
import io.quarkus.deployment.metrics.MetricsCapabilityBuildItem
import org.jboss.jandex.Index
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/** Verifies native hints as build items without claiming native-image execution. */
class ProcessorBuildStepsTest {
  private val processor = ProcessEngineWorkerProcessor()

  @Test fun `empty worker set needs no native hints`() {
    val hints = nativeHints(emptyList())
    assertThat(hints.classes).isEmpty()
    assertThat(hints.hierarchies).isEmpty()
  }

  @Test fun `registers worker and declaring class methods even without variables or reference results`() {
    val index = Index.of(NativeBase::class.java, NativeWorker::class.java)
    val method = index.getClassByName(NativeBase::class.java).firstMethod("nothing")
    val hints = nativeHints(listOf(ProcessEngineWorkerMethodBuildItem(index.getClassByName(NativeWorker::class.java), method, "nothing")))
    assertThat(hints.classes).hasSize(1)
    assertThat(hints.classes[0].classNames).containsExactlyInAnyOrder(NativeBase::class.java.name, NativeWorker::class.java.name)
    assertThat(hints.classes[0].isMethods).isTrue()
    assertThat(hints.hierarchies).isEmpty()
  }

  @Test fun `registers variable and result hierarchies and only custom converter construction`() {
    val index = Index.of(NativeBase::class.java, NativeWorker::class.java, Payload::class.java, Converter::class.java)
    val bean = index.getClassByName(NativeWorker::class.java)
    val methods = bean.methods().filter { it.hasAnnotation(ProcessEngineWorkerProcessor.PROCESS_ENGINE_WORKER) }
      .map { ProcessEngineWorkerMethodBuildItem(bean, it, it.name()) }
    val hints = nativeHints(methods)
    assertThat(hints.classes).hasSize(2)
    val converter = hints.classes.single { Converter::class.java.name in it.classNames }
    assertThat(converter.classNames).containsExactly(Converter::class.java.name)
    assertThat(converter.isConstructors).isTrue()
    assertThat(converter.isFields).isTrue()
    assertThat(converter.isMethods).isFalse()
    assertThat(hints.hierarchies.map { it.type.name().toString() }).containsExactlyInAnyOrder(Payload::class.java.name, String::class.java.name)
    assertThat(hints.hierarchies).allSatisfy {
      assertThat(it.source).isEqualTo(ProcessEngineWorkerProcessor.FEATURE)
      assertThat(it.index.getClassByName(Payload::class.java)).isNotNull()
    }
  }

  @Test fun `selects fallbacks without transactions or metrics`() {
    assertSelection(emptySet(), null, NoneTransactionalExecutorProducer::class.java, NoOpProcessEngineWorkerMetricsProducer::class.java, emptyList())
  }

  @Test fun `selects noop metrics when available metrics capability does not support Micrometer`() {
    assertSelection(emptySet(), MetricsCapabilityBuildItem { false }, NoneTransactionalExecutorProducer::class.java,
      NoOpProcessEngineWorkerMetricsProducer::class.java, emptyList())
  }

  @Test fun `selects optional implementations and excludes their fallback producers`() {
    assertSelection(setOf(Capability.TRANSACTIONS), MetricsCapabilityBuildItem { true }, JtaTransactionalExecutor::class.java,
      MicrometerProcessEngineWorkerMetrics::class.java, listOf(NoneTransactionalExecutorProducer::class.java.name, NoOpProcessEngineWorkerMetricsProducer::class.java.name))
  }

  @Test fun `resolves explicit sentinels equal aliases and blank tenants without trimming nonblank tenants`() {
    val bean = Index.of(Aliases::class.java).getClassByName(Aliases::class.java)
    val sentinel = bean.firstMethod("sentinel")
    val equal = bean.firstMethod("equal")
    assertThat(ProcessEngineWorkerProcessor.effectiveTopic(sentinel.annotation(ProcessEngineWorkerProcessor.PROCESS_ENGINE_WORKER), sentinel)).isEqualTo("sentinel")
    assertThat(ProcessEngineWorkerProcessor.effectiveTopic(equal.annotation(ProcessEngineWorkerProcessor.PROCESS_ENGINE_WORKER), equal)).isEqualTo("same")
    assertThat(ProcessEngineWorkerProcessor.tenantId(null)).isEmpty()
    assertThat(ProcessEngineWorkerProcessor.tenantId(sentinel.annotation(ProcessEngineWorkerProcessor.PROCESS_ENGINE_WORKER))).isEmpty()
    assertThat(ProcessEngineWorkerProcessor.tenantId(equal.annotation(ProcessEngineWorkerProcessor.PROCESS_ENGINE_WORKER))).isEqualTo(" tenant ")
  }

  private fun nativeHints(methods: List<ProcessEngineWorkerMethodBuildItem>): NativeHints {
    val index = Index.of(NativeBase::class.java, NativeWorker::class.java, Payload::class.java, Converter::class.java)
    val classes = mutableListOf<ReflectiveClassBuildItem>()
    val hierarchies = mutableListOf<ReflectiveHierarchyBuildItem>()
    processor.nativeImage(CombinedIndexBuildItem(index, index), methods, { classes.add(it) }, { hierarchies.add(it) })
    return NativeHints(classes, hierarchies)
  }

  private fun assertSelection(capabilities: Set<String>, metrics: MetricsCapabilityBuildItem?, executor: Class<*>,
    expectedMetrics: Class<*>, excludedNames: List<String>) {
    val beans = mutableListOf<AdditionalBeanBuildItem>()
    val excluded = mutableListOf<ExcludedTypeBuildItem>()
    processor.additionalBeans(Capabilities(capabilities), java.util.Optional.ofNullable(metrics), { beans.add(it) }, { excluded.add(it) })
    assertThat(beans.flatMap { it.beanClasses }).containsExactlyInAnyOrder(ProcessEngineWorkerProducers::class.java.name,
      ProcessEngineWorkerRegistration::class.java.name, executor.name, expectedMetrics.name)
    assertThat(beans).allSatisfy { assertThat(it.isRemovable).isFalse() }
    assertThat(excluded.map { it.match }).containsExactlyInAnyOrderElementsOf(excludedNames)
  }

  private data class NativeHints(val classes: List<ReflectiveClassBuildItem>, val hierarchies: List<ReflectiveHierarchyBuildItem>)
  abstract class NativeBase { @ProcessEngineWorker open fun nothing(unannotated: String) {} }
  open class NativeWorker : NativeBase() {
    @ProcessEngineWorker open fun primitive(): Int = 1
    @ProcessEngineWorker open fun convert(@Variable implicit: Payload, @Variable(converter = Variable.DefaultVariableConverter::class) explicit: String,
      @Variable(converter = Converter::class) custom: Payload, unannotated: String): Payload = custom
  }
  class Payload
  class Converter : VariableConverter {
    companion object { @JvmField val INSTANCE = Converter() }
    override fun <T : Any> mapToType(value: Any?, type: Class<T>): T = type.cast(value)
  }
  open class Aliases {
    @ProcessEngineWorker(topic = "__unset", value = "__unset", tenantId = " ") open fun sentinel() {}
    @ProcessEngineWorker(topic = "same", value = "same", tenantId = " tenant ") open fun equal() {}
  }
}
