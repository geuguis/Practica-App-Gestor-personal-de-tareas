package com.example.gestortareas

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Tarea::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tareaDao(): TareaDao

    companion object {
        @Volatile
        private var instancia: AppDatabase? = null

        // Evitar abrir la base de datos varias veces a la vez
        fun obtener(context: Context): AppDatabase {
            val aux: AppDatabase? = instancia
            if (aux != null) {
                return aux
            }
            synchronized(this) {
                var nueva: AppDatabase? = instancia
                if (nueva == null) {
                    nueva = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "tareas.db"
                    ).build()
                    instancia = nueva
                }
                return nueva
            }
        }
    }
}
