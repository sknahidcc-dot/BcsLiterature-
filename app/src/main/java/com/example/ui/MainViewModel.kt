package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.JsonImportResult
import com.example.data.LiteratureCategory
import com.example.data.LiteratureItemEntity
import com.example.data.LiteratureRepository
import com.example.quiz.QuizAnswerStatus
import com.example.quiz.QuizGenerator
import com.example.quiz.QuizPhase
import com.example.quiz.QuizPool
import com.example.quiz.QuizScope
import com.example.quiz.QuizUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppTab(val labelBn: String) {
    HOME("হোম"),
    ADD("যোগ করুন"),
    LIST("তালিকা"),
    QUIZ("পরীক্ষা")
}

enum class CategoryFilter(val labelBn: String) {
    ALL("সব"),
    BANGLA("বাংলা"),
    ENGLISH("ইংরেজি")
}

enum class MemorizedFilter(val labelBn: String) {
    ALL("সব"),
    MEMORIZED("মুখস্থ"),
    UNMEMORIZED("মুখস্থ নয়")
}

data class CategoryBreakdown(
    val total: Int = 0,
    val memorized: Int = 0
) {
    val remaining: Int get() = (total - memorized).coerceAtLeast(0)
}

data class DashboardStats(
    val total: Int = 0,
    val memorized: Int = 0,
    val remaining: Int = 0,
    val bangla: CategoryBreakdown = CategoryBreakdown(),
    val english: CategoryBreakdown = CategoryBreakdown()
) {
    val progressFraction: Float
        get() = if (total == 0) 0f else memorized.toFloat() / total.toFloat()
}

data class ListFilterState(
    val searchQuery: String = "",
    val categoryFilter: CategoryFilter = CategoryFilter.ALL,
    val memorizedFilter: MemorizedFilter = MemorizedFilter.ALL
)

data class PendingImport(
    val result: JsonImportResult
)

enum class StudyScope(val labelBn: String) {
    ALL("সব"),
    BANGLA("বাংলা"),
    ENGLISH("ইংরেজি")
}

data class RandomStudyUiState(
    val isOpen: Boolean = false,
    val scope: StudyScope = StudyScope.ALL,
    val unmemorizedOnly: Boolean = false,
    val hideDetailsMode: Boolean = false,
    val isDetailsRevealed: Boolean = true,
    val currentItem: LiteratureItemEntity? = null,
    val history: List<LiteratureItemEntity> = emptyList(),
    val historyIndex: Int = -1,
    val matchingCount: Int = 0
)

sealed interface UiMessageEvent {
    data class Toast(val text: String) : UiMessageEvent
    data class Snackbar(val text: String) : UiMessageEvent
}

