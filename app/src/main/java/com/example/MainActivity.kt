package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Quiz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.AppPreferences
import com.example.data.BackupManager
import com.example.data.LiteratureRepository
import com.example.ui.AppTab
import com.example.ui.CategoryFilter
import com.example.ui.MainViewModel
import com.example.ui.MemorizedFilter
import com.example.ui.StudyScope
import com.example.ui.UiMessageEvent
import com.example.ui.components.ImportModeDialog
import com.example.ui.components.PermissionExplanationDialog
import com.example.ui.components.SplashOverlay
import com.example.ui.screens.AddItemScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ListScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.RandomStudyScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BcsLiteratureTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = AppDatabase.getInstance(applicationContext)
        val prefs = AppPreferences(applicationContext)
        val backupManager = BackupManager(applicationContext, prefs)
        val repository = LiteratureRepository(db.literatureDao(), backupManager, prefs)
        val factory = MainViewModel.Factory(repository)

        setContent {
            BcsLiteratureTheme {
                val viewModel: MainViewModel = viewModel(factory = factory)
                BcsLiteratureApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BcsLiteratureApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val isSplashVisible by viewModel.isSplashVisible.collectAsStateWithLifecycle()
    val showPermissionDialog by viewModel.showPermissionDialog.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val isSettingsOpen by viewModel.isSettingsOpen.collectAsStateWithLifecycle()
    val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    val filteredItems by viewModel.filteredItems.collectAsStateWithLifecycle()
    val listFilterState by viewModel.listFilterState.collectAsStateWithLifecycle()
    val quizState by viewModel.quizState.collectAsStateWithLifecycle()
    val studyState by viewModel.studyState.collectAsStateWithLifecycle()
    val lastBackupTimestamp by viewModel.lastBackupTimestamp.collectAsStateWithLifecycle()
    val backupTreeUri by viewModel.backupTreeUri.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()

    // Runtime storage permissions for API <= 32
    val legacyStoragePermissions = remember {
        when {
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 -> arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            else -> emptyArray()
        }
    }

    val needsLegacyPermissionRequest = remember(legacyStoragePermissions) {
        legacyStoragePermissions.any { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results.values.any { it }
        if (granted) {
            viewModel.runManualBackup()
        }
        viewModel.dismissPermissionDialog()
    }

    val importPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.handleSelectedImportUri(uri)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri ->
        if (treeUri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
                viewModel.saveLinkedBackupFolder(treeUri.toString())
            } catch (_: Exception) {
                viewModel.saveLinkedBackupFolder(treeUri.toString())
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiMessageEvent.Toast -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_LONG).show()
                }
                is UiMessageEvent.Snackbar -> {
                    snackbarHostState.showSnackbar(event.text)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    val isSubScreenOpen = isSettingsOpen || studyState.isOpen
                    TopAppBar(
                        title = {
                            Text(
                                text = when {
                                    isSettingsOpen -> "সেটিংস"
                                    studyState.isOpen -> "র‍্যান্ডম পড়া"
                                    else -> when (currentTab) {
                                        AppTab.HOME -> "BCS Literature"
                                        AppTab.ADD -> "নতুন সাহিত্য যোগ করুন"
                                        AppTab.LIST -> "সংরক্ষিত তালিকা"
                                        AppTab.QUIZ -> "সাহিত্য পরীক্ষা"
                                    }
                                },
                                fontWeight = FontWeight.Bold
                            )
                        },
                        navigationIcon = {
                            if (isSubScreenOpen) {
                                IconButton(
                                    onClick = {
                                        if (isSettingsOpen) {
                                            viewModel.closeSettings()
                                        } else {
                                            viewModel.closeRandomStudy()
                                        }
                                    },
                                    modifier = Modifier.testTag("topbar_back_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                        contentDescription = "পিছনে যান"
                                    )
                                }
                            }
                        },
                        actions = {
                            if (!isSettingsOpen) {
                                IconButton(
                                    onClick = { viewModel.openSettings() },
                                    modifier = Modifier.testTag("topbar_settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = "সেটিংস ও ব্যাকআপ"
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                },
                bottomBar = {
                    if (!useNavigationRail && !isSettingsOpen && !studyState.isOpen) {
                        NavigationBar {
                            AppTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.labelBn
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.labelBn,
                                            maxLines = 1
                                        )
                                    },
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavigationRail && !isSettingsOpen && !studyState.isOpen) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            AppTab.entries.forEach { tab ->
                                val selected = currentTab == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.labelBn
                                        )
                                    },
                                    label = { Text(tab.labelBn) },
                                    modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                        if (isSettingsOpen) {
                            SettingsScreen(
                                appVersion = BuildConfig.VERSION_NAME,
                                totalItems = allItems.size,
                                lastBackupTimestamp = lastBackupTimestamp,
                                isFolderLinked = !backupTreeUri.isNullOrBlank(),
                                onBackupNow = { viewModel.runManualBackup() },
                                onLinkBackupFolder = { folderPickerLauncher.launch(null) },
                                onExportJson = {
                                    viewModel.exportJsonForShare { shareIntent ->
                                        context.startActivity(shareIntent)
                                    }
                                },
                                onImportJson = {
                                    importPickerLauncher.launch(
                                        arrayOf("application/json", "text/plain", "*/*")
                                    )
                                },
                                onBack = { viewModel.closeSettings() }
                            )
                        } else if (studyState.isOpen) {
                            RandomStudyScreen(
                                state = studyState,
                                onSelectScope = viewModel::setStudyScope,
                                onToggleUnmemorizedOnly = viewModel::setStudyUnmemorizedOnly,
                                onToggleHideDetails = viewModel::toggleStudyHideDetails,
                                onRevealDetails = viewModel::revealStudyDetails,
                                onToggleMemorized = viewModel::toggleCurrentStudyMemorized,
                                onNextRandom = viewModel::nextRandomStudyItem,
                                onPrevious = viewModel::previousStudyItem,
                                onBack = viewModel::closeRandomStudy
                            )
                        } else {
                            when (currentTab) {
                                AppTab.HOME -> DashboardScreen(
                                    stats = stats,
                                    onStartQuizClick = { viewModel.selectTab(AppTab.QUIZ) },
                                    onAddNewClick = { viewModel.selectTab(AppTab.ADD) },
                                    onRandomStudyClick = { viewModel.openRandomStudy() }
                                )
                                AppTab.ADD -> AddItemScreen(
                                    onSaveItem = { category, author, genre, title, note ->
                                        viewModel.addItem(category, author, genre, title, note)
                                    }
                                )
                                AppTab.LIST -> ListScreen(
                                    items = filteredItems,
                                    totalCount = allItems.size,
                                    filterState = listFilterState,
                                    onSearchChange = viewModel::updateSearchQuery,
                                    onCategoryFilterChange = viewModel::updateCategoryFilter,
                                    onMemorizedFilterChange = viewModel::updateMemorizedFilter,
                                    onToggleMemorized = viewModel::toggleMemorized,
                                    onUpdateItem = viewModel::updateItem,
                                    onDeleteItem = viewModel::deleteItem,
                                    onRandomStudyClick = {
                                        val scope = when (listFilterState.categoryFilter) {
                                            CategoryFilter.ALL -> StudyScope.ALL
                                            CategoryFilter.BANGLA -> StudyScope.BANGLA
                                            CategoryFilter.ENGLISH -> StudyScope.ENGLISH
                                        }
                                        val unmemOnly = listFilterState.memorizedFilter == MemorizedFilter.UNMEMORIZED
                                        viewModel.openRandomStudy(
                                            initialScope = scope,
                                            unmemorizedOnly = unmemOnly
                                        )
                                    }
                                )
                                AppTab.QUIZ -> QuizScreen(
                                    state = quizState,
                                    onSelectScope = viewModel::setQuizScope,
                                    onSelectPool = viewModel::setQuizPool,
                                    onStartQuiz = viewModel::startQuiz,
                                    onRevealOptions = viewModel::revealQuizOptions,
                                    onSubmitAnswer = viewModel::submitQuizAnswer,
                                    onRevealAnswer = viewModel::revealAnswerWithoutScoring,
                                    onMarkMemorized = viewModel::markQuizItemMemorized,
                                    onNextQuestion = viewModel::nextQuizQuestion,
                                    onFinishEarly = viewModel::finishQuizEarly,
                                    onResetToSetup = viewModel::resetQuizToSetup,
                                    onGoHome = { viewModel.selectTab(AppTab.HOME) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showPermissionDialog && !isSplashVisible) {
            PermissionExplanationDialog(
                needsLegacyRuntimePermission = needsLegacyPermissionRequest,
                onConfirm = {
                    if (needsLegacyPermissionRequest && legacyStoragePermissions.isNotEmpty()) {
                        storagePermissionLauncher.launch(legacyStoragePermissions)
                    } else {
                        viewModel.dismissPermissionDialog()
                    }
                },
                onDismiss = {
                    viewModel.dismissPermissionDialog()
                }
            )
        }

        pendingImport?.let { pending ->
            ImportModeDialog(
                pendingImport = pending,
                onMerge = { viewModel.confirmImportMerge() },
                onReplace = { viewModel.confirmImportReplace() },
                onDismiss = { viewModel.dismissImportDialog() }
            )
        }

        SplashOverlay(visible = isSplashVisible)
    }
}

private fun tabIcon(tab: AppTab, selected: Boolean) = when (tab) {
    AppTab.HOME -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
    AppTab.ADD -> if (selected) Icons.Rounded.AddCircle else Icons.Outlined.AddCircleOutline
    AppTab.LIST -> if (selected) Icons.Rounded.FormatListBulleted else Icons.Outlined.FormatListBulleted
    AppTab.QUIZ -> if (selected) Icons.Rounded.Quiz else Icons.Outlined.Quiz
}
