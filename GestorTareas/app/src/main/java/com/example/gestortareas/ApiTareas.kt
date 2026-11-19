package com.example.gestortareas

import com.google.gson.JsonParseException
import java.io.IOException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class TareaRemota(
    val userId: Int,
    val id: Int,
    val title: String,
    val completed: Boolean
)

interface ApiTareas {
    @GET("todos")
    suspend fun listar(@Query("userId") usuario: Int): List<TareaRemota>
}

object ServicioTareas {
    val api: ApiTareas = Retrofit.Builder()
        .baseUrl("https://jsonplaceholder.typicode.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiTareas::class.java)
}

sealed interface EstadoCarga {
    data object Inicial : EstadoCarga
    data object Cargando : EstadoCarga
    data class Datos(val tareas: List<TareaRemota>) : EstadoCarga
    data class Error(val mensaje: String) : EstadoCarga
}

suspend fun cargarTareas(api: ApiTareas): EstadoCarga {
    return try {
        val lista: List<TareaRemota> = api.listar(1)
        EstadoCarga.Datos(lista)
    } catch (cancel: CancellationException) {
        // Dejar pasar la cancelación si se sale o gira la pantalla
        throw cancel
    } catch (e: HttpException) {
        EstadoCarga.Error("Respuesta del servicio: " + e.code())
    } catch (e: JsonParseException) {
        EstadoCarga.Error("Los datos recibidos no son compatibles")
    } catch (e: IOException) {
        EstadoCarga.Error("No se pudo conectar. Inténtalo de nuevo")
    }
}
