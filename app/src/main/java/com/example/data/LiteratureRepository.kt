package com.example.data

import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LiteratureRepository(
    private val dao: LiteratureDao,
    private val backupManager: BackupManager,
    private val preferences: AppPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var autoBackupJob: Job? = null

    val allItemsFlow: Flow<List<LiteratureItemEntity>> = dao.observeAll()
    val lastBackupTimestampFlow: Flow<Long> = preferences.lastBackupTimestampFlow
    val backupTreeUriFlow: Flow<String?> = preferences.backupTreeUriFlow

    suspend fun addOrUpdateItem(item: LiteratureItemEntity) {
        dao.upsert(item)
        scheduleAutoBackup()
    }

    suspend fun deleteItem(id: String) {
        dao.deleteById(id)
        scheduleAutoBackup()
    }

    suspend fun setMemorized(id: String, memorized: Boolean) {
        dao.setMemorized(
            id = id,
            memorized = memorized,
            updatedAt = System.currentTimeMillis()
        )
        scheduleAutoBackup()
    }

    suspend fun importMerge(items: List<LiteratureItemEntity>) {
        if (items.isNotEmpty()) {
            dao.upsertAll(items)
            scheduleAutoBackup()
        }
    }

    suspend fun importReplace(items: List<LiteratureItemEntity>) {
        dao.replaceAll(items)
        scheduleAutoBackup()
    }

    suspend fun tryAutoRestoreOnLaunch(): Boolean {
        val count = dao.getCount()
        if (count > 0) return false

        val backup = backupManager.readAutoBackupIfExists() ?: return false
        if (backup.validItems.isEmpty()) return false

        dao.upsertAll(backup.validItems)
        return true
    }

    suspend fun runBackupNow(): Boolean {
        val items = dao.getAllOnce()
        return backupManager.writeAutoBackup(items)
    }

    suspend fun createShareExportIntent(): Intent? {
        val items = dao.getAllOnce()
        return backupManager.createShareExportIntent(items)
    }

    suspend fun parseImportUri(uri: Uri): JsonImportResult? {
        val raw = backupManager.readJsonFromUri(uri) ?: return null
        return LiteratureJsonCodec.decode(raw)
    }

    suspend fun isPermissionPromptShown(): Boolean = preferences.isPermissionPromptShown()

    suspend fun markPermissionPromptShown() = preferences.setPermissionPromptShown(true)

    suspend fun saveBackupTreeUri(uriString: String?) {
        preferences.setBackupTreeUri(uriString)
        if (uriString != null) {
            runBackupNow()
        }
    }

    private fun scheduleAutoBackup() {
        autoBackupJob?.cancel()
        autoBackupJob = scope.launch {
            delay(800L)
            val items = dao.getAllOnce()
            backupManager.writeAutoBackup(items)
        }
    }
}
