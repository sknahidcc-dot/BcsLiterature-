package com.example.data

enum class LiteratureCategory(
    val value: String,
    val fullLabelBn: String,
    val shortLabelBn: String
) {
    BANGLA("bangla", "বাংলা সাহিত্য", "বাংলা"),
    ENGLISH("english", "ইংরেজি সাহিত্য", "ইংরেজি");

    companion object {
        fun fromValue(raw: String?): LiteratureCategory? {
            return when (raw?.trim()?.lowercase()) {
                "bangla" -> BANGLA
                "english" -> ENGLISH
                else -> null
            }
        }
    }
}
