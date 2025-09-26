package com.martinmperez.apptracknotes.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun observeNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :now WHERE id = :noteId")
    suspend fun softDelete(noteId: Long, now: Long = System.currentTimeMillis())

    // Útil para limpieza eventual
    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun purgeDeleted()
}