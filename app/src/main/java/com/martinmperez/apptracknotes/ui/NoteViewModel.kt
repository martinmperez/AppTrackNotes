// app/src/main/java/com/martinmperez/apptracknotes/ui/NoteViewModel.kt
package com.martinmperez.apptracknotes.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.martinmperez.apptracknotes.data.NoteRepository
import com.martinmperez.apptracknotes.data.local.AppDatabase
import com.martinmperez.apptracknotes.data.remote.NoteRemoteDataSource
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class NoteViewModel(app: Application) : AndroidViewModel(app) {

    // -------------------- DATOS (Room + Firestore) --------------------
    private val dao = AppDatabase.getInstance(app).noteDao()

    // Auth (la usamos para obtener el uid real)
    private val auth = FirebaseAuth.getInstance()

    private var lastUid: String? = auth.currentUser?.uid

    private val remote = NoteRemoteDataSource(
        db = FirebaseFirestore.getInstance(),
        uidProvider = { auth.currentUser?.uid ?: error("No authenticated user") }
    )

    private val repo = NoteRepository(dao, remote)

    val notes = repo.notes.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun add(title: String, content: String) = viewModelScope.launch {
        if (auth.currentUser == null) {
            _authState.value = _authState.value.copy(error = "Iniciá sesión para crear notas")
            return@launch
        }
        repo.add(title, content)
    }

    fun delete(id: Long) = viewModelScope.launch {
        if (auth.currentUser == null) {
            _authState.value = _authState.value.copy(error = "Iniciá sesión para borrar notas")
            return@launch
        }
        repo.softDelete(id)
    }

    fun syncNow() = viewModelScope.launch {
        if (auth.currentUser == null) {
            // opcional: reflejar en UI que debe iniciar sesión
            _authState.value = _authState.value.copy(error = "Iniciá sesión para sincronizar")
            return@launch
        }
        repo.syncNow()
    }

    // -------------------- AUTH (Paso 4) --------------------
    data class AuthUiState(
        val isLoggedIn: Boolean = false,
        val email: String? = null,
        val error: String? = null,
        val loading: Boolean = false
    )

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState

    private val authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        val newUid = user?.uid

        // si cambió el usuario (o se cerró sesión), limpiamos la cache local
        if (newUid != lastUid) {
            lastUid = newUid
            viewModelScope.launch {
                dao.clearAll()        // 🔹 deja la lista vacía en la UI
            }
        }

        _authState.value = _authState.value.copy(
            isLoggedIn = user != null,
            email = user?.email,
            error = null,
            loading = false
        )
    }

    init {
        auth.addAuthStateListener(authListener)
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }

    fun signUp(email: String, password: String) = viewModelScope.launch {
        try {
            _authState.value = _authState.value.copy(loading = true, error = null)
            auth.createUserWithEmailAndPassword(email, password).await()
            // El listener actualizará _authState
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(
                loading = false,
                error = e.message ?: "Error al registrar"
            )
        }
    }

    fun signIn(email: String, password: String) = viewModelScope.launch {
        try {
            _authState.value = _authState.value.copy(loading = true, error = null)
            auth.signInWithEmailAndPassword(email, password).await()
        } catch (e: Exception) {
            _authState.value = _authState.value.copy(
                loading = false,
                error = e.message ?: "Error al iniciar sesión"
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            // opcional: limpiar antes de cerrar sesión
            dao.clearAll()
            auth.signOut()
        }
    }
}
