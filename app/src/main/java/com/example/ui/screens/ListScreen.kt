package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LibraryBooks
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.LiteratureCategory
import com.example.data.LiteratureItemEntity
import com.example.ui.BengaliUtils
import com.example.ui.CategoryFilter
import com.example.ui.ListFilterState
import com.example.ui.MemorizedFilter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(
    items: List<LiteratureItemEntity>,
    totalCount: Int,
    filterState: ListFilterState,
    onSearchChange: (String) -> Unit,
    onCategoryFilterChange: (CategoryFilter) -> Unit,
    onMemorizedFilterChange: (MemorizedFilter) -> Unit,
    onToggleMemorized: (LiteratureItemEntity) -> Unit,
    onUpdateItem: (
        original: LiteratureItemEntity,
        category: LiteratureCategory,
        authorName: String,
        genre: String,
        title: String,
        note: String
    ) -> Unit,
    onDeleteItem: (String) -> Unit,
    onRandomStudyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var itemToDelete by remember { mutableStateOf<LiteratureItemEntity?>(null) }
    var itemToEdit by remember { mutableStateOf<LiteratureItemEntity?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxSize()
        ) {
            // Search & Filter Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = { Text("সাহিত্যের নাম, লেখক বা ধরন খুঁজুন...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "খুঁজুন"
                        )
                    },
                    trailingIcon = {
                        if (filterState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchChange("") },
                                modifier = Modifier.testTag("list_clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "সার্চ মুছুন"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("list_search_input")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CategoryFilter.entries.forEach { cat ->
                        FilterChip(
                            selected = filterState.categoryFilter == cat,
                            onClick = { onCategoryFilterChange(cat) },
                            label = { Text(cat.labelBn) },
                            modifier = Modifier.testTag("filter_cat_${cat.name.lowercase()}")
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    MemorizedFilter.entries.filter { it != MemorizedFilter.ALL }.forEach { mem ->
                        val isSelected = filterState.memorizedFilter == mem
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onMemorizedFilterChange(
                                    if (isSelected) MemorizedFilter.ALL else mem
                                )
                            },
                            label = { Text(mem.labelBn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.testTag("filter_mem_${mem.name.lowercase()}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "দেখানো হচ্ছে ${BengaliUtils.toBengaliDigits(items.size)}টি (মোট ${BengaliUtils.toBengaliDigits(totalCount)}টি)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (totalCount > 0) {
                        TextButton(
                            onClick = onRandomStudyClick,
                            modifier = Modifier.testTag("list_random_study_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("র‍্যান্ডম পড়ুন")
                        }
                    }
                }
            }

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LibraryBooks,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (totalCount == 0) {
                                "এখনো কোনো তথ্য সংরক্ষিত নেই"
                            } else {
                                "খোঁজার সাথে মেলে এমন কিছু পাওয়া যায়নি"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (totalCount == 0) {
                                "'যোগ করুন' ট্যাব থেকে আপনার প্রথম সাহিত্যের তথ্য যোগ করুন।"
                            } else {
                                "অন্য শব্দ বা ফিল্টার পরিবর্তন করে চেষ্টা করুন।"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = items, key = { it.id }) { item ->
                        LiteratureItemCard(
                            item = item,
                            onToggleMemorized = { onToggleMemorized(item) },
                            onEdit = { itemToEdit = item },
                            onDelete = { itemToDelete = item }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    itemToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("তথ্যটি মুছে ফেলবেন?") },
            text = {
                Text(
                    "'${target.title}' (${target.authorName}) তথ্যটি তালিকা থেকে স্থায়ীভাবে মুছে যাবে।",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteItem(target.id)
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("confirm_delete_button")
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { itemToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_button")
                ) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Edit Bottom Sheet
    itemToEdit?.let { editingItem ->
        BackHandler { itemToEdit = null }
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { itemToEdit = null },
            sheetState = sheetState
        ) {
            EditItemSheetContent(
                item = editingItem,
                onDismiss = { itemToEdit = null },
                onSave = { category, author, genre, title, note ->
                    onUpdateItem(editingItem, category, author, genre, title, note)
                    itemToEdit = null
                }
            )
        }
    }
}

@Composable
private fun LiteratureItemCard(
    item: LiteratureItemEntity,
    onToggleMemorized: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("literature_item_card_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = item.authorName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = item.categoryEnum.shortLabelBn,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = item.genre,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (item.note.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = item.memorized,
                    onClick = onToggleMemorized,
                    leadingIcon = {
                        Icon(
                            imageVector = if (item.memorized) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.RadioButtonUnchecked
                            },
                            contentDescription = if (item.memorized) "মুখস্থ হয়েছে" else "মুখস্থ চিহ্নিত করুন",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    label = {
                        Text(if (item.memorized) "মুখস্থ হয়েছে ✓" else "মুখস্থ করুন")
                    },
                    modifier = Modifier.testTag("toggle_memorized_${item.id}")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "সম্পাদনা করুন",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = "মুছে ফেলুন",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditItemSheetContent(
    item: LiteratureItemEntity,
    onDismiss: () -> Unit,
    onSave: (
        category: LiteratureCategory,
        authorName: String,
        genre: String,
        title: String,
        note: String
    ) -> Unit
) {
    var category by remember(item.id) { mutableStateOf(item.categoryEnum) }
    var authorName by remember(item.id) { mutableStateOf(item.authorName) }
    var genre by remember(item.id) { mutableStateOf(item.genre) }
    var title by remember(item.id) { mutableStateOf(item.title) }
    var note by remember(item.id) { mutableStateOf(item.note) }

    var authorError by remember { mutableStateOf<String?>(null) }
    var genreError by remember { mutableStateOf<String?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "তথ্য সম্পাদনা করুন",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            LiteratureCategory.entries.forEachIndexed { index, cat ->
                SegmentedButton(
                    selected = category == cat,
                    onClick = { category = cat },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = LiteratureCategory.entries.size
                    )
                ) {
                    Text(cat.fullLabelBn)
                }
            }
        }

        OutlinedTextField(
            value = authorName,
            onValueChange = {
                authorName = it
                authorError = null
            },
            label = { Text("লেখকের নাম *") },
            isError = authorError != null,
            supportingText = authorError?.let { err -> { Text(err) } },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_author_input")
        )

        OutlinedTextField(
            value = genre,
            onValueChange = {
                genre = it
                genreError = null
            },
            label = { Text("সাহিত্যের ধরন *") },
            isError = genreError != null,
            supportingText = genreError?.let { err -> { Text(err) } },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_genre_input")
        )

        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                titleError = null
            },
            label = { Text("সাহিত্যের নাম *") },
            isError = titleError != null,
            supportingText = titleError?.let { err -> { Text(err) } },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_title_input")
        )

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("নোট") },
            minLines = 3,
            maxLines = 5,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("edit_note_input")
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
            ) {
                Text("বাতিল")
            }
            Button(
                onClick = {
                    val aErr = if (authorName.isBlank()) "লেখকের নাম লিখুন" else null
                    val gErr = if (genre.isBlank()) "সাহিত্যের ধরন লিখুন" else null
                    val tErr = if (title.isBlank()) "সাহিত্যের নাম লিখুন" else null
                    authorError = aErr
                    genreError = gErr
                    titleError = tErr
                    if (aErr == null && gErr == null && tErr == null) {
                        onSave(category, authorName, genre, title, note)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("edit_save_button")
            ) {
                Text("হালনাগাদ করুন")
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
