package com.example.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BackupManager(
    private val context: Context,
    private val preferences: AppPreferences
) {
    companion object {
        const val BACKUP_FOLDER_NAME = "BCSLiterature"
        const val BACKUP_FILE_NAME = "backup.json"
        const val EXPORT_SHARE_FILE_NAME = "BCS-Literature-Backup.json"
        private const val MIME_JSON = "application/json"
    }

    suspend fun writeAutoBackup(items: List<LiteratureItemEntity>): Boolean = withContext(Dispatchers.IO) {
        val json = LiteratureJsonCodec.encode(items)
        var succeeded = false

        // 1. Write to public Documents/BCSLiterature/backup.json via MediaStore (API 29+) or File (API 24-28)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (writeViaMediaStore(json)) {
                succeeded = true
            }
        } else {
            if (writeLegacyPublicDocuments(json)) {
                succeeded = true
            }
        }

        // 2. Also write to persisted SAF folder if user linked one
        val treeUriStr = preferences.getBackupTreeUri()
        if (!treeUriStr.isNullOrBlank()) {
            if (writeToSafTree(Uri.parse(treeUriStr), json)) {
                succeeded = true
            }
        }

        if (succeeded) {
            preferences.setLastBackupTimestamp(System.currentTimeMillis())
        }
        succeeded
    }

    suspend fun readAutoBackupIfExists(): JsonImportResult? = withContext(Dispatchers.IO) {
        // 1. Check persisted SAF tree URI first if configured
        val treeUriStr = preferences.getBackupTreeUri()
        if (!treeUriStr.isNullOrBlank()) {
            val safJson = readFromSafTree(Uri.parse(treeUriStr))
            if (!safJson.isNullOrBlank()) {
                val parsed = LiteratureJsonCodec.decode(safJson)
                if (parsed.isValidDocument && parsed.validItems.isNotEmpty()) {
                    return@withContext parsed
                }
            }
        }

        // 2. Check MediaStore on API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val msJson = readViaMediaStore()
            if (!msJson.isNullOrBlank()) {
                val parsed = LiteratureJsonCodec.decode(msJson)
                if (parsed.isValidDocument && parsed.validItems.isNotEmpty()) {
                    return@withContext parsed
                }
            }
        }

        // 3. Fallback: direct public Documents/BCSLiterature/backup.json (API 24-29 or granted storage)
        val legacyJson = readLegacyPublicDocuments()
        if (!legacyJson.isNullOrBlank()) {
            val parsed = LiteratureJsonCodec.decode(legacyJson)
            if (parsed.isValidDocument && parsed.validItems.isNotEmpty()) {
                return@withContext parsed
            }
        }

        null
    }

    suspend fun createShareExportIntent(items: List<LiteratureItemEntity>): Intent? = withContext(Dispatchers.IO) {
        try {
            val json = LiteratureJsonCodec.encode(items)
            val exportFile = File(context.cacheDir, EXPORT_SHARE_FILE_NAME)
            exportFile.writeText(json, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportFile
            )

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = MIME_JSON
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "BCS Literature Backup")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            Intent.createChooser(sendIntent, "ব্যাকআপ ফাইল শেয়ার বা সংরক্ষণ করুন")
        } catch (_: Exception) {
            null
        }
    }

    suspend fun readJsonFromUri(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun writeViaMediaStore(json: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val relativePath = "${Environment.DIRECTORY_DOCUMENTS}/$BACKUP_FOLDER_NAME"

        return try {
            val existingUri = findMediaStoreBackupUri()
            if (existingUri != null) {
                try {
                    resolver.openOutputStream(existingUri, "wt")?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    return true
                } catch (_: SecurityException) {
                    // Existing file owned by another install; insert a fresh entry below
                } catch (_: Exception) {
                    try {
                        resolver.delete(existingUri, null, null)
                    } catch (_: Exception) {
                    }
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, BACKUP_FILE_NAME)
                put(MediaStore.MediaColumns.MIME_TYPE, MIME_JSON)
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val newUri = resolver.insert(collection, values) ?: return false
            resolver.openOutputStream(newUri, "w")?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return false

            val doneValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            resolver.update(newUri, doneValues, null, null)
            true
        } catch (_: Exception) {
            // Fallback to legacy File if running on Android 10 with requestLegacyExternalStorage
            writeLegacyPublicDocuments(json)
        }
    }

    private fun readViaMediaStore(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val uri = findMediaStoreBackupUri() ?: return null
        return try {
            context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun findMediaStoreBackupUri(): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val resolver = context.contentResolver
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH
        )
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
        val selectionArgs = arrayOf(
            BACKUP_FILE_NAME,
            "${Environment.DIRECTORY_DOCUMENTS}/$BACKUP_FOLDER_NAME%"
        )

        return try {
            resolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    ContentUris.withAppendedId(collection, id)
                } else {
                    null
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun writeLegacyPublicDocuments(json: String): Boolean {
        return try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val appDir = File(docsDir, BACKUP_FOLDER_NAME)
            if (!appDir.exists() && !appDir.mkdirs()) {
                return false
            }
            val backupFile = File(appDir, BACKUP_FILE_NAME)
            backupFile.writeText(json, Charsets.UTF_8)
            true
        } catch (_: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun readLegacyPublicDocuments(): String? {
        return try {
            val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            val backupFile = File(File(docsDir, BACKUP_FOLDER_NAME), BACKUP_FILE_NAME)
            if (backupFile.exists() && backupFile.canRead()) {
                backupFile.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun writeToSafTree(treeUri: Uri, json: String): Boolean {
        return try {
            val resolver = context.contentResolver
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootDocId)
            val existingFileUri = findChildDocumentUri(treeUri, rootDocId, BACKUP_FILE_NAME)

            val targetUri = existingFileUri ?: DocumentsContract.createDocument(
                resolver,
                rootDocUri,
                MIME_JSON,
                BACKUP_FILE_NAME
            ) ?: return false

            resolver.openOutputStream(targetUri, "wt")?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return false
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun readFromSafTree(treeUri: Uri): String? {
        return try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            val targetUri = findChildDocumentUri(treeUri, rootDocId, BACKUP_FILE_NAME) ?: return null
            context.contentResolver.openInputStream(targetUri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun findChildDocumentUri(treeUri: Uri, parentDocId: String, displayName: String): Uri? {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        return try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    if (idIdx >= 0 && nameIdx >= 0) {
                        val name = cursor.getString(nameIdx)
                        if (name == displayName) {
                            val docId = cursor.getString(idIdx)
                            return DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                        }
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
