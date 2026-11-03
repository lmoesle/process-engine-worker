package dev.bpmcrafters.processengine.worker.quarkus.deployment.support

import dev.bpmcrafters.processengineapi.Empty
import dev.bpmcrafters.processengineapi.task.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

open class RecordingServiceTaskCompletionApi : ServiceTaskCompletionApi {
  val completed = CopyOnWriteArrayList<CompleteTaskCmd>()
  val completedByError = CopyOnWriteArrayList<CompleteTaskByErrorCmd>()
  val failed = CopyOnWriteArrayList<FailTaskCmd>()
  @Volatile private var listener: Runnable = Runnable {}

  override fun completeTask(cmd: CompleteTaskCmd): CompletableFuture<Empty> {
    listener.run()
    completed.add(cmd)
    return CompletableFuture.completedFuture(Empty)
  }

  override fun completeTaskByError(cmd: CompleteTaskByErrorCmd): CompletableFuture<Empty> {
    listener.run()
    completedByError.add(cmd)
    return CompletableFuture.completedFuture(Empty)
  }

  override fun failTask(cmd: FailTaskCmd): CompletableFuture<Empty> {
    listener.run()
    failed.add(cmd)
    return CompletableFuture.completedFuture(Empty)
  }

  fun setListener(listener: Runnable) { this.listener = listener }

  fun reset() {
    completed.clear()
    completedByError.clear()
    failed.clear()
    listener = Runnable {}
  }
}
