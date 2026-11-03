package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.JvmWorkerFixtures

object PackagePrivateMethodFixture {
  fun archive() = JvmWorkerFixtures.archive("PackagePrivateMethodWorker", 0, "package-private")
}
