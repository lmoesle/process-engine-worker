package dev.bpmcrafters.processengine.worker.quarkus.deployment.support

import io.quarkus.test.QuarkusExtensionTest
import org.jboss.shrinkwrap.api.ShrinkWrap
import org.jboss.shrinkwrap.api.spec.JavaArchive

object TestSupport {
  fun archive(vararg classes: Class<*>): JavaArchive = archiveWithoutAdapters(TestApiProducer::class.java,
    InMemoryTaskSubscriptionApi::class.java, RecordingServiceTaskCompletionApi::class.java, *classes)

  fun archiveWithoutAdapters(vararg classes: Class<*>): JavaArchive = ShrinkWrap.create(JavaArchive::class.java).also { archive ->
    listOf(TestSupport::class.java, *classes).forEach {
      archive.addClass(it)
      addNestedClasses(archive, it)
    }
  }

  /** Kotlin companions and nested fixtures must also be available in the reloaded application archive. */
  fun addNestedClasses(archive: JavaArchive, type: Class<*>) {
    type.declaredClasses.forEach { nested ->
      archive.addClass(nested)
      addNestedClasses(archive, nested)
    }
  }

  fun withoutJpa(test: QuarkusExtensionTest): QuarkusExtensionTest = test
    .overrideConfigKey("quarkus.hibernate-orm.enabled", "false")
    .overrideConfigKey("quarkus.datasource.devservices.enabled", "false")

  fun withJpa(test: QuarkusExtensionTest, databaseName: String): QuarkusExtensionTest = test
    .overrideConfigKey("quarkus.datasource.db-kind", "h2")
    .overrideConfigKey("quarkus.datasource.jdbc.url", "jdbc:h2:mem:$databaseName;DB_CLOSE_DELAY=-1")
    .overrideConfigKey("quarkus.hibernate-orm.schema-management.strategy", "drop-and-create")

  fun anyInChain(throwable: Throwable?, predicate: (Throwable) -> Boolean): Boolean = throwable != null &&
    (predicate(throwable) || throwable.suppressed.any { anyInChain(it, predicate) } ||
      (throwable.cause !== throwable && anyInChain(throwable.cause, predicate)))
}
