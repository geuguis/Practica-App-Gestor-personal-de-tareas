package com.example.gestortareas

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.ajustes by preferencesDataStore("ajustes")
private val SOLO_PENDIENTES = booleanPreferencesKey("solo_pendientes")

// Devolver preferencias vacías si falla la lectura para que no se cierre la app
fun observarFiltro(context: Context): Flow<Boolean> {
    return context.ajustes.data
        .catch { err ->
            if (err is IOException) {
                emit(emptyPreferences())
            } else {
                throw err
            }
        }
        .map { pref: Preferences ->
            val valor: Boolean? = pref[SOLO_PENDIENTES]
            if (valor != null) {
                valor
            } else {
                false
            }
        }
}

suspend fun guardarFiltro(context: Context, valor: Boolean) {
    context.ajustes.edit { pref ->
        pref[SOLO_PENDIENTES] = valor
    }
}
