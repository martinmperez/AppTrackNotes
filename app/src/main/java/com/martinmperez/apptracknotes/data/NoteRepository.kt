package com.martinmperez.apptracknotes.data

import com.martinmperez.apptracknotes.data.local.Note
import com.martinmperez.apptracknotes.data.local.NoteDao
import com.martinmperez.apptracknotes.data.remote.NoteRemoteDataSource
import kotlinx.coroutines.flow.Flow

class NoteRepository(
    private val dao: NoteDao,
    private val remote: NoteRemoteDataSource
) {
    val notes: Flow<List<Note>> = dao.observeNotes()

    suspend fun add(title: String, content: String) {
        val now = System.currentTimeMillis()
        dao.insert(
            Note(title = title, content = content, createdAt = now, updatedAt = now, isSynced = false)
        )
    }

    suspend fun edit(id: Long, title: String, content: String) {
        val now = System.currentTimeMillis()
        val current = dao.getById(id) ?: return
        dao.update(current.copy(
            title = title,
            content = content,
            updatedAt = now,
            isSynced = false
        ))
    }

    suspend fun softDelete(id: Long) {
        val now = System.currentTimeMillis()
        dao.softDelete(id, now)
    }

    suspend fun purgeDeleted() = dao.purgeDeleted()

    // ---------- SINCRONIZACIÓN ----------
    suspend fun syncUp() {
        val pending = dao.getAllPendingSync()
        for (n in pending) {
            if (n.isDeleted) {
                n.remoteId?.let { remote.markDeleted(it, n.updatedAt) }
            } else {
                val newRemoteId = remote.upsert(n)
                dao.update(n.copy(remoteId = newRemoteId, isSynced = true))
            }
        }
    }

    suspend fun syncDown() {
        val remotes = remote.fetchAll()
        for (r in remotes) {
            val local = dao.getByRemoteId(r.remoteId)
            if (local == null) {
                if (!r.isDeleted) {
                    dao.insert(
                        Note(
                            title = r.title,
                            content = r.content,
                            createdAt = r.createdAt,
                            updatedAt = r.updatedAt,
                            isDeleted = r.isDeleted,
                            isSynced = true,
                            remoteId = r.remoteId
                        )
                    )
                }
            } else {
                when {
                    r.updatedAt > local.updatedAt -> {
                        dao.update(local.copy(
                            title = r.title,
                            content = r.content,
                            updatedAt = r.updatedAt,
                            isDeleted = r.isDeleted,
                            isSynced = true
                        ))
                    }
                    r.updatedAt <= local.updatedAt -> {
                        // si local es más nuevo, lo subirá en la próxima syncUp
                    }
                }
            }
        }
    }

    suspend fun syncNow() {
        syncUp()
        syncDown()
        // opcional: purgeDeleted() si querés limpiar definitivamente
    }
}
