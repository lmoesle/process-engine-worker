package dev.bpmcrafters.processengine.worker.quarkus.deployment.support

import dev.bpmcrafters.processengineapi.MetaInfo
import dev.bpmcrafters.processengineapi.MetaInfoAware
import dev.bpmcrafters.processengineapi.impl.task.AbstractTaskSubscriptionApiImpl
import dev.bpmcrafters.processengineapi.impl.task.InMemSubscriptionRepository
import dev.bpmcrafters.processengineapi.impl.task.TaskSubscriptionHandle
import dev.bpmcrafters.processengineapi.task.TaskInformation

open class InMemoryTaskSubscriptionApi(
  private val repository: InMemSubscriptionRepository = InMemSubscriptionRepository()
) : AbstractTaskSubscriptionApiImpl(repository) {
  class TestMetaInfo : MetaInfo
  override fun meta(instance: MetaInfoAware): MetaInfo = TestMetaInfo()

  val subscriptions: List<TaskSubscriptionHandle> get() = repository.getTaskSubscriptions()

  fun getSubscription(topic: String): TaskSubscriptionHandle = subscriptions.firstOrNull { it.taskDescriptionKey == topic }
    ?: throw IllegalArgumentException("No subscription for topic $topic, known: $subscriptions")

  fun deliver(topic: String, taskInformation: TaskInformation, payload: Map<String, Any?>) {
    getSubscription(topic).action.accept(taskInformation, payload)
  }

  fun deliver(topic: String, taskId: String, payload: Map<String, Any?>) = deliver(topic, taskInformation(taskId), payload)

  companion object {
    fun taskInformation(taskId: String) = TaskInformation(taskId, mapOf("processInstanceId" to "instance-$taskId", "retries" to "3"))
  }
}
