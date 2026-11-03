package dev.bpmcrafters.processengine.worker.quarkus.deployment.support

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset
import org.jboss.shrinkwrap.api.spec.JavaArchive
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type

/** Kotlin has no package-private methods. Emit the exact JVM access flags without Java source or internal-name mangling. */
object JvmWorkerFixtures {
  /** Java's inherited class-method precedence over interface defaults cannot be expressed without a Kotlin override. */
  fun subclass(name: String, superclass: Class<*>, vararg interfaces: Class<*>): ByteArray {
    val writer = ClassWriter(0)
    val parent = Type.getInternalName(superclass)
    writer.visit(V17, ACC_PUBLIC or ACC_SUPER, name, null, parent, interfaces.map { Type.getInternalName(it) }.toTypedArray())
    writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
      visitCode()
      visitVarInsn(ALOAD, 0)
      visitMethodInsn(INVOKESPECIAL, parent, "<init>", "()V", false)
      visitInsn(RETURN)
      visitMaxs(1, 1)
      visitEnd()
    }
    writer.visitEnd()
    return writer.toByteArray()
  }

  fun archive(simpleName: String, methodAccess: Int, topic: String): JavaArchive {
    val name = "dev/bpmcrafters/processengine/worker/quarkus/deployment/workers/invalid/$simpleName"
    val writer = ClassWriter(0)
    writer.visit(V17, ACC_PUBLIC or ACC_SUPER, name, null, "java/lang/Object", null)
    writer.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null).apply {
      visitCode()
      visitVarInsn(ALOAD, 0)
      visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
      visitInsn(RETURN)
      visitMaxs(1, 1)
      visitEnd()
    }
    writer.visitMethod(methodAccess, "work", "()V", null, null).apply {
      visitAnnotation(Type.getDescriptor(ProcessEngineWorker::class.java), true).apply {
        visit("topic", topic)
        visitEnd()
      }
      visitCode()
      visitInsn(RETURN)
      visitMaxs(0, if (methodAccess and ACC_STATIC == 0) 1 else 0)
      visitEnd()
    }
    writer.visitEnd()
    return TestSupport.archive(JvmWorkerFixtures::class.java).add(ByteArrayAsset(writer.toByteArray()), "$name.class")
  }
}
