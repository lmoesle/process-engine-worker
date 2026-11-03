package dev.bpmcrafters.processengine.worker.quarkus.idempotency

import dev.bpmcrafters.processengine.worker.idempotency.IdempotencyRegistry
import dev.bpmcrafters.processengine.worker.idempotency.TaskLogEntry
import dev.bpmcrafters.processengine.worker.transaction.TransactionalExecutor
import dev.bpmcrafters.processengineapi.CommonRestrictions
import dev.bpmcrafters.processengineapi.task.TaskInformation
import jakarta.persistence.EntityManager
import java.time.Clock
import java.util.function.Supplier

/**
 * Quarkus JPA registry using the shared entity and its legacy Java-serialization converter unchanged.
 * Writes join an existing transaction or start one through the configured executor.
 */
class EntityManagerJpaIdempotencyRegistry(
  private val entityManager: Supplier<EntityManager>,
  private val transactionalExecutor: TransactionalExecutor,
  private val clock: Clock = Clock.systemUTC()
) : IdempotencyRegistry {

  override fun register(taskInformation: TaskInformation, result: Map<String, Any?>) {
    transactionalExecutor.executeInTransaction {
      val em = entityManager.get()
      val entry = TaskLogEntry(
        taskInformation.taskId,
        requireNotNull(taskInformation.meta[CommonRestrictions.PROCESS_INSTANCE_ID]) {
          "Task information of ${taskInformation.taskId} must contain the process instance id."
        },
        clock.instant(),
        result
      )
      if (em.find(TaskLogEntry::class.java, taskInformation.taskId) == null) {
        em.persist(entry)
      } else {
        em.merge(entry)
      }
      em.flush()
    }
  }

  override fun getTaskResult(taskInformation: TaskInformation): Map<String, Any?>? =
    transactionalExecutor.executeInTransaction {
      entityManager.get().find(TaskLogEntry::class.java, taskInformation.taskId)?.result
    }

  override fun removeTaskResult(taskId: String) {
    transactionalExecutor.executeInTransaction {
      entityManager.get().createQuery("delete from TaskLogEntry e where e.taskId = :taskId")
        .setParameter("taskId", taskId).executeUpdate()
    }
  }

  override fun removeAllTaskResults(processInstanceId: String) {
    transactionalExecutor.executeInTransaction {
      entityManager.get().createQuery("delete from TaskLogEntry e where e.processInstanceId = :processInstanceId")
        .setParameter("processInstanceId", processInstanceId).executeUpdate()
    }
  }
}
