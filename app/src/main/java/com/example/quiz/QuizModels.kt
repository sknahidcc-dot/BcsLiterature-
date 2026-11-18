package com.example.quiz

import com.example.data.LiteratureItemEntity

enum class QuizScope(val labelBn: String) {
    BANGLA("বাংলা"),
    ENGLISH("ইংরেজি"),
    BOTH("উভয়")
}

enum class QuizPool(val labelBn: String) {
    UNMEMORIZED_ONLY("শুধু মুখস্থ হয়নি"),
    ALL("সব")
}

enum class QuizQuestionType {
    WHO_WROTE,   // "'{title}' {genre}টি কার লেখা?" -> options are author names
    WHICH_GENRE  // "'{title}' কোন ধরনের সাহিত্য?" -> options are genres
}

data class QuizQuestion(
    val item: LiteratureItemEntity,
    val type: QuizQuestionType,
    val prompt: String,
    val options: List<String>,
    val correctOption: String
)

enum class QuizAnswerStatus {
    UNANSWERED,
    CORRECT,
    WRONG,
    REVEALED
}

enum class QuizPhase {
    SETUP,
    ACTIVE,
    SUMMARY
}

data class QuizUiState(
    val phase: QuizPhase = QuizPhase.SETUP,
    val scope: QuizScope = QuizScope.BOTH,
    val pool: QuizPool = QuizPool.UNMEMORIZED_ONLY,
    val availableCount: Int = 0,
    val currentQuestion: QuizQuestion? = null,
    val questionNumber: Int = 0,
    val totalPlannedQuestions: Int = 0,
    val isOptionsRevealed: Boolean = false,
    val selectedOption: String? = null,
    val answerStatus: QuizAnswerStatus = QuizAnswerStatus.UNANSWERED,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val revealedCount: Int = 0,
    val recentItemIds: List<String> = emptyList()
)
