package com.example.gestortareas

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {
    @Query("SELECT * FROM tareas ORDER BY id DESC")
    fun observar(): Flow<List<Tarea>>

    @Insert
    suspend fun insertar(tarea: Tarea): Long

    @Update
    suspend fun actualizar(tarea: Tarea): Int

    @Delete
    suspend fun eliminar(tarea: Tarea): Int
}
