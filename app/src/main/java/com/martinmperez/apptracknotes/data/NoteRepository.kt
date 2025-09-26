package com.martinmperez.apptracknotes.data

import com.martinmperez.apptracknotes.data.local.Note
import com.martinmperez.apptracknotes.data.local.NoteDao
import kotlinx.coroutines.flow.Flow

class NoteRepository(private val dao: NoteDao) {
    val notes: Flow<List<Note>> = dao.observeNotes()

    suspend fun add(title: String, content: String) {
        val now = System.currentTimeMillis()
        dao.insert(Note(title = title, content = content, createdAt = now, updatedAt = now))
    }

    suspend fun edit(id: Long, title: String, content: String) {
        val now = System.currentTimeMillis()
        dao.update(
            Note(id = id, title = title, content = content, updatedAt = now)
        )
    }

    suspend fun softDelete(id: Long) = dao.softDelete(id)
    suspend fun purgeDeleted() = dao.purgeDeleted()
}