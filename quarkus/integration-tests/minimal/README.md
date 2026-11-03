# Packaged JVM consumer smoke

This standalone consumer intentionally has no optional JTA, Micrometer, or JPA dependencies.
It packages a real Quarkus application and launches its fast-jar during `verify`, in a separate
JVM without the extension test runner's broader classpath. The application checks actual
class absence, `NONE`/no-op collaborator selection, enabled worker registration, invocation,
and successful completion through test-only in-memory API stubs, then exits. It does not
test a live process-engine adapter or native images.

The extension suite separately verifies disabled and empty worker sets without API beans.
It does not simulate absence of optional extensions by pruning a mixed test dependency graph.

From the repository root, first install the extension and its reactor dependencies:

```shell
./mvnw -B -ntp -DskipTests -DskipExamples -DskipITests -pl :process-engine-worker-quarkus-deployment -am install
./mvnw -B -ntp -f quarkus/integration-tests/minimal/pom.xml clean verify
```

Run the library tests before the install command; `-DskipTests` avoids repeating validated tests.
The development-branch CI workflow runs both commands after its full `verify` step.

The smoke is deliberately outside the library reactor so application packaging cannot
affect library release artifacts. `-DskipITests` skips launching the packaged application.
