package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.ProcessEngineWorker

open class CustomBeansWorker {
  @ProcessEngineWorker(topic = "custom")
  open fun work(greeting: CustomBeans.Greeting): String = greeting.text
}
