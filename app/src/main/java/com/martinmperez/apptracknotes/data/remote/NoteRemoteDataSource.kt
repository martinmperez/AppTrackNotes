package com.martinmperez.apptracknotes.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.martinmperez.apptracknotes.data.local.Note
import kotlinx.coroutines.tasks.await

/**
 * Fuente de datos remota para Notas en Firestore.
 * Estructura en Firestore:
 *   users/{uid}/notes/{remoteId}
 *
 * Mientras no uses Auth, queda uid = "demo". En la Parte 3 lo reemplazamos por request.auth.uid.
 */
class NoteRemoteDataSource(
    private val db: FirebaseFirestore,
    private val uidProvider: () -> String = { "demo" }
) {
    /** Referencia a la colección de notas del usuario actual */
    private fun notesCol() = db
        .collection("users")
        .document(uidProvider())
        .collection("notes")

    /**
     * Crea o actualiza (upsert) una nota en Firestore.
     * - Si la nota ya tiene remoteId, actualiza ese doc.
     * - Si no, crea un doc nuevo y devuelve su id.
     */
    suspend fun upsert(note: Note): String {
        val docRef = if (note.remoteId != null) {
            notesCol().document(note.remoteId)
        } else {
            notesCol().document() // genera id nuevo
        }

        val data = mapOf(
            "title" to note.title,
            "content" to note.content,
            "createdAt" to note.createdAt,
            "updatedAt" to note.updatedAt,
            "isDeleted" to note.isDeleted
        )

        // merge para no pisar accidentalmente otros campos si se agregaran a futuro
        docRef.set(data, SetOptions.merge()).await()
        return docRef.id
    }

    /**
     * Marca una nota como borrada en remoto (soft delete).
     * No elimina el documento, solo setea isDeleted = true y actualiza updatedAt.
     */
    suspend fun markDeleted(remoteId: String, updatedAt: Long) {
        notesCol()
            .document(remoteId)
            .set(
                mapOf("isDeleted" to true, "updatedAt" to updatedAt),
                SetOptions.merge()
            )
            .await()
    }

    /**
     * Trae todas las notas remotas del usuario actual.
     * Devuelve una lista de RemoteNote (modelo simple para sync).
     */
    suspend fun fetchAll(): List<RemoteNote> {
        val snap = notesCol().get().await()
        return snap.documents.mapNotNull { d ->
            val title = d.getString("title") ?: return@mapNotNull null
            val content = d.getString("content") ?: ""
            val createdAt = d.getLong("createdAt") ?: 0L
            val updatedAt = d.getLong("updatedAt") ?: 0L
            val isDeleted = d.getBoolean("isDeleted") ?: false

            RemoteNote(
                remoteId = d.id,
                title = title,
                content = content,
                createdAt = createdAt,
                updatedAt = updatedAt,
                isDeleted = isDeleted
            )
        }
    }
}

/** DTO remoto mínimo para sincronización */
data class RemoteNote(
    val remoteId: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean
)

/** Helpers opcionales para mapear entre modelos (si te resultan cómodos) */
fun RemoteNote.toLocal(): Note = Note(
    id = 0, // se completará al insertar en Room
    title = title,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isDeleted = isDeleted,
    isSynced = true,
    remoteId = remoteId
)

fun Note.toRemotePayload(): Map<String, Any?> = mapOf(
    "title" to title,
    "content" to content,
    "createdAt" to createdAt,
    "updatedAt" to updatedAt,
    "isDeleted" to isDeleted
)
