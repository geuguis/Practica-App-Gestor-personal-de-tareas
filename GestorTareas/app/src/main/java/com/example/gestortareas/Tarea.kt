package com.example.gestortareas

import androidx.room.Entity
import androidx.room.PrimaryKey

// Tabla para guardar las tareas en la base local
@Entity(tableName = "tareas")
data class Tarea(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val titulo: String,
    val descripcion: String = "",
    val prioridad: String = "Media",
    val completada: Boolean = false
)
