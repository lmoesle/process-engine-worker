package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.idempotency.TaskLogEntry
import dev.bpmcrafters.processengine.worker.quarkus.JpaIdempotencyRegistryProducer
import dev.bpmcrafters.processengine.worker.quarkus.NoOpIdempotencyRegistryProducer
import io.quarkus.arc.deployment.AdditionalBeanBuildItem
import io.quarkus.arc.deployment.ExcludedTypeBuildItem
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem
import io.quarkus.deployment.Capabilities
import io.quarkus.deployment.Capability
import io.quarkus.deployment.builditem.CombinedIndexBuildItem
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem
import org.jboss.jandex.Index
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JpaProcessorBuildStepsTest {
  @Test fun `absent Hibernate capability selects noop even when model is indexed`() {
    val result = select(emptySet())
    assertThat(result.beans).hasSize(1)
    assertThat(result.beans.single().beanClasses).containsExactly(NoOpIdempotencyRegistryProducer::class.java.name)
    assertThat(result.excluded.map { it.match }).containsExactly(JpaIdempotencyRegistryProducer::class.java.name)
    assertThat(result.models).isEmpty()
    assertThat(result.reflection).isEmpty()
    assertThat(result.errors).isEmpty()
  }

  @Test fun `Hibernate and JPA model without JTA produce actionable validation error and no JPA bean`() {
    val result = select(setOf(Capability.HIBERNATE_ORM))
    assertThat(result.errors).hasSize(1)
    assertThat(result.errors.single().values).hasSize(1)
    assertThat(result.errors.single().values.single()).hasMessageContaining("PROCESS-ENGINE-WORKER-035").hasMessageContaining("quarkus-narayana-jta")
    assertThat(result.excluded.map { it.match }).containsExactly(NoOpIdempotencyRegistryProducer::class.java.name)
    assertThat(result.beans).isEmpty()
    assertThat(result.models).isEmpty()
    assertThat(result.reflection).isEmpty()
  }

  private fun select(capabilities: Set<String>): Result {
    val index = Index.of(TaskLogEntry::class.java)
    val result = Result()
    ProcessEngineWorkerJpaProcessor().jpaIdempotencyRegistry(Capabilities(capabilities), CombinedIndexBuildItem(index, index),
      { result.beans.add(it) }, { result.excluded.add(it) }, { result.models.add(it) }, { result.reflection.add(it) }, { result.errors.add(it) })
    return result
  }
  private class Result {
    val beans = mutableListOf<AdditionalBeanBuildItem>()
    val excluded = mutableListOf<ExcludedTypeBuildItem>()
    val models = mutableListOf<AdditionalJpaModelBuildItem>()
    val reflection = mutableListOf<ReflectiveClassBuildItem>()
    val errors = mutableListOf<ValidationErrorBuildItem>()
  }
}
