package com.martinmperez.apptracknotes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.martinmperez.apptracknotes.data.NoteRepository
import com.martinmperez.apptracknotes.data.local.AppDatabase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = NoteRepository(
        AppDatabase.getInstance(app).noteDao()
    )

    val notes = repo.notes.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun add(title: String, content: String) = viewModelScope.launch {
        repo.add(title, content)
    }
    fun delete(id: Long) = viewModelScope.launch { repo.softDelete(id) }
}