package com.example.gestortareas

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

// Comprobar que no esté vacío y tenga mínimo 3 letras después de quitar espacios
fun errorTitulo(texto: String): String? {
    val limpio: String = texto.trim()
    if (limpio.isEmpty()) {
        return "El título no puede estar vacío"
    }
    if (limpio.length < 3) {
        return "Escribe al menos 3 caracteres"
    }
    return null
}

// Preparar el texto para mandarlo con el selector del móvil
fun compartirTarea(context: Context, tarea: Tarea): String? {
    val descTexto: String
    if (tarea.descripcion.trim().isEmpty()) {
        descTexto = "Sin descripción"
    } else {
        descTexto = tarea.descripcion.trim()
    }

    val mensaje: String = "Tarea: " + tarea.titulo +
        "\nDescripción: " + descTexto +
        "\nPrioridad: " + tarea.prioridad

    val envio = Intent(Intent.ACTION_SEND)
    envio.type = "text/plain"
    envio.putExtra(Intent.EXTRA_TEXT, mensaje)

    return try {
        context.startActivity(Intent.createChooser(envio, "Compartir tarea"))
        null
    } catch (e: ActivityNotFoundException) {
        "No hay ninguna aplicación compatible para compartir"
    }
}

@Composable
fun AppGestorTareas(dao: TareaDao, modifier: Modifier = Modifier) {
    val context: Context = LocalContext.current
    val flujoTareas = remember(dao) { dao.observar() }
    val tareas by flujoTareas.collectAsStateWithLifecycle(emptyList())

    val flujoFiltro = remember(context) { observarFiltro(context) }
    val soloPendientes by flujoFiltro.collectAsStateWithLifecycle(false)

    // Guardar la pantalla y los campos con rememberSaveable para no perder datos al girar
    var pantalla by rememberSaveable { mutableStateOf("lista") }
    var idEditar by rememberSaveable { mutableStateOf(0L) }
    var tituloForm by rememberSaveable { mutableStateOf("") }
    var descForm by rememberSaveable { mutableStateOf("") }
    var prioForm by rememberSaveable { mutableStateOf("Media") }
    var completadaForm by rememberSaveable { mutableStateOf(false) }
    var intentoForm by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = pantalla != "lista") {
        pantalla = "lista"
    }

    if (pantalla == "formulario") {
        PantallaFormulario(
            dao = dao,
            idEditar = idEditar,
            titulo = tituloForm,
            descripcion = descForm,
            prioridad = prioForm,
            completada = completadaForm,
            intento = intentoForm,
            alCambiarTitulo = { nuevo -> tituloForm = nuevo },
            alCambiarDesc = { nueva -> descForm = nueva },
            alCambiarPrio = { nueva -> prioForm = nueva },
            alCambiarIntento = { valor -> intentoForm = valor },
            alVolver = { pantalla = "lista" },
            modifier = modifier
        )
    } else if (pantalla == "remotas") {
        PantallaTareasRemotas(
            alVolver = { pantalla = "lista" },
            modifier = modifier
        )
    } else {
        PantallaLista(
            dao = dao,
            tareas = tareas,
            soloPendientes = soloPendientes,
            alNuevaTarea = {
                idEditar = 0L
                tituloForm = ""
                descForm = ""
                prioForm = "Media"
                completadaForm = false
                intentoForm = false
                pantalla = "formulario"
            },
            alEditarTarea = { t ->
                idEditar = t.id
                tituloForm = t.titulo
                descForm = t.descripcion
                prioForm = t.prioridad
                completadaForm = t.completada
                intentoForm = false
                pantalla = "formulario"
            },
            alAbrirRemotas = { pantalla = "remotas" },
            modifier = modifier
        )
    }
}

