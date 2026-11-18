package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LiteratureItemEntity
import com.example.data.LiteratureJsonCodec
import com.example.quiz.QuizGenerator
import com.example.quiz.QuizPool
import com.example.quiz.QuizQuestionType
import com.example.quiz.QuizScope
import com.example.ui.BengaliUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BCS Literature", appName)
    }

    @Test
    fun `json codec encodes and decodes skipping invalid entries`() {
        val item = LiteratureItemEntity(
            id = "id-1",
            category = "bangla",
            authorName = "মুনীর চৌধুরী",
            genre = "নাটক",
            title = "মানুষ",
            note = "মুক্তিযুদ্ধভিত্তিক",
            memorized = false,
            createdAt = 1000L,
            updatedAt = 2000L
        )
        val encoded = LiteratureJsonCodec.encode(listOf(item))
        val decoded = LiteratureJsonCodec.decode(encoded)
        assertTrue(decoded.isValidDocument)
        assertEquals(0, decoded.skippedCount)
        assertEquals(1, decoded.validItems.size)
        assertEquals("মানুষ", decoded.validItems[0].title)

        val mixedJson = """
            {
              "app": "BCS Literature",
              "version": 1,
              "items": [
                {
                  "id": "id-1",
                  "category": "bangla",
                  "authorName": "মুনীর চৌধুরী",
                  "genre": "নাটক",
                  "title": "মানুষ"
                },
                {
                  "id": "bad-1",
                  "category": "bangla",
                  "authorName": "",
                  "genre": "নাটক",
                  "title": "অসম্পূর্ণ"
                }
              ]
            }
        """.trimIndent()
        val mixedResult = LiteratureJsonCodec.decode(mixedJson)
        assertTrue(mixedResult.isValidDocument)
        assertEquals(1, mixedResult.validItems.size)
        assertEquals(1, mixedResult.skippedCount)
    }

    @Test
    fun `quiz generator builds questions with unique distractors and filters pool`() {
        val items = listOf(
            LiteratureItemEntity("1", "bangla", "মুনীর চৌধুরী", "নাটক", "মানুষ", memorized = false),
            LiteratureItemEntity("2", "bangla", "সৈয়দ ওয়ালীউল্লাহ", "উপন্যাস", "লালসালু", memorized = true),
            LiteratureItemEntity("3", "bangla", "রবীন্দ্রনাথ ঠাকুর", "কবিতা", "সোনার তরী", memorized = false),
            LiteratureItemEntity("4", "bangla", "কাজী নজরুল ইসলাম", "কাব্যগ্রন্থ", "অগ্নিবীণা", memorized = false),
            LiteratureItemEntity("5", "english", "William Shakespeare", "Drama", "Hamlet", memorized = false)
        )

        val unmemorizedBangla = QuizGenerator.filterPool(items, QuizScope.BANGLA, QuizPool.UNMEMORIZED_ONLY)
        assertEquals(3, unmemorizedBangla.size)

        val q = QuizGenerator.buildQuestion(items[0], QuizQuestionType.WHO_WROTE, items)
        assertEquals("'মানুষ' নাটকটি কার লেখা?", q.prompt)
        assertEquals(4, q.options.size)
        assertTrue(q.options.contains("মুনীর চৌধুরী"))
        assertEquals(4, q.options.distinct().size)
    }

    @Test
    fun `bengali digits formatter converts western digits`() {
        assertEquals("১২০%", BengaliUtils.toBengaliDigits("120%"))
    }
}
