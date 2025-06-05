package com.example.test4.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.test4.data.Note
import com.example.test4.data.NoteDatabase
import com.example.test4.repository.NoteRepository
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: NoteRepository
    
    init {
        val noteDao = NoteDatabase.getDatabase(application).noteDao()
        repository = NoteRepository(noteDao)
    }
    
    fun getAllNotes(): LiveData<List<Note>> = repository.getAllNotes()
    
    fun searchNotes(query: String): LiveData<List<Note>> = repository.searchNotes(query)
    
    fun getNotesByCategory(category: String): LiveData<List<Note>> = repository.getNotesByCategory(category)
    
    fun getAllCategories(): LiveData<List<String>> = repository.getAllCategories()
    
    fun getNotesSortedByTitle(): LiveData<List<Note>> = repository.getNotesSortedByTitle()
    fun getNotesSortedByCreatedDate(): LiveData<List<Note>> = repository.getNotesSortedByCreatedDate()
    
    fun insertNote(note: Note): LiveData<Long> {
        val result = MutableLiveData<Long>()
        viewModelScope.launch {
            val id = repository.insertNote(note)
            result.postValue(id)
        }
        return result
    }
    
    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }
    
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }
    
    fun getNoteById(id: Long): LiveData<Note?> {
        val result = MutableLiveData<Note?>()
        viewModelScope.launch {
            val note = repository.getNoteById(id)
            result.postValue(note)
        }
        return result
    }
    
    fun deleteAllNotes() {
        viewModelScope.launch {
            repository.deleteAllNotes()
        }
    }
} 