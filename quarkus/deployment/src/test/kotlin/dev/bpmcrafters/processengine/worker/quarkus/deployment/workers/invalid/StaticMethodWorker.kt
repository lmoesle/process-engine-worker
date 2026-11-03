package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers.invalid

import dev.bpmcrafters.processengine.worker.quarkus.deployment.support.JvmWorkerFixtures
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_STATIC

object StaticMethodFixture {
  fun archive() = JvmWorkerFixtures.archive("StaticMethodWorker", ACC_PUBLIC or ACC_STATIC, "static")
}
