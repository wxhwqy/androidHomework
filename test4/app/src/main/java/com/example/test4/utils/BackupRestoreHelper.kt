package com.example.test4.utils

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.test4.data.Note
import com.example.test4.data.NoteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class BackupRestoreHelper(private val context: Context) {
    
    private val gson = Gson()
    private val database = NoteDatabase.getDatabase(context)
    
    companion object {
        private const val TAG = "BackupRestoreHelper"
        private const val BACKUP_DIRECTORY = "NoteAppBackup"
        private const val BACKUP_FILE_PREFIX = "notes_backup_"
        private const val BACKUP_FILE_EXTENSION = ".json"
    }
    
    fun backupData(callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notes = getAllNotesFromDatabase()
                                
                if (notes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback(false)
                    }
                    return@launch
                }
                
                val backupDir = getBackupDirectory()
                if (!backupDir.exists()) {
                    val created = backupDir.mkdirs()
                    Log.d(TAG, "创建备份目录: $created")
                }
                
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val backupFile = File(backupDir, "$BACKUP_FILE_PREFIX$timestamp$BACKUP_FILE_EXTENSION")
                
                val json = gson.toJson(notes)
                FileWriter(backupFile).use { writer ->
                    writer.write(json)
                }
                cleanOldBackups()
                
                withContext(Dispatchers.Main) {
                    callback(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }
    
    fun restoreData(callback: (Boolean) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                
                val latestBackupFile = getLatestBackupFile()
                if (latestBackupFile == null) {
                    withContext(Dispatchers.Main) {
                        callback(false)
                    }
                    return@launch
                }
                                
                val json = FileReader(latestBackupFile).use { reader ->
                    reader.readText()
                }
                
                if (json.isBlank()) {
                    withContext(Dispatchers.Main) {
                        callback(false)
                    }
                    return@launch
                }
                
                val type = object : TypeToken<List<Note>>() {}.type
                val notes: List<Note> = gson.fromJson(json, type)
                                
                if (notes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        callback(false)
                    }
                    return@launch
                }
                
                database.noteDao().deleteAllNotes()
                Log.d(TAG, "已清除现有数据")
                
                var successCount = 0
                notes.forEach { note ->
                    try {
                        val newNote = note.copy(id = 0)
                        val newId = database.noteDao().insertNote(newNote)
                        if (newId > 0) {
                            successCount++
                        }
                    } catch (e: Exception) {
                    }
                }

                withContext(Dispatchers.Main) {
                    callback(successCount > 0)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(false)
                }
            }
        }
    }
    
    private suspend fun getAllNotesFromDatabase(): List<Note> {
        return try {
            val dao = database.noteDao()
            dao.getAllNotesForBackup()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun getBackupDirectory(): File {
        return try {
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), BACKUP_DIRECTORY)
            } else {
                File(context.filesDir, BACKUP_DIRECTORY)
            }
        } catch (e: Exception) {
            Log.w(TAG, "无法访问外部存储，使用内部存储", e)
            File(context.filesDir, BACKUP_DIRECTORY)
        }
    }

    private fun getLatestBackupFile(): File? {
        val backupDir = getBackupDirectory()
        if (!backupDir.exists()) {
            Log.d(TAG, "备份目录不存在: ${backupDir.absolutePath}")
            return null
        }
        
        val backupFiles = backupDir.listFiles { file ->
            file.name.startsWith(BACKUP_FILE_PREFIX) && file.name.endsWith(BACKUP_FILE_EXTENSION)
        }
        
        Log.d(TAG, "找到 ${backupFiles?.size ?: 0} 个备份文件")
        
        return backupFiles?.maxByOrNull { it.lastModified() }
    }

    fun getAllBackupFiles(): List<File> {
        val backupDir = getBackupDirectory()
        if (!backupDir.exists()) {
            return emptyList()
        }
        
        return backupDir.listFiles { file ->
            file.name.startsWith(BACKUP_FILE_PREFIX) && file.name.endsWith(BACKUP_FILE_EXTENSION)
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
    
    fun cleanOldBackups(keepCount: Int = 5) {
        try {
            val backupFiles = getAllBackupFiles()
            if (backupFiles.size > keepCount) {
                val filesToDelete = backupFiles.drop(keepCount)
                filesToDelete.forEach { file ->
                    if (file.delete()) {
                        Log.d(TAG, "删除旧备份文件: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "清理旧备份文件失败", e)
        }
    }
} 