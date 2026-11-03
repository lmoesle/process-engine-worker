package dev.bpmcrafters.processengine.worker.quarkus.deployment.workers

import dev.bpmcrafters.processengine.worker.idempotency.IdempotencyRegistry
import dev.bpmcrafters.processengine.worker.idempotency.InMemoryIdempotencyRegistry
import dev.bpmcrafters.processengine.worker.registrar.ParameterResolutionStrategy
import dev.bpmcrafters.processengine.worker.registrar.ParameterResolver
import dev.bpmcrafters.processengine.worker.registrar.ResultResolver
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import java.lang.reflect.Parameter

@ApplicationScoped
class CustomBeans {
  data class Greeting(val text: String)

  @Produces @Singleton
  fun parameterResolver(): ParameterResolver = ParameterResolver.builder().addStrategy(GreetingStrategy()).build()

  class GreetingStrategy : ParameterResolutionStrategy {
    override fun test(parameter: Parameter) = parameter.type == Greeting::class.java
    override fun apply(wrapper: ParameterResolutionStrategy.Wrapper): Any = Greeting("hello ${wrapper.taskInformation.taskId}")
  }

  @Produces @Singleton
  fun resultResolver(): ResultResolver = ResultResolver.builder().addStrategy(ResultResolver.ResultResolutionStrategy(
    { it.returnType == String::class.java }, { mapOf("text" to it) }
  )).build()

  @Produces @Singleton
  fun idempotencyRegistry(): IdempotencyRegistry = InMemoryIdempotencyRegistry()
}
