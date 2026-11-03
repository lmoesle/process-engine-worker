package dev.bpmcrafters.processengine.worker.idempotency

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/**
 * This converter converts a result map to a byte array (and back) using Java serialization.
 *
 * It is important to note that empty results are converted to `null` to save space.
 */
@Converter(autoApply = false)
class TaskResultMapConverter : AttributeConverter<Map<String, Any?>, ByteArray> {

  override fun convertToDatabaseColumn(attribute: Map<String, Any?>?): ByteArray? {
    if (attribute.isNullOrEmpty()) {
      return null
    }
    ByteArrayOutputStream().use { bytes ->
      ObjectOutputStream(bytes).use { it.writeObject(attribute) }
      return bytes.toByteArray()
    }
  }

  override fun convertToEntityAttribute(dbData: ByteArray?): Map<String, Any?> {
    if (dbData == null) {
      return mapOf()
    }
    @Suppress("UNCHECKED_CAST")
    return ObjectInputStream(ByteArrayInputStream(dbData)).use { it.readObject() as Map<String, Any?> }
  }

}
