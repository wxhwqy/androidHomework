package com.example.test4.repository

import androidx.lifecycle.LiveData
import com.example.test4.data.Note
import com.example.test4.data.NoteDao

class NoteRepository(private val noteDao: NoteDao) {
    
    fun getAllNotes(): LiveData<List<Note>> = noteDao.getAllNotes()
    
    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)
    
    fun searchNotes(query: String): LiveData<List<Note>> = noteDao.searchNotes(query)
    
    fun getNotesByCategory(category: String): LiveData<List<Note>> = noteDao.getNotesByCategory(category)
    
    fun getAllCategories(): LiveData<List<String>> = noteDao.getAllCategories()
    
    fun getNotesSortedByTitle(): LiveData<List<Note>> = noteDao.getNotesSortedByTitle()
    
    fun getNotesSortedByCreatedDate(): LiveData<List<Note>> = noteDao.getNotesSortedByCreatedDate()
    
    suspend fun insertNote(note: Note): Long = noteDao.insertNote(note)
    
    suspend fun updateNote(note: Note) = noteDao.updateNote(note)
    
    suspend fun deleteNote(note: Note) = noteDao.deleteNote(note)
    
    suspend fun deleteAllNotes() = noteDao.deleteAllNotes()
} 