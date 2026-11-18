package com.example.quiz

import com.example.data.LiteratureCategory
import com.example.data.LiteratureItemEntity
import kotlin.random.Random

object QuizGenerator {

    fun filterPool(
        allItems: List<LiteratureItemEntity>,
        scope: QuizScope,
        pool: QuizPool
    ): List<LiteratureItemEntity> {
        return allItems.filter { item ->
            val matchesScope = when (scope) {
                QuizScope.BANGLA -> item.categoryEnum == LiteratureCategory.BANGLA
                QuizScope.ENGLISH -> item.categoryEnum == LiteratureCategory.ENGLISH
                QuizScope.BOTH -> true
            }
            val matchesPool = when (pool) {
                QuizPool.UNMEMORIZED_ONLY -> !item.memorized
                QuizPool.ALL -> true
            }
            matchesScope && matchesPool
        }
    }

    /**
     * Picks an item using weighted random selection:
     * unmemorized items have 4x higher weight than memorized items.
     */
    fun pickNextQuestion(
        poolItems: List<LiteratureItemEntity>,
        allItems: List<LiteratureItemEntity>,
        recentIds: List<String>,
        random: Random = Random.Default
    ): QuizQuestion? {
        if (poolItems.isEmpty()) return null

        val candidates = if (poolItems.size > 1) {
            val filtered = poolItems.filterNot { it.id == recentIds.lastOrNull() }
            filtered.ifEmpty { poolItems }
        } else {
            poolItems
        }

        val selectedItem = pickWeightedItem(candidates, random)
        val type = QuizQuestionType.entries[random.nextInt(QuizQuestionType.entries.size)]
        return buildQuestion(selectedItem, type, allItems, random)
    }

    private fun pickWeightedItem(
        items: List<LiteratureItemEntity>,
        random: Random
    ): LiteratureItemEntity {
        val weights = items.map { item -> if (item.memorized) 1 else 4 }
        val totalWeight = weights.sum()
        var target = random.nextInt(totalWeight)
        for (i in items.indices) {
            target -= weights[i]
            if (target < 0) return items[i]
        }
        return items.last()
    }

    fun buildQuestion(
        item: LiteratureItemEntity,
        type: QuizQuestionType,
        allItems: List<LiteratureItemEntity>,
        random: Random = Random.Default
    ): QuizQuestion {
        val prompt: String
        val correctAnswer: String
        val extractor: (LiteratureItemEntity) -> String

        when (type) {
            QuizQuestionType.WHO_WROTE -> {
                prompt = "'${item.title}' ${item.genre}টি কার লেখা?"
                correctAnswer = item.authorName.trim()
                extractor = { it.authorName.trim() }
            }
            QuizQuestionType.WHICH_WORK -> {
                prompt = "${item.authorName} এর লেখা কোনটি?"
                correctAnswer = item.title.trim()
                extractor = { it.title.trim() }
            }
            QuizQuestionType.WHICH_GENRE -> {
                prompt = "'${item.title}' কোন ধরনের সাহিত্য?"
                correctAnswer = item.genre.trim()
                extractor = { it.genre.trim() }
            }
        }

        // Prefer distractors from the same category first, then from all saved items
        val sameCategoryPool = allItems
            .asSequence()
            .filter { it.id != item.id && it.category == item.category }
            .map(extractor)
            .filter { it.isNotBlank() && !it.equals(correctAnswer, ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .toList()
            .shuffled(random)

        val otherCategoryPool = allItems
            .asSequence()
            .filter { it.id != item.id && it.category != item.category }
            .map(extractor)
            .filter { candidate ->
                candidate.isNotBlank() &&
                    !candidate.equals(correctAnswer, ignoreCase = true) &&
                    sameCategoryPool.none { it.equals(candidate, ignoreCase = true) }
            }
            .distinctBy { it.lowercase() }
            .toList()
            .shuffled(random)

        val distractors = (sameCategoryPool + otherCategoryPool).take(3)
        val options = (listOf(correctAnswer) + distractors).shuffled(random)

        return QuizQuestion(
            item = item,
            type = type,
            prompt = prompt,
            options = options,
            correctOption = correctAnswer
        )
    }
}
