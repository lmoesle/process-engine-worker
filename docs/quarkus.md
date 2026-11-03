# Quarkus

The Quarkus extension discovers synchronous `@ProcessEngineWorker` methods at build time
and registers them before `StartupEvent`. It uses the framework-independent core registrar
for parameter conversion, results, completion, errors, and retries.

## Dependencies

Add the runtime extension to a Quarkus application (use the same version as the other worker modules):

```xml
<dependency>
  <groupId>dev.bpm-crafters.process-engine-worker</groupId>
  <artifactId>process-engine-worker-quarkus</artifactId>
  <version>${process-engine-worker.version}</version>
</dependency>
```

The extension builds against Quarkus 3.38.1 and targets Java 17. Import your application's
Quarkus BOM as usual. Do not add the deployment artifact to application dependencies.

Provide exactly one CDI bean implementing each of `TaskSubscriptionApi` and
`ServiceTaskCompletionApi` from the Process Engine API. These APIs normally come from
your process-engine adapter. This extension does **not** implement an adapter or deploy
BPMN/DMN resources automatically. Choose and configure an adapter compatible with your
engine and Quarkus version; no particular adapter artifact is required by this extension.

## Worker

```java
import dev.bpmcrafters.processengine.worker.ProcessEngineWorker;
import dev.bpmcrafters.processengine.worker.Variable;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Map;

@ApplicationScoped
public class ShippingWorker {
  @ProcessEngineWorker("ship-order")
  public Map<String, Object> ship(@Variable("orderId") String orderId) {
    return Map.of("shippedOrderId", orderId);
  }
}
```

Classes without a CDI scope receive `@Singleton`. Workers must be class beans, not
producer-created instances. Methods must be public, nonstatic, and concrete. Inherited
public worker methods apply to each concrete bean; an unannotated override suppresses
the inherited worker. Two concrete beans inheriting the same worker topic and tenant
therefore conflict. Annotate the desired constructor with `@Inject` if a class has both
no-argument and parameterized constructors.

Custom CDI qualifiers are supported: registration selects the exact discovered class bean,
not another bean assignable to its type. Quarkus build-profile/property conditions apply
before worker validation and registration. An inactive producer does not cause its result
class to become an automatically scoped worker.

`value` and `topic` are aliases; omit both to use the method name. A topic must be unique
per effective tenant, including the configured default tenant. Nonblank tenant strings
remain unchanged, including surrounding whitespace.

Kotlin workers use the same annotations. Enable Kotlin's `all-open` compiler plugin for
CDI normal scopes such as `jakarta.enterprise.context.ApplicationScoped` in your application.
The default variable converter uses Quarkus's Jackson `ObjectMapper`, including its customizers.
Result conversion follows the core defaults (maps); provide a custom `ResultResolver` for DTO results.

## Configuration

Use the prefix `dev.bpm-crafters.process-api.worker` in `application.properties`:

| Suffix | Default | Meaning |
| --- | --- | --- |
| `enabled` | `true` | Register discovered workers |
| `tenant-id` | unset | Default tenant for workers without a nonblank annotation tenant |
| `complete-tasks-before-commit` | `false` | Complete transactional tasks before commit instead of after the executor returns |
| `remove-task-result-on-completion` | `true` | Remove a saved result after successful completion |

Disabled registration and an empty worker set start without adapter beans. Enabled workers
fail startup with a diagnostic when either adapter API is missing or ambiguous.
Shutdown unsubscribes workers before default-priority shutdown observers. Adapters using
custom observer priorities must coordinate their ordering with the extension (priority 2100).

## Optional collaborators

All default collaborator beans use `@DefaultBean`. Supply a CDI bean to replace
`VariableConverter`, `ParameterResolver`, `ResultResolver`, `TransactionalExecutor`,
`ProcessEngineWorkerMetrics`, or `IdempotencyRegistry`.

* **Transactions:** add `io.quarkus:quarkus-narayana-jta` to enable the JTA executor.
  It joins an existing transaction or starts one. Without it, the executor is `NONE`.
  CDI `@Transactional` interception is independent of a custom executor: replacing the
  executor with `NONE` does not disable CDI interceptors. Completion after the executor
  returns is not a guarantee that an enclosing transaction has already committed.
* **Metrics:** add Quarkus Micrometer support to record core worker metrics in its
  `MeterRegistry`. Otherwise metrics are no-op.
* **Idempotency:** the default is no-op. Provide an `InMemoryIdempotencyRegistry` CDI
  bean for local, nonpersistent caching or your own implementation. The extension installs
  the executor's after-commit hook on hook-aware registries, unwrapping CDI client proxies.
  Idempotency is not an exactly-once or distributed-concurrency guarantee.
* **JPA:** add `process-engine-worker-idempotency-registry-jpa` at the worker version,
  `io.quarkus:quarkus-hibernate-orm`, JTA, and a JDBC driver. Configure the default
  datasource/persistence unit and manage the `task_log_entry_` table schema in production.
  When Hibernate ORM is enabled and the shared JPA module is present, the extension provides
  a default JPA registry. A custom registry bean takes precedence. The Quarkus-local registry
  reuses the shared entity and legacy Java-serialization converter unchanged: stored nonempty
  payload values must be Java-serializable. Existing byte format, null/empty handling, and
  schema semantics remain those of the shared module.

`autoComplete=false` disables automatic successful completion, not automatic reporting of
worker exceptions. Hook-aware registries receive the executor's commit hook; behavior of
custom registries and executors remains their responsibility.

## Limits and validation

Only synchronous worker invocation is supported; do not return asynchronous/reactive values
or use suspending worker methods. The extension includes reflection registrations, but native
images have **not** been verified. In particular, arbitrary Java-serialized JPA payloads are
not claimed to work in native images.

Run the extension suite with:

```shell
./mvnw -B -ntp -DskipExamples -DskipITests -pl :process-engine-worker-quarkus-deployment -am clean verify
```

The standalone [minimal packaged consumer](../quarkus/integration-tests/minimal) exercises
packaging, JVM startup, default fallback beans, and an enabled worker using test-only API
stubs without optional transaction/metrics/JPA dependencies. It checks class absence in the
packaged JVM, independently of the extension test runner's broader classpath. The extension
suite separately checks disabled/empty worker sets without adapter API beans, and Hibernate
ORM without the worker JPA model using application-model and index assertions.
Development-branch CI installs the validated extension locally and runs this consumer after
the full reactor verification; it does not publish the consumer as a library release artifact.
