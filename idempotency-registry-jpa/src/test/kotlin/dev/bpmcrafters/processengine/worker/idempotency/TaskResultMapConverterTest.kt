package dev.bpmcrafters.processengine.worker.idempotency

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.Instant

internal class TaskResultMapConverterTest {

  private val converter = TaskResultMapConverter()
  private val result = linkedMapOf<String, Any?>(
    "text" to "completed", "count" to 42, "nullable" to null,
    "nested" to arrayListOf("first", "second"), "created" to Instant.parse("2026-01-01T00:00:00Z")
  )

  @Test
  fun `writes exactly the legacy Java serialization bytes`() {
    assertThat(converter.convertToDatabaseColumn(result)).isEqualTo(legacyBytes(result))
    ObjectInputStream(ByteArrayInputStream(converter.convertToDatabaseColumn(result)!!)).use {
      assertThat(it.readObject()).isEqualTo(result)
    }
  }

  @Test
  fun `reads results written by the legacy converter`() {
    assertThat(converter.convertToEntityAttribute(legacyBytes(result))).isEqualTo(result)
  }

  @Test
  fun `preserves null and empty result semantics`() {
    assertThat(converter.convertToDatabaseColumn(null)).isNull()
    assertThat(converter.convertToDatabaseColumn(emptyMap())).isNull()
    assertThat(converter.convertToEntityAttribute(null)).isEmpty()
    assertThat(converter.convertToEntityAttribute(legacyBytes(emptyMap()))).isEmpty()
  }

  @Test
  fun `retains entity identity and schema mappings after extraction`() {
    val entity = TaskLogEntry::class.java
    assertThat(entity.name).isEqualTo("dev.bpmcrafters.processengine.worker.idempotency.TaskLogEntry")
    assertThat(entity.getAnnotation(Table::class.java).name).isEqualTo("task_log_entry_")
    val columns = mapOf(
      "taskId" to "task_id_", "processInstanceId" to "process_instance_id_",
      "createdAt" to "created_at_", "result" to "result_"
    )
    columns.forEach { (field, column) ->
      assertThat(entity.getDeclaredField(field).getAnnotation(Column::class.java).name).isEqualTo(column)
    }
    assertThat(entity.getDeclaredField("taskId").isAnnotationPresent(Id::class.java)).isTrue()
    assertThat(entity.getDeclaredField("taskId").getAnnotation(Column::class.java).length).isEqualTo(100)
    assertThat(entity.getDeclaredField("processInstanceId").getAnnotation(Column::class.java).length).isEqualTo(100)
    assertThat(entity.getDeclaredField("result").getAnnotation(Convert::class.java).converter.java)
      .isEqualTo(TaskResultMapConverter::class.java)
  }

  // Same algorithm as the Spring-only converter before extraction; do not delegate to the converter under test.
  private fun legacyBytes(value: Map<String, Any?>): ByteArray = ByteArrayOutputStream().use { bytes ->
    ObjectOutputStream(bytes).use { it.writeObject(value) }
    bytes.toByteArray()
  }
}
