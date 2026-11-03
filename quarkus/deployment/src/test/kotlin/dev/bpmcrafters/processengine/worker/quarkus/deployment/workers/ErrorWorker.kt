package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.BpmnErrorOccurred
import dev.bpmcrafters.processengine.worker.FailJobException
import dev.bpmcrafters.processengine.worker.ProcessEngineWorker
import java.time.Duration

open class ErrorWorker {
  @ProcessEngineWorker(topic = "bpmn-error")
  @Throws(BpmnErrorOccurred::class)
  open fun bpmnError(): Unit = throw BpmnErrorOccurred("business error happened", "BUSINESS_ERROR", mapOf("reason" to "test"))

  @ProcessEngineWorker(topic = "runtime-error")
  open fun runtimeError(): Unit = throw IllegalStateException("boom")

  @ProcessEngineWorker(topic = "fail-job")
  @Throws(FailJobException::class)
  open fun failJob(): Unit = throw FailJobException("fail with retries", null, 7, Duration.ofSeconds(30))
}
