package com.example.gestortareas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.gestortareas.ui.theme.GestorTareasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Obtener el DAO una sola vez al arrancar la actividad
        val dao: TareaDao = AppDatabase.obtener(applicationContext).tareaDao()
        enableEdgeToEdge()
        setContent {
            GestorTareasTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppGestorTareas(dao, Modifier.padding(innerPadding))
                }
            }
        }
    }
}
