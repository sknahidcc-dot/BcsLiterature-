package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.data.LiteratureCategory

private val banglaGenreSuggestions = listOf("নাটক", "উপন্যাস", "কবিতা", "কাব্যগ্রন্থ", "প্রবন্ধ", "ছোটগল্প", "আত্মজীবনী")
private val englishGenreSuggestions = listOf("Drama", "Novel", "Poetry", "Poem", "Essay", "Short Story", "Sonnet")

@Composable
fun AddItemScreen(
    onSaveItem: (
        category: LiteratureCategory,
        authorName: String,
        genre: String,
        title: String,
        note: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by rememberSaveable { mutableStateOf(LiteratureCategory.BANGLA) }
    var authorName by rememberSaveable { mutableStateOf("") }
    var genre by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    var authorError by rememberSaveable { mutableStateOf<String?>(null) }
    var genreError by rememberSaveable { mutableStateOf<String?>(null) }
    var titleError by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 1.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "সাহিত্যের বিভাগ নির্বাচন করুন",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LiteratureCategory.entries.forEachIndexed { index, category ->
                            SegmentedButton(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = LiteratureCategory.entries.size
                                ),
                                modifier = Modifier.testTag("add_category_${category.value}")
                            ) {
                                Text(
                                    text = category.fullLabelBn,
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = authorName,
                        onValueChange = {
                            authorName = it
                            if (authorError != null) authorError = null
                        },
                        label = { Text("লেখকের নাম *") },
                        placeholder = {
                            Text(
                                if (selectedCategory == LiteratureCategory.BANGLA) {
                                    "যেমন: মুনীর চৌধুরী"
                                } else {
                                    "যেমন: William Shakespeare"
                                }
                            )
                        },
                        isError = authorError != null,
                        supportingText = authorError?.let { err -> { Text(err) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_author_input")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = genre,
                            onValueChange = {
                                genre = it
                                if (genreError != null) genreError = null
                            },
                            label = { Text("সাহিত্যের ধরন *") },
                            placeholder = {
                                Text(
                                    if (selectedCategory == LiteratureCategory.BANGLA) {
                                        "যেমন: নাটক, উপন্যাস, কবিতা"
                                    } else {
                                        "যেমন: Drama, Novel, Poetry"
                                    }
                                )
                            },
                            isError = genreError != null,
                            supportingText = genreError?.let { err -> { Text(err) } },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_genre_input")
                        )

                        val suggestions = if (selectedCategory == LiteratureCategory.BANGLA) {
                            banglaGenreSuggestions
                        } else {
                            englishGenreSuggestions
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            suggestions.forEach { suggestion ->
                                FilterChip(
                                    selected = genre.trim().equals(suggestion, ignoreCase = true),
                                    onClick = {
                                        genre = suggestion
                                        genreError = null
                                    },
                                    label = { Text(suggestion) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (titleError != null) titleError = null
                        },
                        label = { Text("সাহিত্যের নাম *") },
                        placeholder = {
                            Text(
                                if (selectedCategory == LiteratureCategory.BANGLA) {
                                    "যেমন: মানুষ"
                                } else {
                                    "যেমন: Hamlet"
                                }
                            )
                        },
                        isError = titleError != null,
                        supportingText = titleError?.let { err -> { Text(err) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_title_input")
                    )

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("নোট (ঐচ্ছিক)") },
                        placeholder = { Text("গুরুত্বপূর্ণ তথ্য, প্রকাশকাল বা পটভূমি লিখে রাখুন...") },
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_note_input")
                    )

                    Button(
                        onClick = {
                            val aErr = if (authorName.isBlank()) "লেখকের নাম লিখুন" else null
                            val gErr = if (genre.isBlank()) "সাহিত্যের ধরন লিখুন" else null
                            val tErr = if (title.isBlank()) "সাহিত্যের নাম লিখুন" else null
                            authorError = aErr
                            genreError = gErr
                            titleError = tErr

                            if (aErr == null && gErr == null && tErr == null) {
                                onSaveItem(
                                    selectedCategory,
                                    authorName,
                                    genre,
                                    title,
                                    note
                                )
                                authorName = ""
                                genre = ""
                                title = ""
                                note = ""
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("add_save_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BookmarkAdd,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সংরক্ষণ করুন",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
