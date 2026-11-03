package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.idempotency.IdempotencyRegistry
import dev.bpmcrafters.processengine.worker.idempotency.NoOpIdempotencyRegistry
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.TestSupport
import dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.SimpleWorker
import io.quarkus.arc.ClientProxy
import io.quarkus.maven.dependency.ArtifactKey
import io.quarkus.narayana.jta.QuarkusTransaction
import io.quarkus.test.QuarkusExtensionTest
import io.quarkus.deployment.Capabilities
import io.quarkus.deployment.Capability
import io.quarkus.deployment.builditem.CombinedIndexBuildItem
import io.quarkus.deployment.builditem.ServiceStartBuildItem
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem
import jakarta.inject.Inject
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.Id
import org.assertj.core.api.Assertions.assertThat
import org.jboss.jandex.DotName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class HibernateWithoutWorkerJpaModelTest {
  companion object {
    @JvmField @RegisterExtension val TEST = TestSupport.withJpa(QuarkusExtensionTest()
      .setArchiveProducer { TestSupport.archive(SimpleWorker::class.java, BusinessEntity::class.java) }
      .setExcludedDependencies(setOf(ArtifactKey.of("dev.bpm-crafters.process-engine-worker", "process-engine-worker-idempotency-registry-jpa")))
      .addBuildChainCustomizer { builder -> builder.addBuildStep { context ->
        // Class.forName could fall back to the test runner's parent; check the actual application model and index.
        val model = context.consume(CurateOutcomeBuildItem::class.java).applicationModel
        assertThat(model.dependencies).noneMatch { it.key == ArtifactKey.of("dev.bpm-crafters.process-engine-worker", "process-engine-worker-idempotency-registry-jpa") }
        assertThat(context.consume(CombinedIndexBuildItem::class.java).index.getClassByName(DotName.createSimple("dev.bpmcrafters.processengine.worker.idempotency.TaskLogEntry"))).isNull()
        assertThat(context.consume(Capabilities::class.java).isPresent(Capability.HIBERNATE_ORM)).isTrue()
        context.produce(ServiceStartBuildItem("test-hibernate-without-worker-model"))
      }.consumes(CurateOutcomeBuildItem::class.java).consumes(CombinedIndexBuildItem::class.java)
        .consumes(Capabilities::class.java).produces(ServiceStartBuildItem::class.java).build()
      }, "without-worker-model")
  }
  @Entity(name = "BusinessEntity")
  open class BusinessEntity() {
    @field:Id var id: Long? = null
    constructor(id: Long) : this() { this.id = id }
  }
  @field:Inject lateinit var registry: IdempotencyRegistry
  @field:Inject lateinit var entityManager: EntityManager

  @Test fun `falls back to noop while the applications Hibernate persistence unit works`() {
    assertThat(ClientProxy.unwrap(registry)).isInstanceOf(NoOpIdempotencyRegistry::class.java)
    QuarkusTransaction.requiringNew().run { entityManager.persist(BusinessEntity(1L)) }
    val count = QuarkusTransaction.requiringNew().call { entityManager.createQuery("select count(e) from BusinessEntity e", Long::class.javaObjectType).singleResult }
    assertThat(count).isEqualTo(1L)
  }
}
