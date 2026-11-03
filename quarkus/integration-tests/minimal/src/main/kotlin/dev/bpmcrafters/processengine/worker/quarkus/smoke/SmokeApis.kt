package dev.bpmcrafters.processengine.worker.quarkus.smoke

import dev.bpmcrafters.processengineapi.Empty
import dev.bpmcrafters.processengineapi.MetaInfo
import dev.bpmcrafters.processengineapi.MetaInfoAware
import dev.bpmcrafters.processengineapi.impl.task.AbstractTaskSubscriptionApiImpl
import dev.bpmcrafters.processengineapi.impl.task.InMemSubscriptionRepository
import dev.bpmcrafters.processengineapi.task.*
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import java.util.concurrent.CompletableFuture

/** Test-only synchronous API stubs: no process engine or network connection. */
@Singleton
class SmokeApis {
  @Produces @Singleton fun subscriptions() = Subscriptions(InMemSubscriptionRepository())
  @Produces @Singleton fun completion() = Completion()

  class Subscriptions(private val repository: InMemSubscriptionRepository) : AbstractTaskSubscriptionApiImpl(repository) {
    override fun meta(instance: MetaInfoAware): MetaInfo = object : MetaInfo {}

    fun deliver() {
      val subscriptions = repository.getTaskSubscriptions()
      check(subscriptions.size == 1) { "Expected one API subscription" }
      subscriptions[0].action.accept(TaskInformation("smoke-task", emptyMap()), emptyMap())
    }
  }

  class Completion : ServiceTaskCompletionApi {
    var payload: Map<String, Any?>? = null
    var failed = false

    override fun completeTask(cmd: CompleteTaskCmd): CompletableFuture<Empty> {
      payload = cmd.get()
      return CompletableFuture.completedFuture(Empty)
    }
    override fun completeTaskByError(cmd: CompleteTaskByErrorCmd): CompletableFuture<Empty> {
      failed = true
      return CompletableFuture.completedFuture(Empty)
    }
    override fun failTask(cmd: FailTaskCmd): CompletableFuture<Empty> {
      failed = true
      return CompletableFuture.completedFuture(Empty)
    }
  }
}