@Composable
fun PantallaLista(
    dao: TareaDao,
    tareas: List<Tarea>,
    soloPendientes: Boolean,
    alNuevaTarea: () -> Unit,
    alEditarTarea: (Tarea) -> Unit,
    alAbrirRemotas: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tareaABorrar by remember { mutableStateOf<Tarea?>(null) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var idOcupado by remember { mutableStateOf(0L) }

    // Bucle normal para quedarse solo con las pendientes cuando el switch está activo
    val listaMostrar: MutableList<Tarea> = mutableListOf()
    for (t in tareas) {
        if (!soloPendientes || !t.completada) {
            listaMostrar.add(t)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.icono_tarea),
                contentDescription = "Icono gestor de tareas",
                modifier = Modifier.size(36.dp),
                contentScale = ContentScale.Fit
            )
            Text(
                text = "Gestor personal de tareas",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = alNuevaTarea) {
                Text("Nueva tarea")
            }
            OutlinedButton(onClick = alAbrirRemotas) {
                Text("Tareas de ejemplo")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Mostrar solo pendientes")
            Switch(
                checked = soloPendientes,
                onCheckedChange = { marcado ->
                    scope.launch {
                        try {
                            guardarFiltro(context, marcado)
                            aviso = null
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (e: Exception) {
                            aviso = "No se pudo guardar la preferencia"
                        }
                    }
                }
            )
        }

        if (aviso != null) {
            Text(
                text = aviso!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        if (listaMostrar.isEmpty()) {
            val textoVacio: String
            if (soloPendientes && tareas.isNotEmpty()) {
                textoVacio = "No tienes tareas pendientes ahora mismo."
            } else {
                textoVacio = "Todavía no hay tareas guardadas. Pulsa Nueva tarea para añadir una."
            }
            Text(textoVacio)
        } else {
            // Usar weight(1f) para que la lista ocupe el hueco libre sin tapar lo de arriba
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(listaMostrar, key = { t -> t.id }) { tarea ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = tarea.completada,
                                    enabled = idOcupado != tarea.id,
                                    onCheckedChange = { nuevoEstado ->
                                        idOcupado = tarea.id
                                        scope.launch {
                                            try {
                                                val cambiada = tarea.copy(completada = nuevoEstado)
                                                dao.actualizar(cambiada)
                                                aviso = null
                                            } catch (cancel: CancellationException) {
                                                throw cancel
                                            } catch (e: SQLiteException) {
                                                aviso = "Error al actualizar la tarea"
                                            } finally {
                                                idOcupado = 0L
                                            }
                                        }
                                    }
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = tarea.titulo,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    val estadoTxt: String = if (tarea.completada) {
                                        "Completada"
                                    } else {
                                        "Pendiente"
                                    }
                                    Text("Prioridad: " + tarea.prioridad + " · " + estadoTxt)
                                }
                            }

                            if (tarea.descripcion.trim().isNotEmpty()) {
                                Text(tarea.descripcion)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { alEditarTarea(tarea) }) {
                                    Text("Editar")
                                }
                                TextButton(onClick = {
                                    val res: String? = compartirTarea(context, tarea)
                                    aviso = res
                                }) {
                                    Text("Compartir")
                                }
                                TextButton(onClick = { tareaABorrar = tarea }) {
                                    Text("Eliminar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val objetivo: Tarea? = tareaABorrar
    if (objetivo != null) {
        AlertDialog(
            onDismissRequest = { tareaABorrar = null },
            title = { Text("Eliminar tarea") },
            text = { Text("¿Seguro que quieres borrar \"" + objetivo.titulo + "\"?") },
            confirmButton = {
                TextButton(onClick = {
                    val copia: Tarea = objetivo
                    tareaABorrar = null
                    scope.launch {
                        try {
                            dao.eliminar(copia)
                            aviso = null
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (e: SQLiteException) {
                            aviso = "No se pudo eliminar la tarea"
                        }
                    }
                }) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { tareaABorrar = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun SelectorPrioridad(prioridad: String, alElegir: (String) -> Unit) {
    var expandido by remember { mutableStateOf(false) }
    val opciones: List<String> = listOf("Baja", "Media", "Alta")

    Box {
        OutlinedButton(onClick = { expandido = true }) {
            Text("Prioridad: $prioridad")
        }
        DropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            for (op in opciones) {
                DropdownMenuItem(
                    text = { Text(op) },
                    onClick = {
                        alElegir(op)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
fun PantallaFormulario(
    dao: TareaDao,
    idEditar: Long,
    titulo: String,
    descripcion: String,
    prioridad: String,
    completada: Boolean,
    intento: Boolean,
    alCambiarTitulo: (String) -> Unit,
    alCambiarDesc: (String) -> Unit,
    alCambiarPrio: (String) -> Unit,
    alCambiarIntento: (Boolean) -> Unit,
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var guardando by remember { mutableStateOf(false) }
    var errorBd by remember { mutableStateOf<String?>(null) }

    val falloTitulo: String? = errorTitulo(titulo)
    val opcionesPrio: List<String> = listOf("Baja", "Media", "Alta")
    val prioValida: Boolean = prioridad in opcionesPrio

    val textoCabecera: String
    if (idEditar == 0L) {
        textoCabecera = "Nueva tarea"
    } else {
        textoCabecera = "Editar tarea"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = textoCabecera,
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = titulo,
            onValueChange = { nuevo -> alCambiarTitulo(nuevo) },
            label = { Text("Título") },
            singleLine = true,
            enabled = !guardando,
            isError = intento && falloTitulo != null,
            supportingText = {
                if (intento && falloTitulo != null) {
                    Text(falloTitulo)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = descripcion,
            onValueChange = { nueva -> alCambiarDesc(nueva) },
            label = { Text("Descripción (opcional)") },
            enabled = !guardando,
            modifier = Modifier.fillMaxWidth()
        )

        SelectorPrioridad(prioridad) { op ->
            alCambiarPrio(op)
        }

        if (intento && !prioValida) {
            Text(
                text = "Elige una prioridad válida (Baja, Media o Alta)",
                color = MaterialTheme.colorScheme.error
            )
        }

        if (errorBd != null) {
            Text(
                text = errorBd!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Evitar que se guarde dos veces si el usuario pulsa rápido
            Button(
                enabled = !guardando,
                onClick = {
                    if (guardando) {
                        return@Button
                    }
                    alCambiarIntento(true)
                    val tituloLimpio: String = titulo.trim()
                    val descLimpia: String = descripcion.trim()
                    if (errorTitulo(tituloLimpio) == null && prioValida) {
                        guardando = true
                        errorBd = null
                        scope.launch {
                            try {
                                if (idEditar == 0L) {
                                    val nueva = Tarea(
                                        0L,
                                        tituloLimpio,
                                        descLimpia,
                                        prioridad,
                                        false
                                    )
                                    dao.insertar(nueva)
                                } else {
                                    val modificada = Tarea(
                                        idEditar,
                                        tituloLimpio,
                                        descLimpia,
                                        prioridad,
                                        completada
                                    )
                                    dao.actualizar(modificada)
                                }
                                alVolver()
                            } catch (cancel: CancellationException) {
                                throw cancel
                            } catch (e: SQLiteException) {
                                errorBd = "No se pudo guardar la tarea"
                            } finally {
                                guardando = false
                            }
                        }
                    }
                }
            ) {
                Text(if (guardando) "Guardando..." else "Guardar")
            }

            OutlinedButton(
                enabled = !guardando,
                onClick = alVolver
            ) {
                Text("Cancelar")
            }
        }
    }
}

@Composable
fun PantallaTareasRemotas(
    alVolver: () -> Unit,
    modifier: Modifier = Modifier
) {
    var estado by remember { mutableStateOf<EstadoCarga>(EstadoCarga.Inicial) }
    val scope = rememberCoroutineScope()

    fun pedirAlServidor() {
        if (estado is EstadoCarga.Cargando) {
            return
        }
        estado = EstadoCarga.Cargando
        scope.launch {
            estado = cargarTareas(ServicioTareas.api)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tareas de ejemplo (API REST)",
            style = MaterialTheme.typography.headlineSmall
        )
        Text("Estas tareas vienen de JSONPlaceholder y no se guardan en tu lista personal.")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = estado !is EstadoCarga.Cargando,
                onClick = { pedirAlServidor() }
            ) {
                Text("Solicitar tareas")
            }
            OutlinedButton(onClick = alVolver) {
                Text("Volver")
            }
        }

        val actual: EstadoCarga = estado
        if (actual is EstadoCarga.Inicial) {
            Text("Pulsa el botón para pedir las tareas al servidor.")
        } else if (actual is EstadoCarga.Cargando) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator()
                Text("Cargando tareas del servidor...")
            }
        } else if (actual is EstadoCarga.Error) {
            Text(
                text = actual.mensaje,
                color = MaterialTheme.colorScheme.error
            )
            Button(onClick = { pedirAlServidor() }) {
                Text("Reintentar")
            }
        } else if (actual is EstadoCarga.Datos) {
            if (actual.tareas.isEmpty()) {
                Text("El servidor ha devuelto una lista vacía.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(actual.tareas, key = { elem -> elem.id }) { item ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                val estadoTexto: String = if (item.completed) {
                                    "Completada"
                                } else {
                                    "Pendiente"
                                }
                                Text("ID: " + item.id + " · " + estadoTexto)
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
