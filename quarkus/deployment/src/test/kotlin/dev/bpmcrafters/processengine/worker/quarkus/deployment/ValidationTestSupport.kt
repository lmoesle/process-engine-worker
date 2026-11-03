package dev.bpmcrafters.processengine.worker.quarkus.deployment

import dev.bpmcrafters.processengine.worker.quarkus.ProcessEngineWorkerValidationException
import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.TestSupport
import io.quarkus.test.QuarkusExtensionTest
import org.assertj.core.api.Assertions.assertThat
import org.jboss.shrinkwrap.api.spec.JavaArchive

object ValidationTestSupport {
  fun expectValidationError(messagePart: String, vararg classes: Class<*>): QuarkusExtensionTest =
    expectValidationError(messagePart) { TestSupport.archive(*classes) }

  fun expectValidationError(messagePart: String, archive: () -> JavaArchive): QuarkusExtensionTest =
    TestSupport.withoutJpa(QuarkusExtensionTest().setArchiveProducer(archive).assertException { throwable ->
      assertThat(TestSupport.anyInChain(throwable) {
        it is ProcessEngineWorkerValidationException && it.message.orEmpty().contains(messagePart)
      }).`as`("expected a ProcessEngineWorkerValidationException containing '$messagePart' in $throwable").isTrue()
    })
}
