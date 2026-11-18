package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bcs_literature_prefs")

class AppPreferences(private val context: Context) {

    private object Keys {
        val PERMISSION_PROMPT_SHOWN = booleanPreferencesKey("permission_prompt_shown")
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val BACKUP_TREE_URI = stringPreferencesKey("backup_tree_uri")
    }

    val lastBackupTimestampFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_BACKUP_TIMESTAMP] ?: 0L
    }

    val backupTreeUriFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.BACKUP_TREE_URI]
    }

    suspend fun isPermissionPromptShown(): Boolean {
        return context.dataStore.data.first()[Keys.PERMISSION_PROMPT_SHOWN] ?: false
    }

    suspend fun setPermissionPromptShown(shown: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PERMISSION_PROMPT_SHOWN] = shown
        }
    }

    suspend fun setLastBackupTimestamp(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_BACKUP_TIMESTAMP] = timestamp
        }
    }

    suspend fun getBackupTreeUri(): String? {
        return context.dataStore.data.first()[Keys.BACKUP_TREE_URI]
    }

    suspend fun setBackupTreeUri(uriString: String?) {
        context.dataStore.edit { prefs ->
            if (uriString == null) {
                prefs.remove(Keys.BACKUP_TREE_URI)
            } else {
                prefs[Keys.BACKUP_TREE_URI] = uriString
            }
        }
    }
}
