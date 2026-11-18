package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class JsonImportResult(
    val validItems: List<LiteratureItemEntity>,
    val skippedCount: Int,
    val isValidDocument: Boolean
)

object LiteratureJsonCodec {
    private const val APP_NAME = "BCS Literature"
    private const val FORMAT_VERSION = 1

    fun encode(items: List<LiteratureItemEntity>): String {
        val root = JSONObject()
        root.put("app", APP_NAME)
        root.put("version", FORMAT_VERSION)

        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("category", item.category)
            obj.put("authorName", item.authorName)
            obj.put("genre", item.genre)
            obj.put("title", item.title)
            obj.put("note", item.note)
            obj.put("memorized", item.memorized)
            obj.put("createdAt", item.createdAt)
            obj.put("updatedAt", item.updatedAt)
            array.put(obj)
        }
        root.put("items", array)
        return root.toString(2)
    }

    fun decode(rawJson: String): JsonImportResult {
        if (rawJson.isBlank()) {
            return JsonImportResult(emptyList(), 0, isValidDocument = false)
        }
        return try {
            val root = JSONObject(rawJson)
            val appField = root.optString("app", "")
            if (appField.isNotBlank() && appField != APP_NAME) {
                return JsonImportResult(emptyList(), 0, isValidDocument = false)
            }
            val array = root.optJSONArray("items")
                ?: return JsonImportResult(emptyList(), 0, isValidDocument = false)

            val parsed = mutableListOf<LiteratureItemEntity>()
            var skipped = 0
            val now = System.currentTimeMillis()

            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i)
                if (obj == null) {
                    skipped++
                    continue
                }

                val category = LiteratureCategory.fromValue(obj.optString("category", ""))
                val authorName = obj.optString("authorName", "").trim()
                val genre = obj.optString("genre", "").trim()
                val title = obj.optString("title", "").trim()

                if (category == null || authorName.isEmpty() || genre.isEmpty() || title.isEmpty()) {
                    skipped++
                    continue
                }

                val id = obj.optString("id", "").trim().ifEmpty { UUID.randomUUID().toString() }
                val note = obj.optString("note", "").trim()
                val memorized = obj.optBoolean("memorized", false)
                val createdAt = obj.optLong("createdAt", now).let { if (it > 0L) it else now }
                val updatedAt = obj.optLong("updatedAt", createdAt).let { if (it > 0L) it else createdAt }

                parsed.add(
                    LiteratureItemEntity(
                        id = id,
                        category = category.value,
                        authorName = authorName,
                        genre = genre,
                        title = title,
                        note = note,
                        memorized = memorized,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                )
            }

            JsonImportResult(
                validItems = parsed,
                skippedCount = skipped,
                isValidDocument = true
            )
        } catch (_: Exception) {
            JsonImportResult(emptyList(), 0, isValidDocument = false)
        }
    }
}
