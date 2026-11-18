package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "literature_items")
data class LiteratureItemEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val category: String, // "bangla" | "english"
    val authorName: String,
    val genre: String,
    val title: String,
    val note: String = "",
    val memorized: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val categoryEnum: LiteratureCategory
        get() = LiteratureCategory.fromValue(category) ?: LiteratureCategory.BANGLA
}
