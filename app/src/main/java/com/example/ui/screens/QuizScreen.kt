package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.quiz.QuizAnswerStatus
import com.example.quiz.QuizPhase
import com.example.quiz.QuizPool
import com.example.quiz.QuizScope
import com.example.quiz.QuizUiState
import com.example.ui.BengaliUtils
import com.example.ui.theme.QuizCorrectContainerDark
import com.example.ui.theme.QuizCorrectContainerLight
import com.example.ui.theme.QuizCorrectDark
import com.example.ui.theme.QuizCorrectLight
import com.example.ui.theme.QuizWrongContainerDark
import com.example.ui.theme.QuizWrongContainerLight
import com.example.ui.theme.QuizWrongDark
import com.example.ui.theme.QuizWrongLight
import kotlin.math.roundToInt

private val optionLabelsBn = listOf("ক", "খ", "গ", "ঘ")

@Composable
fun QuizScreen(
    state: QuizUiState,
    onSelectScope: (QuizScope) -> Unit,
    onSelectPool: (QuizPool) -> Unit,
    onStartQuiz: () -> Unit,
    onRevealOptions: () -> Unit,
    onSubmitAnswer: (String) -> Unit,
    onRevealAnswer: () -> Unit,
    onMarkMemorized: (Boolean) -> Unit,
    onNextQuestion: () -> Unit,
    onFinishEarly: () -> Unit,
    onResetToSetup: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.phase != QuizPhase.SETUP) {
        BackHandler {
            onResetToSetup()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (state.phase) {
            QuizPhase.SETUP -> QuizSetupView(
                state = state,
                onSelectScope = onSelectScope,
                onSelectPool = onSelectPool,
                onStartQuiz = onStartQuiz
            )
            QuizPhase.ACTIVE -> QuizActiveView(
                state = state,
                onRevealOptions = onRevealOptions,
                onSubmitAnswer = onSubmitAnswer,
                onRevealAnswer = onRevealAnswer,
                onMarkMemorized = onMarkMemorized,
                onNextQuestion = onNextQuestion,
                onFinishEarly = onFinishEarly
            )
            QuizPhase.SUMMARY -> QuizSummaryView(
                state = state,
                onPlayAgain = onStartQuiz,
                onChangeSettings = onResetToSetup,
                onGoHome = onGoHome
            )
        }
    }
}

