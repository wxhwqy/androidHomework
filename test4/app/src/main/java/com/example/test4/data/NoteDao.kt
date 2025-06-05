package com.example.test4.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NoteDao {
    
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun getAllNotes(): LiveData<List<Note>>
    
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAllNotesForBackup(): List<Note>
    
    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?
    
    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchNotes(query: String): LiveData<List<Note>>
    
    @Query("SELECT * FROM notes WHERE category = :category ORDER BY updatedAt DESC")
    fun getNotesByCategory(category: String): LiveData<List<Note>>
    
    @Query("SELECT DISTINCT category FROM notes ORDER BY category")
    fun getAllCategories(): LiveData<List<String>>
    
    @Query("SELECT * FROM notes ORDER BY title ASC")
    fun getNotesSortedByTitle(): LiveData<List<Note>>
    
    @Query("SELECT * FROM notes ORDER BY createdAt DESC")
    fun getNotesSortedByCreatedDate(): LiveData<List<Note>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long
    
    @Update
    suspend fun updateNote(note: Note)
    
    @Delete
    suspend fun deleteNote(note: Note)
    
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
} 