class MainViewModel(
    private val repository: LiteratureRepository
) : ViewModel() {

    private val _isSplashVisible = MutableStateFlow(true)
    val isSplashVisible: StateFlow<Boolean> = _isSplashVisible.asStateFlow()

    private val _showPermissionDialog = MutableStateFlow(false)
    val showPermissionDialog: StateFlow<Boolean> = _showPermissionDialog.asStateFlow()

    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _isSettingsOpen = MutableStateFlow(false)
    val isSettingsOpen: StateFlow<Boolean> = _isSettingsOpen.asStateFlow()

    private val _listFilterState = MutableStateFlow(ListFilterState())
    val listFilterState: StateFlow<ListFilterState> = _listFilterState.asStateFlow()

    private val _quizState = MutableStateFlow(QuizUiState())
    val quizState: StateFlow<QuizUiState> = _quizState.asStateFlow()

    private val _studyState = MutableStateFlow(RandomStudyUiState())
    val studyState: StateFlow<RandomStudyUiState> = _studyState.asStateFlow()

    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImport: StateFlow<PendingImport?> = _pendingImport.asStateFlow()

    private val _events = MutableSharedFlow<UiMessageEvent>()
    val events: SharedFlow<UiMessageEvent> = _events.asSharedFlow()

    val allItems: StateFlow<List<LiteratureItemEntity>> = repository.allItemsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val lastBackupTimestamp: StateFlow<Long> = repository.lastBackupTimestampFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val backupTreeUri: StateFlow<String?> = repository.backupTreeUriFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val dashboardStats: StateFlow<DashboardStats> = allItems
        .map { items ->
            val banglaItems = items.filter { it.categoryEnum == LiteratureCategory.BANGLA }
            val englishItems = items.filter { it.categoryEnum == LiteratureCategory.ENGLISH }
            val totalMem = items.count { it.memorized }
            DashboardStats(
                total = items.size,
                memorized = totalMem,
                remaining = (items.size - totalMem).coerceAtLeast(0),
                bangla = CategoryBreakdown(
                    total = banglaItems.size,
                    memorized = banglaItems.count { it.memorized }
                ),
                english = CategoryBreakdown(
                    total = englishItems.size,
                    memorized = englishItems.count { it.memorized }
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardStats()
        )

    val filteredItems: StateFlow<List<LiteratureItemEntity>> = combine(
        allItems,
        _listFilterState
    ) { items, filter ->
        val query = filter.searchQuery.trim().lowercase()
        items.filter { item ->
            val matchesCategory = when (filter.categoryFilter) {
                CategoryFilter.ALL -> true
                CategoryFilter.BANGLA -> item.categoryEnum == LiteratureCategory.BANGLA
                CategoryFilter.ENGLISH -> item.categoryEnum == LiteratureCategory.ENGLISH
            }
            val matchesMemorized = when (filter.memorizedFilter) {
                MemorizedFilter.ALL -> true
                MemorizedFilter.MEMORIZED -> item.memorized
                MemorizedFilter.UNMEMORIZED -> !item.memorized
            }
            val matchesQuery = if (query.isEmpty()) {
                true
            } else {
                item.title.lowercase().contains(query) ||
                    item.authorName.lowercase().contains(query) ||
                    item.genre.lowercase().contains(query) ||
                    item.note.lowercase().contains(query)
            }
            matchesCategory && matchesMemorized && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            val restored = repository.tryAutoRestoreOnLaunch()
            if (restored) {
                _events.emit(UiMessageEvent.Toast("ব্যাকআপ থেকে ডাটা ফিরিয়ে আনা হয়েছে"))
            }

            // Update quiz available count whenever items change
            launch {
                allItems.collect { items ->
                    _quizState.update { state ->
                        val count = QuizGenerator.filterPool(items, state.scope, state.pool).size
                        state.copy(availableCount = count)
                    }
                }
            }

            if (!repository.isPermissionPromptShown()) {
                _showPermissionDialog.value = true
            }

            delay(450L)
            _isSplashVisible.value = false
        }
    }

    fun selectTab(tab: AppTab) {
        _isSettingsOpen.value = false
        _studyState.update { it.copy(isOpen = false) }
        _currentTab.value = tab
    }

    fun openSettings() {
        _studyState.update { it.copy(isOpen = false) }
        _isSettingsOpen.value = true
    }

    fun closeSettings() {
        _isSettingsOpen.value = false
    }

    // --- Random Study (Flashcard Reading Mode) ---

    fun openRandomStudy(
        initialScope: StudyScope = StudyScope.ALL,
        unmemorizedOnly: Boolean = false
    ) {
        _isSettingsOpen.value = false
        val pool = filterStudyPool(allItems.value, initialScope, unmemorizedOnly)
        val picked = pickWeightedStudyItem(pool, lastId = null)
        _studyState.value = RandomStudyUiState(
            isOpen = true,
            scope = initialScope,
            unmemorizedOnly = unmemorizedOnly,
            hideDetailsMode = false,
            isDetailsRevealed = true,
            currentItem = picked,
            history = listOfNotNull(picked),
            historyIndex = if (picked != null) 0 else -1,
            matchingCount = pool.size
        )
    }

    fun closeRandomStudy() {
        _studyState.update { it.copy(isOpen = false) }
    }

    fun setStudyScope(scope: StudyScope) {
        val current = _studyState.value
        val pool = filterStudyPool(allItems.value, scope, current.unmemorizedOnly)
        val picked = pickWeightedStudyItem(pool, lastId = null)
        _studyState.value = current.copy(
            scope = scope,
            currentItem = picked,
            history = listOfNotNull(picked),
            historyIndex = if (picked != null) 0 else -1,
            isDetailsRevealed = !current.hideDetailsMode,
            matchingCount = pool.size
        )
    }

    fun setStudyUnmemorizedOnly(unmemorizedOnly: Boolean) {
        val current = _studyState.value
        val pool = filterStudyPool(allItems.value, current.scope, unmemorizedOnly)
        val picked = pickWeightedStudyItem(pool, lastId = null)
        _studyState.value = current.copy(
            unmemorizedOnly = unmemorizedOnly,
            currentItem = picked,
            history = listOfNotNull(picked),
            historyIndex = if (picked != null) 0 else -1,
            isDetailsRevealed = !current.hideDetailsMode,
            matchingCount = pool.size
        )
    }

    fun toggleStudyHideDetails(hide: Boolean) {
        _studyState.update {
            it.copy(
                hideDetailsMode = hide,
                isDetailsRevealed = !hide
            )
        }
    }

    fun revealStudyDetails() {
        _studyState.update { it.copy(isDetailsRevealed = true) }
    }

    fun nextRandomStudyItem() {
        val current = _studyState.value
        if (current.historyIndex in 0 until current.history.lastIndex) {
            val nextIdx = current.historyIndex + 1
            _studyState.value = current.copy(
                currentItem = current.history[nextIdx],
                historyIndex = nextIdx,
                isDetailsRevealed = !current.hideDetailsMode
            )
            return
        }

        val pool = filterStudyPool(allItems.value, current.scope, current.unmemorizedOnly)
        val nextItem = pickWeightedStudyItem(pool, lastId = current.currentItem?.id)
        if (nextItem == null) {
            _studyState.update { it.copy(currentItem = null, matchingCount = 0) }
            return
        }
        val updatedHistory = (current.history + nextItem).takeLast(30)
        _studyState.value = current.copy(
            currentItem = nextItem,
            history = updatedHistory,
            historyIndex = updatedHistory.lastIndex,
            isDetailsRevealed = !current.hideDetailsMode,
            matchingCount = pool.size
        )
    }

    fun previousStudyItem() {
        val current = _studyState.value
        if (current.historyIndex > 0) {
            val prevIdx = current.historyIndex - 1
            _studyState.value = current.copy(
                currentItem = current.history[prevIdx],
                historyIndex = prevIdx,
                isDetailsRevealed = !current.hideDetailsMode
            )
        }
    }

    fun toggleCurrentStudyMemorized() {
        val item = _studyState.value.currentItem ?: return
        val nextMemorized = !item.memorized
        viewModelScope.launch {
            repository.setMemorized(item.id, nextMemorized)
            _studyState.update { state ->
                val cur = state.currentItem ?: return@update state
                val updated = cur.copy(memorized = nextMemorized)
                val updatedHistory = state.history.map { if (it.id == cur.id) updated else it }
                val poolCount = filterStudyPool(allItems.value, state.scope, state.unmemorizedOnly).size
                state.copy(
                    currentItem = updated,
                    history = updatedHistory,
                    matchingCount = poolCount
                )
            }
        }
    }

    private fun filterStudyPool(
        items: List<LiteratureItemEntity>,
        scope: StudyScope,
        unmemorizedOnly: Boolean
    ): List<LiteratureItemEntity> {
        return items.filter { item ->
            val matchesScope = when (scope) {
                StudyScope.ALL -> true
                StudyScope.BANGLA -> item.categoryEnum == LiteratureCategory.BANGLA
                StudyScope.ENGLISH -> item.categoryEnum == LiteratureCategory.ENGLISH
            }
            val matchesMem = !unmemorizedOnly || !item.memorized
            matchesScope && matchesMem
        }
    }

    private fun pickWeightedStudyItem(
        pool: List<LiteratureItemEntity>,
        lastId: String?
    ): LiteratureItemEntity? {
        if (pool.isEmpty()) return null
        val candidates = if (pool.size > 1 && lastId != null) {
            pool.filterNot { it.id == lastId }.ifEmpty { pool }
        } else {
            pool
        }
        val weights = candidates.map { if (it.memorized) 1 else 4 }
        val totalWeight = weights.sum()
        var target = kotlin.random.Random.nextInt(totalWeight)
        for (i in candidates.indices) {
            target -= weights[i]
            if (target < 0) return candidates[i]
        }
        return candidates.last()
    }

    fun dismissPermissionDialog() {
        _showPermissionDialog.value = false
        viewModelScope.launch {
            repository.markPermissionPromptShown()
        }
    }

    fun updateSearchQuery(query: String) {
        _listFilterState.update { it.copy(searchQuery = query) }
    }

    fun updateCategoryFilter(filter: CategoryFilter) {
        _listFilterState.update { it.copy(categoryFilter = filter) }
    }

    fun updateMemorizedFilter(filter: MemorizedFilter) {
        _listFilterState.update { it.copy(memorizedFilter = filter) }
    }

    fun addItem(
        category: LiteratureCategory,
        authorName: String,
        genre: String,
        title: String,
        note: String
    ) {
        val now = System.currentTimeMillis()
        val entity = LiteratureItemEntity(
            id = UUID.randomUUID().toString(),
            category = category.value,
            authorName = authorName.trim(),
            genre = genre.trim(),
            title = title.trim(),
            note = note.trim(),
            memorized = false,
            createdAt = now,
            updatedAt = now
        )
        viewModelScope.launch {
            repository.addOrUpdateItem(entity)
            _events.emit(UiMessageEvent.Snackbar("সফলভাবে সংরক্ষিত হয়েছে"))
        }
    }

    fun updateItem(
        original: LiteratureItemEntity,
        category: LiteratureCategory,
        authorName: String,
        genre: String,
        title: String,
        note: String
    ) {
        val updated = original.copy(
            category = category.value,
            authorName = authorName.trim(),
            genre = genre.trim(),
            title = title.trim(),
            note = note.trim(),
            updatedAt = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.addOrUpdateItem(updated)
            _events.emit(UiMessageEvent.Snackbar("তথ্যটি হালনাগাদ করা হয়েছে"))
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.deleteItem(id)
            _events.emit(UiMessageEvent.Snackbar("তথ্যটি মুছে ফেলা হয়েছে"))
        }
    }

    fun toggleMemorized(item: LiteratureItemEntity) {
        val next = !item.memorized
        viewModelScope.launch {
            repository.setMemorized(item.id, next)
        }
    }

    // --- Quiz Flow ---

    fun setQuizScope(scope: QuizScope) {
        val count = QuizGenerator.filterPool(allItems.value, scope, _quizState.value.pool).size
        _quizState.update { it.copy(scope = scope, availableCount = count) }
    }

    fun setQuizPool(pool: QuizPool) {
        val count = QuizGenerator.filterPool(allItems.value, _quizState.value.scope, pool).size
        _quizState.update { it.copy(pool = pool, availableCount = count) }
    }

    fun startQuiz() {
        val items = allItems.value
        val state = _quizState.value
        val poolItems = QuizGenerator.filterPool(items, state.scope, state.pool)
        if (poolItems.isEmpty()) {
            viewModelScope.launch {
                _events.emit(UiMessageEvent.Snackbar("এই ফিল্টারে কোনো তথ্য নেই। আগে তথ্য যোগ করুন।"))
            }
            return
        }

        val plannedTotal = poolItems.size.coerceIn(1, 15)
        val firstQuestion = QuizGenerator.pickNextQuestion(
            poolItems = poolItems,
            allItems = items,
            recentIds = emptyList()
        )

        _quizState.update {
            it.copy(
                phase = QuizPhase.ACTIVE,
                availableCount = poolItems.size,
                currentQuestion = firstQuestion,
                questionNumber = 1,
                totalPlannedQuestions = plannedTotal,
                isOptionsRevealed = false,
                selectedOption = null,
                answerStatus = QuizAnswerStatus.UNANSWERED,
                correctCount = 0,
                wrongCount = 0,
                revealedCount = 0,
                recentItemIds = listOfNotNull(firstQuestion?.item?.id)
            )
        }
    }

    fun revealQuizOptions() {
        _quizState.update { it.copy(isOptionsRevealed = true) }
    }

    fun submitQuizAnswer(option: String) {
        val state = _quizState.value
        val question = state.currentQuestion ?: return
        if (state.answerStatus != QuizAnswerStatus.UNANSWERED) return

        val isCorrect = option == question.correctOption
        _quizState.update {
            it.copy(
                isOptionsRevealed = true,
                selectedOption = option,
                answerStatus = if (isCorrect) QuizAnswerStatus.CORRECT else QuizAnswerStatus.WRONG,
                correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount,
                wrongCount = if (!isCorrect) it.wrongCount + 1 else it.wrongCount
            )
        }
    }

    fun revealAnswerWithoutScoring() {
        val state = _quizState.value
        if (state.answerStatus != QuizAnswerStatus.UNANSWERED) return
        _quizState.update {
            it.copy(
                isOptionsRevealed = true,
                selectedOption = null,
                answerStatus = QuizAnswerStatus.REVEALED,
                revealedCount = it.revealedCount + 1
            )
        }
    }

    fun markQuizItemMemorized(memorized: Boolean) {
        val currentQ = _quizState.value.currentQuestion ?: return
        viewModelScope.launch {
            repository.setMemorized(currentQ.item.id, memorized)
            _quizState.update { state ->
                val q = state.currentQuestion ?: return@update state
                state.copy(
                    currentQuestion = q.copy(
                        item = q.item.copy(memorized = memorized)
                    )
                )
            }
            val msg = if (memorized) {
                "মুখস্থ হিসেবে চিহ্নিত হয়েছে ✓"
            } else {
                "আবার পরীক্ষার জন্য রাখা হয়েছে"
            }
            _events.emit(UiMessageEvent.Toast(msg))
        }
    }

    fun nextQuizQuestion() {
        val state = _quizState.value
        if (state.questionNumber >= state.totalPlannedQuestions) {
            _quizState.update { it.copy(phase = QuizPhase.SUMMARY) }
            return
        }

        val items = allItems.value
        val poolItems = QuizGenerator.filterPool(items, state.scope, state.pool)
        if (poolItems.isEmpty()) {
            _quizState.update { it.copy(phase = QuizPhase.SUMMARY) }
            return
        }

        val nextQuestion = QuizGenerator.pickNextQuestion(
            poolItems = poolItems,
            allItems = items,
            recentIds = state.recentItemIds
        )
        if (nextQuestion == null) {
            _quizState.update { it.copy(phase = QuizPhase.SUMMARY) }
            return
        }

        _quizState.update {
            it.copy(
                currentQuestion = nextQuestion,
                questionNumber = it.questionNumber + 1,
                isOptionsRevealed = false,
                selectedOption = null,
                answerStatus = QuizAnswerStatus.UNANSWERED,
                recentItemIds = (it.recentItemIds + nextQuestion.item.id).takeLast(5)
            )
        }
    }

    fun finishQuizEarly() {
        _quizState.update { it.copy(phase = QuizPhase.SUMMARY) }
    }

    fun resetQuizToSetup() {
        val count = QuizGenerator.filterPool(
            allItems.value,
            _quizState.value.scope,
            _quizState.value.pool
        ).size
        _quizState.update {
            QuizUiState(
                phase = QuizPhase.SETUP,
                scope = it.scope,
                pool = it.pool,
                availableCount = count
            )
        }
    }

    // --- Backup, Export, Import ---

    fun runManualBackup() {
        viewModelScope.launch {
            val ok = repository.runBackupNow()
            val msg = if (ok) {
                "Documents/BCSLiterature/backup.json এ ব্যাকআপ সংরক্ষিত হয়েছে"
            } else {
                "ব্যাকআপ সংরক্ষণ করা যায়নি। স্টোরেজ অনুমতি বা ফোল্ডার যাচাই করুন।"
            }
            _events.emit(UiMessageEvent.Toast(msg))
        }
    }

    fun exportJsonForShare(onReady: (Intent) -> Unit) {
        viewModelScope.launch {
            val intent = repository.createShareExportIntent()
            if (intent != null) {
                onReady(intent)
            } else {
                _events.emit(UiMessageEvent.Toast("এক্সপোর্ট ফাইল তৈরি করা যায়নি"))
            }
        }
    }

    fun handleSelectedImportUri(uri: Uri) {
        viewModelScope.launch {
            val result = repository.parseImportUri(uri)
            if (result == null || !result.isValidDocument || result.validItems.isEmpty()) {
                _events.emit(UiMessageEvent.Toast("সঠিক BCS Literature JSON ফাইল পাওয়া যায়নি"))
            } else {
                _pendingImport.value = PendingImport(result)
            }
        }
    }

    fun confirmImportMerge() {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        viewModelScope.launch {
            repository.importMerge(pending.result.validItems)
            val addedCount = BengaliUtils.toBengaliDigits(pending.result.validItems.size)
            val msg = if (pending.result.skippedCount > 0) {
                "${addedCount}টি তথ্য মার্জ হয়েছে (${BengaliUtils.toBengaliDigits(pending.result.skippedCount)}টি অবৈধ তথ্য বাদ দেওয়া হয়েছে)"
            } else {
                "${addedCount}টি তথ্য সফলভাবে মার্জ হয়েছে"
            }
            _events.emit(UiMessageEvent.Toast(msg))
        }
    }

    fun confirmImportReplace() {
        val pending = _pendingImport.value ?: return
        _pendingImport.value = null
        viewModelScope.launch {
            repository.importReplace(pending.result.validItems)
            val addedCount = BengaliUtils.toBengaliDigits(pending.result.validItems.size)
            val msg = if (pending.result.skippedCount > 0) {
                "ডাটা রিপ্লেস করে ${addedCount}টি তথ্য যোগ হয়েছে (${BengaliUtils.toBengaliDigits(pending.result.skippedCount)}টি অবৈধ বাদ পড়েছে)"
            } else {
                "সব ডাটা রিপ্লেস করে ${addedCount}টি তথ্য বসানো হয়েছে"
            }
            _events.emit(UiMessageEvent.Toast(msg))
        }
    }

    fun dismissImportDialog() {
        _pendingImport.value = null
    }

    fun saveLinkedBackupFolder(uriString: String) {
        viewModelScope.launch {
            repository.saveBackupTreeUri(uriString)
            _events.emit(UiMessageEvent.Toast("ব্যাকআপ ফোল্ডার সফলভাবে যুক্ত হয়েছে"))
        }
    }

    class Factory(private val repository: LiteratureRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(repository) as T
        }
    }
}