@Composable
private fun QuizSetupView(
    state: QuizUiState,
    onSelectScope: (QuizScope) -> Unit,
    onSelectPool: (QuizPool) -> Unit,
    onStartQuiz: () -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "পরীক্ষার সেটিংস",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "সাহিত্যের বিভাগ",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        QuizScope.entries.forEachIndexed { index, scope ->
                            SegmentedButton(
                                selected = state.scope == scope,
                                onClick = { onSelectScope(scope) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = QuizScope.entries.size
                                ),
                                modifier = Modifier.testTag("quiz_scope_${scope.name.lowercase()}")
                            ) {
                                Text(scope.labelBn)
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "প্রশ্নের পুল",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        QuizPool.entries.forEachIndexed { index, pool ->
                            SegmentedButton(
                                selected = state.pool == pool,
                                onClick = { onSelectPool(pool) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = QuizPool.entries.size
                                ),
                                modifier = Modifier.testTag("quiz_pool_${pool.name.lowercase()}")
                            ) {
                                Text(
                                    text = pool.labelBn,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (state.availableCount > 0) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (state.availableCount > 0) {
                            "এই ফিল্টারে ${BengaliUtils.toBengaliDigits(state.availableCount)}টি আইটেম থেকে এলোমেলো প্রশ্ন তৈরি হবে। যেগুলো মুখস্থ হয়নি সেগুলো বেশি বার আসবে।"
                        } else {
                            "এই ফিল্টারে কোনো তথ্য নেই। প্রথমে 'যোগ করুন' থেকে তথ্য যোগ করুন অথবা 'সব' নির্বাচন করুন।"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Button(
                    onClick = onStartQuiz,
                    enabled = state.availableCount > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .testTag("quiz_start_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "পরীক্ষা শুরু করুন",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun QuizActiveView(
    state: QuizUiState,
    onRevealOptions: () -> Unit,
    onSubmitAnswer: (String) -> Unit,
    onRevealAnswer: () -> Unit,
    onMarkMemorized: (Boolean) -> Unit,
    onNextQuestion: () -> Unit,
    onFinishEarly: () -> Unit
) {
    val question = state.currentQuestion ?: return
    val isDark = isSystemInDarkTheme()
    val correctColor = if (isDark) QuizCorrectDark else QuizCorrectLight
    val correctBg = if (isDark) QuizCorrectContainerDark else QuizCorrectContainerLight
    val wrongColor = if (isDark) QuizWrongDark else QuizWrongLight
    val wrongBg = if (isDark) QuizWrongContainerDark else QuizWrongContainerLight

    val isResolved = state.answerStatus != QuizAnswerStatus.UNANSWERED
    val progress = if (state.totalPlannedQuestions == 0) 0f else {
        state.questionNumber.toFloat() / state.totalPlannedQuestions.toFloat()
    }

    Column(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Quiz Progress Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "প্রশ্ন ${BengaliUtils.toBengaliDigits(state.questionNumber)} / ${BengaliUtils.toBengaliDigits(state.totalPlannedQuestions)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onFinishEarly,
                modifier = Modifier.testTag("quiz_finish_early_button")
            ) {
                Text("পরীক্ষা শেষ করুন")
            }
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            strokeCap = StrokeCap.Round
        )

        // Question Prompt Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = question.item.categoryEnum.fullLabelBn,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = question.prompt,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Options Box (Blurred until tapped!)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .then(
                    if (!state.isOptionsRevealed) {
                        Modifier
                            .clickable { onRevealOptions() }
                            .testTag("quiz_blurred_options_area")
                    } else {
                        Modifier
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (!state.isOptionsRevealed) {
                            Modifier
                                .blur(14.dp)
                                .alpha(0.16f)
                        } else {
                            Modifier
                        }
                    ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                question.options.forEachIndexed { idx, option ->
                    val isSelected = state.selectedOption == option
                    val isCorrectOption = option == question.correctOption

                    val cardColor = when {
                        !isResolved -> MaterialTheme.colorScheme.surface
                        isCorrectOption -> correctBg
                        isSelected && !isCorrectOption -> wrongBg
                        else -> MaterialTheme.colorScheme.surface
                    }
                    val borderColor = when {
                        !isResolved -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                        isCorrectOption -> correctColor
                        isSelected && !isCorrectOption -> wrongColor
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardColor,
                        border = BorderStroke(
                            width = if (isResolved && (isCorrectOption || isSelected)) 2.dp else 1.dp,
                            color = borderColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = state.isOptionsRevealed && !isResolved) {
                                onSubmitAnswer(option)
                            }
                            .testTag("quiz_option_$idx")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = optionLabelsBn.getOrElse(idx) { "${idx + 1}" },
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isResolved && isCorrectOption) FontWeight.Bold else FontWeight.Medium
                                )
                            }

                            if (isResolved && isCorrectOption) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "সঠিক উত্তর",
                                    tint = correctColor
                                )
                            } else if (isResolved && isSelected && !isCorrectOption) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "ভুল উত্তর",
                                    tint = wrongColor
                                )
                            }
                        }
                    }
                }
            }

            // Frosted prompt overlay when options are blurred
            if (!state.isOptionsRevealed) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier.matchParentSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Visibility,
                            contentDescription = "অপশন দেখুন",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "অপশনগুলো দেখতে এখানে ট্যাপ করুন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "আগে মনে করার চেষ্টা করুন, তারপর উত্তর বাছাই করুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // "উত্তর দেখুন" button before answering
        if (!isResolved) {
            OutlinedButton(
                onClick = onRevealAnswer,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("quiz_reveal_answer_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("উত্তর দেখুন")
            }
        }

        // Answer Result + Note + Memorize Actions + Next Question
        AnimatedVisibility(visible = isResolved) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val (bannerText, bannerBg, bannerFg) = when (state.answerStatus) {
                    QuizAnswerStatus.CORRECT -> Triple(
                        "চমৎকার! আপনার উত্তর সঠিক হয়েছে।",
                        correctBg,
                        correctColor
                    )
                    QuizAnswerStatus.WRONG -> Triple(
                        "ভুল উত্তর! সঠিক উত্তর: ${question.correctOption}",
                        wrongBg,
                        wrongColor
                    )
                    QuizAnswerStatus.REVEALED -> Triple(
                        "সঠিক উত্তর: ${question.correctOption}",
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    QuizAnswerStatus.UNANSWERED -> Triple("", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface)
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = bannerBg,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = bannerText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = bannerFg,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                // Revealed Note
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "তথ্য ও নোট:",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${question.item.title} (${question.item.genre}) — ${question.item.authorName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = question.item.note.ifBlank { "এই তথ্যের সাথে কোনো অতিরিক্ত নোট নেই।" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // "মুখস্থ হয়েছে ✓" & "আবার পরীক্ষা করব" buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isMem = question.item.memorized
                    if (isMem) {
                        Button(
                            onClick = { onMarkMemorized(true) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("quiz_mark_memorized_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = correctColor,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("মুখস্থ হয়েছে ✓")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = { onMarkMemorized(true) },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("quiz_mark_memorized_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("মুখস্থ হয়েছে ✓")
                        }
                    }

                    OutlinedButton(
                        onClick = { onMarkMemorized(false) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("quiz_repeat_later_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("আবার পরীক্ষা করব")
                    }
                }

                Button(
                    onClick = onNextQuestion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("quiz_next_question_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (state.questionNumber >= state.totalPlannedQuestions) {
                            "ফলাফল দেখুন"
                        } else {
                            "পরের প্রশ্ন"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun QuizSummaryView(
    state: QuizUiState,
    onPlayAgain: () -> Unit,
    onChangeSettings: () -> Unit,
    onGoHome: () -> Unit
) {
    val totalAttempted = state.correctCount + state.wrongCount
    val accuracy = if (totalAttempted == 0) 0 else {
        ((state.correctCount.toFloat() / totalAttempted.toFloat()) * 100f).roundToInt()
    }

    Column(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quiz_summary_card"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(16.dp)
                            .size(44.dp)
                    )
                }

                Text(
                    text = "পরীক্ষা সম্পন্ন হয়েছে!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "সঠিকতার হার: ${BengaliUtils.toBengaliDigits(accuracy)}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryStatBox(
                        label = "সঠিক উত্তর",
                        value = state.correctCount,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatBox(
                        label = "ভুল উত্তর",
                        value = state.wrongCount,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryStatBox(
                        label = "উত্তর দেখা",
                        value = state.revealedCount,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Button(
            onClick = onPlayAgain,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .testTag("quiz_play_again_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("আবার পরীক্ষা দিন")
        }

        FilledTonalButton(
            onClick = onChangeSettings,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("বিষয় বা ফিল্টার পরিবর্তন করুন")
        }

        OutlinedButton(
            onClick = onGoHome,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(imageVector = Icons.Rounded.Home, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("হোমে ফিরে যান")
        }
    }
}

@Composable
private fun SummaryStatBox(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = BengaliUtils.toBengaliDigits(value),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
