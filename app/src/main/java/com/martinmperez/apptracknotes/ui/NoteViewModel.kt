package com.martinmperez.apptracknotes.ui

import com.google.firebase.firestore.FirebaseFirestore
import com.martinmperez.apptracknotes.data.NoteRepository
import com.martinmperez.apptracknotes.data.local.AppDatabase
import com.martinmperez.apptracknotes.data.remote.NoteRemoteDataSource
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import android.app.Application
import androidx.lifecycle.AndroidViewModel

class NoteViewModel(app: Application) : AndroidViewModel(app) {
    // Local (Room)
    private val dao = AppDatabase.getInstance(app).noteDao()

    // Remoto (Firestore)  ← ESTAS SON LAS LÍNEAS A LAS QUE TE REFERÍAS
    private val remote = NoteRemoteDataSource(FirebaseFirestore.getInstance())

    // Repositorio que une local + remoto
    private val repo = NoteRepository(dao, remote)

    // Estado para la UI
    val notes = repo.notes.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Acciones expuestas a la UI
    fun add(title: String, content: String) = viewModelScope.launch {
        repo.add(title, content)
    }

    fun delete(id: Long) = viewModelScope.launch {
        repo.softDelete(id)
    }

    fun syncNow() = viewModelScope.launch {
        repo.syncNow()
    }
}