package com.jffloresriera.maizellm.ui

import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jffloresriera.maizellm.R
import com.jffloresriera.maizellm.data.Tema
import com.jffloresriera.maizellm.domain.Cuotas
import com.jffloresriera.maizellm.domain.Lang
import com.jffloresriera.maizellm.domain.Level
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    usuario: UsuarioSesion,
    cuentaViewModel: CuentaViewModel,
    viewModel: ChatViewModel = viewModel(),
) {
    val estado by viewModel.uiState.collectAsState()
    val estadoCuenta by cuentaViewModel.uiState.collectAsState()
    val cuenta = estadoCuenta.cuenta
    val puedeEscribir = cuenta != null && Cuotas.puedeGenerar(cuenta)
    val contexto = LocalContext.current

    // Sonido corto cuando la respuesta queda lista (no es una notificación del
    // sistema, es un "beep" generado en el momento, no necesita archivos de audio).
    LaunchedEffect(Unit) {
        viewModel.eventosSonido.collect {
            val tono = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
            tono.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        }
    }

    var menuAbierto by remember { mutableStateOf(false) }
    var dialogoAcercaDeAbierto by remember { mutableStateOf(false) }
    var dialogoConfigAbierto by remember { mutableStateOf(false) }
    var dialogoPlanesAbierto by remember { mutableStateOf(false) }
    var dialogoCerrarSesionAbierto by remember { mutableStateOf(false) }

    if (dialogoPlanesAbierto) {
        DialogoPlanes(
            cuenta = cuenta,
            usuario = usuario,
            mensajePlan = estadoCuenta.mensajePlan,
            onSolicitar = cuentaViewModel::registrarSolicitud,
            onRefrescar = cuentaViewModel::refrescarPlan,
            onCerrar = { dialogoPlanesAbierto = false; cuentaViewModel.descartarMensajePlan() },
        )
    }
    if (dialogoCerrarSesionAbierto) {
        AlertDialog(
            onDismissRequest = { dialogoCerrarSesionAbierto = false },
            title = { Text("Cerrar sesión") },
            text = { Text("Para volver a entrar necesitarás Internet para iniciar sesión con GitHub.") },
            confirmButton = {
                TextButton(onClick = {
                    dialogoCerrarSesionAbierto = false
                    viewModel.borrarConversacion()
                    cuentaViewModel.cerrarSesion()
                }) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { dialogoCerrarSesionAbierto = false }) { Text("Cancelar") }
            },
        )
    }

    if (dialogoAcercaDeAbierto) {
        DialogoAcercaDe(onCerrar = { dialogoAcercaDeAbierto = false })
    }
    if (dialogoConfigAbierto) {
        DialogoConfiguracion(
            mostrarMetricas = estado.mostrarMetricas,
            sonidoActivado = estado.sonidoActivado,
            tema = estado.tema,
            onMostrarMetricasChange = viewModel::cambiarMostrarMetricas,
            onSonidoChange = viewModel::cambiarSonido,
            onTemaChange = viewModel::cambiarTema,
            onCerrar = { dialogoConfigAbierto = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agro IA", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.padding(start = 12.dp).size(32.dp),
                        tint = Color.Unspecified,
                    )
                },
                actions = {
                    IconButton(onClick = { menuAbierto = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones")
                    }
                    DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                        DropdownMenuItem(
                            text = { Text("Mi plan") },
                            leadingIcon = { Icon(Icons.Filled.WorkspacePremium, contentDescription = null) },
                            onClick = { menuAbierto = false; dialogoPlanesAbierto = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Acerca de") },
                            leadingIcon = { Icon(Icons.Filled.Info, contentDescription = null) },
                            onClick = { menuAbierto = false; dialogoAcercaDeAbierto = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Configuración") },
                            leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                            onClick = { menuAbierto = false; dialogoConfigAbierto = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Compartir conversación") },
                            leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                            onClick = {
                                menuAbierto = false
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, viewModel.construirTranscripcion())
                                }
                                contexto.startActivity(Intent.createChooser(intent, "Compartir conversación"))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Borrar conversación") },
                            leadingIcon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
                            onClick = { menuAbierto = false; viewModel.borrarConversacion() },
                        )
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
                            onClick = { menuAbierto = false; dialogoCerrarSesionAbierto = true },
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(), // empuja el contenido hacia arriba cuando el teclado está abierto
        ) {

            BarraPlan(cuenta = cuenta, onClick = { dialogoPlanesAbierto = true })

            SelectorIdiomaYNivel(
                lang = estado.lang,
                level = estado.level,
                onLangChange = viewModel::cambiarIdioma,
                onLevelChange = viewModel::cambiarNivel,
            )

            when (val estadoModelo = estado.estadoModelo) {
                is EstadoModelo.Instalando ->
                    EstadoCargaCentral("Preparando el modelo por primera vez…")
                is EstadoModelo.InstalandoConProgreso ->
                    EstadoCargaCentral("Copiando el modelo… ${estadoModelo.porcentaje}%")
                is EstadoModelo.Cargando ->
                    EstadoCargaCentral("Cargando el modelo en memoria…")
                is EstadoModelo.Error ->
                    EstadoCargaCentral("No se pudo preparar el modelo: ${estadoModelo.mensaje}")
                is EstadoModelo.Listo ->
                    ConversacionLista(
                        mensajes = estado.mensajes,
                        generandoRespuesta = estado.generandoRespuesta,
                        textoEnProgreso = estado.textoEnProgreso,
                        mostrarMetricas = estado.mostrarMetricas,
                        lang = estado.lang,
                        onEnviar = viewModel::enviarPregunta,
                        bloqueo = if (puedeEscribir) null else {
                            { AvisoSinTokens(cuenta = cuenta, onVerPlanes = { dialogoPlanesAbierto = true }) }
                        },
                        modifier = Modifier.weight(1f),
                    )
            }
        }
    }
}

@Composable
private fun DialogoAcercaDe(onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Acerca de Agro IA") },
        text = {
            Column {
                Text(
                    "Chatbot de sanidad del cultivo de maíz, con un modelo de lenguaje " +
                        "especializado que corre 100% en el celular, sin conexión a internet.",
                )
                Text(
                    "Modelo: Llama 3.2 1B + DAPT + SFT (Reto 11).",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Desarrollador: Jesús Enrique Flores Riera",
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "Estudiante de Geomática",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
    )
}

@Composable
private fun DialogoConfiguracion(
    mostrarMetricas: Boolean,
    sonidoActivado: Boolean,
    tema: Tema,
    onMostrarMetricasChange: (Boolean) -> Unit,
    onSonidoChange: (Boolean) -> Unit,
    onTemaChange: (Tema) -> Unit,
    onCerrar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Configuración") },
        text = {
            Column {
                FilaInterruptor(
                    etiqueta = "Mostrar métricas de generación",
                    activado = mostrarMetricas,
                    onCambio = onMostrarMetricasChange,
                )
                FilaInterruptor(
                    etiqueta = "Sonido al terminar la respuesta",
                    activado = sonidoActivado,
                    onCambio = onSonidoChange,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                Text("Tema", style = MaterialTheme.typography.bodyMedium)
                FilaTema("Igual que el sistema", Tema.SISTEMA, tema, onTemaChange)
                FilaTema("Claro", Tema.CLARO, tema, onTemaChange)
                FilaTema("Oscuro", Tema.OSCURO, tema, onTemaChange)
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
    )
}

@Composable
private fun FilaInterruptor(etiqueta: String, activado: Boolean, onCambio: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(etiqueta, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = activado, onCheckedChange = onCambio)
    }
}

@Composable
private fun FilaTema(etiqueta: String, valor: Tema, seleccionado: Tema, onSeleccionar: (Tema) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = valor == seleccionado, onClick = { onSeleccionar(valor) })
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun EstadoCargaCentral(texto: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(texto, modifier = Modifier.padding(top = 16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorIdiomaYNivel(
    lang: Lang,
    level: Level,
    onLangChange: (Lang) -> Unit,
    onLevelChange: (Level) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SelectorDesplegable(
            etiquetaActual = if (lang == Lang.ES) "Español" else "English",
            opciones = Lang.entries.map { it to (if (it == Lang.ES) "Español" else "English") },
            onSeleccionar = onLangChange,
            modifier = Modifier.weight(1f),
        )
        SelectorDesplegable(
            etiquetaActual = etiquetaNivel(level),
            opciones = Level.entries.map { it to etiquetaNivel(it) },
            onSeleccionar = onLevelChange,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Selector simple con un botón que abre un DropdownMenu (API de nivel superior, estable,
 * sin la ambigüedad de scope de ExposedDropdownMenuBox). Genérico en T para reusarlo con
 * Lang y Level.
 */
@Composable
private fun <T> SelectorDesplegable(
    etiquetaActual: String,
    opciones: List<Pair<T, String>>,
    onSeleccionar: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expandido by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expandido = true },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(etiquetaActual, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { (valor, etiqueta) ->
                DropdownMenuItem(
                    text = { Text(etiqueta) },
                    onClick = { onSeleccionar(valor); expandido = false },
                )
            }
        }
    }
}

private fun etiquetaNivel(level: Level): String = when (level) {
    Level.AGRICULTOR -> "Agricultor"
    Level.TECNICO -> "Técnico"
    Level.CIENTIFICO -> "Científico"
}

@Composable
private fun ConversacionLista(
    mensajes: List<Mensaje>,
    generandoRespuesta: Boolean,
    textoEnProgreso: String?,
    mostrarMetricas: Boolean,
    lang: Lang,
    onEnviar: (String) -> Unit,
    modifier: Modifier = Modifier,
    // Si no es null, reemplaza la caja de texto (sin tokens o acceso bloqueado).
    bloqueo: (@Composable () -> Unit)? = null,
) {
    val listState = rememberLazyListState()
    // Se desplaza solo hasta abajo cada vez que hay un mensaje nuevo o mientras
    // se va revelando la respuesta — así siempre se ve lo último, incluso con
    // el teclado abierto tapando parte de la pantalla.
    LaunchedEffect(mensajes.size, textoEnProgreso) {
        val total = mensajes.size + if (generandoRespuesta) 1 else 0
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(mensajes) { mensaje -> BurbujaMensaje(mensaje, mostrarMetricas) }
            if (generandoRespuesta) {
                item {
                    if (textoEnProgreso.isNullOrEmpty()) {
                        BurbujaEscribiendo(lang)
                    } else {
                        BurbujaTexto(textoEnProgreso)
                    }
                }
            }
        }

        if (bloqueo != null && !generandoRespuesta) {
            bloqueo()
            return@Column
        }

        var texto by rememberSaveable { mutableStateOf("") }
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextField(
                    value = texto,
                    onValueChange = { texto = it },
                    placeholder = { Text("Escribe tu pregunta sobre el cultivo…") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.weight(1f),
                )
                FilledIconButton(onClick = {
                    if (texto.isNotBlank()) {
                        onEnviar(texto)
                        texto = ""
                    }
                }) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
                }
            }
        }
    }
}

@Composable
private fun BurbujaMensaje(mensaje: Mensaje, mostrarMetricas: Boolean) {
    val esUsuario = mensaje.autor == Autor.USUARIO
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (esUsuario) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (esUsuario) 18.dp else 4.dp,
                bottomEnd = if (esUsuario) 4.dp else 18.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (esUsuario) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
            ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Text(
                    mensaje.texto,
                    color = if (esUsuario) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                )
                if (!esUsuario && mensaje.referencias.isNotEmpty()) {
                    ListaReferencias(mensaje.referencias)
                }
                if (!esUsuario && mensaje.cifraSinVerificar) {
                    AvisoCifraSinVerificar()
                }
                if (!esUsuario && mostrarMetricas && mensaje.tokensGenerados != null) {
                    Text(
                        "${mensaje.tokensGenerados} tokens · " +
                            "%.1f tok/s · %.1f s".format(
                                mensaje.tokensPorSegundo ?: 0f,
                                (mensaje.tiempoGeneracionMs ?: 0L) / 1000f,
                            ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

/** Muestra cada cita que el modelo incluyó como una fila separada (puede ser 1 o más). */
@Composable
private fun ListaReferencias(referencias: List<String>) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        referencias.forEach { referencia ->
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    Icons.Filled.Link,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp).padding(top = 2.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    referencia,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun AvisoCifraSinVerificar() {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Filled.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "Cifra sin verificar. Consulta siempre la etiqueta registrada (ICA/EPA).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

/** Burbuja del asistente mientras se "revela" el texto (ver nota en ChatViewModel). */
@Composable
private fun BurbujaTexto(texto: String) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Text(
                texto,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

// Datos sobre maíz/agronomía/ciencia, para que la espera (puede tardar bastante
// en CPU de celular) sea más informativa que un simple "Escribiendo…". Tono
// formal, acorde con el resto de la app. Son hechos reales (varios ya
// verificados en el documento de contexto del proyecto, no inventados para
// "rellenar"). Salen en orden aleatorio (sin repetir dos veces seguidas) para
// que no se note un patrón fijo al usar la app varias veces.
private val MENSAJES_ESPERA_ES = listOf(
    // Historia y origen
    "El maíz es originario de Mesoamérica, domesticado a partir de una gramínea silvestre llamada teocintle.",
    "El maíz llegó a Europa en el siglo XVI, tras la llegada de los españoles a América.",
    "Antes de la llegada de los europeos, el maíz ya era la base de la alimentación en gran parte de América.",
    "El nombre científico del maíz es Zea mays.",
    "El maíz pertenece a la familia de las gramíneas, igual que el trigo, el arroz y la caña de azúcar.",
    // Biología y genética
    "El maíz es una planta de polinización cruzada: el viento transporta el polen de una planta a otra.",
    "Cada mazorca de maíz puede tener cientos de granos, cada uno resultado de una fecundación independiente.",
    "El maíz posee uno de los genomas más extensos entre los cereales cultivados.",
    "Existen miles de variedades de maíz adaptadas a distintos climas y altitudes.",
    "El maíz puede cultivarse desde el nivel del mar hasta más de 3.000 metros de altitud.",
    "El color de los granos de maíz depende de pigmentos como antocianinas y carotenoides.",
    "El maíz morado debe su color a un alto contenido de antocianinas.",
    // Agronomía y cultivo
    "El maíz necesita temperaturas superiores a 10 °C para germinar adecuadamente.",
    "La densidad de siembra del maíz varía según la variedad y el objetivo de producción.",
    "El maíz es sensible al estrés hídrico, especialmente durante la floración.",
    "La rotación de cultivos ayuda a reducir la acumulación de plagas y enfermedades en el suelo.",
    "El manejo integrado de plagas combina métodos biológicos, culturales y químicos.",
    "La aplicación oportuna de fertilizantes nitrogenados es clave para el rendimiento del maíz.",
    "El maíz responde bien a suelos francos, profundos y bien drenados.",
    "La siembra directa ayuda a conservar la humedad y reducir la erosión del suelo.",
    "El monitoreo temprano de plagas y enfermedades reduce las pérdidas en el cultivo.",
    "La rotación con leguminosas puede mejorar la disponibilidad de nitrógeno en el suelo.",
    // Enfermedades y sanidad
    "La mancha de asfalto del maíz es causada por el hongo Phyllachora maydis.",
    "El tizón del norte del maíz es causado por el hongo Exserohilum turcicum.",
    "La roya común del maíz es causada por el hongo Puccinia sorghi.",
    "La alta humedad y las temperaturas moderadas favorecen muchas enfermedades foliares del maíz.",
    "La identificación temprana de enfermedades foliares permite tomar decisiones de manejo más efectivas.",
    // Producción y uso
    "El maíz, el trigo y el arroz son los tres cereales más cultivados del mundo.",
    "El maíz se usa para consumo humano, alimento animal y procesos industriales.",
    "El almidón de maíz se usa en la industria alimentaria, farmacéutica y de papel.",
    "El maíz es materia prima para producir etanol como biocombustible.",
    "Colombia cultiva maíz en climas muy distintos, desde zonas cálidas hasta de clima frío.",
    "El maíz forma parte de la dieta tradicional en gran parte de América Latina.",
    // Tecnología y agricultura de precisión
    "La agricultura de precisión usa sensores, drones e imágenes satelitales para monitorear los cultivos.",
    "Los modelos de inteligencia artificial pueden apoyar la identificación de enfermedades a partir de imágenes de hojas.",
    "El uso de datos climáticos históricos ayuda a planear mejor las fechas de siembra.",
    "El riego por goteo permite un uso más eficiente del agua en cultivos exigentes.",
    // Sobre este proyecto
    "Este modelo se entrenó con un corpus de más de 41.000 documentos en español e inglés.",
    "El modelo que usa esta app tiene 1.000 millones de parámetros.",
    "Reducir el tamaño del modelo permite que corra directamente en un teléfono celular.",
    "El modelo fue ajustado para responder en tres niveles: agricultor, técnico y científico.",
    "Toda la inferencia de este chatbot ocurre en el propio dispositivo, sin conexión a internet.",
    "La cuantización reduce el tamaño del modelo conservando la mayor parte de su capacidad.",
    // Seguridad alimentaria y contexto
    "La sanidad vegetal es clave para reducir pérdidas de cosecha y aportar a la seguridad alimentaria.",
    "El acceso oportuno a información agronómica confiable puede mejorar la toma de decisiones en campo.",
    "Las etiquetas de los productos fitosanitarios indican las dosis autorizadas y las medidas de seguridad.",
    "En Colombia, el ICA regula el registro y uso de productos para la protección de cultivos.",
    "En Estados Unidos, la EPA regula el registro de plaguicidas y productos fitosanitarios.",
    "Verificar siempre la etiqueta oficial de un producto es la forma más segura de conocer su dosis correcta.",
    "La ciencia agrícola combina biología, química, suelos y clima para mejorar la producción de alimentos.",
    // Científicos de la agronomía
    "Norman Borlaug, padre de la Revolución Verde, recibió el Premio Nobel de la Paz en 1970.",
    "Norman Borlaug desarrolló variedades de trigo de alto rendimiento que ayudaron a evitar hambrunas.",
    "George Washington Carver promovió la rotación de cultivos para recuperar suelos agotados por el algodón.",
    "Gregor Mendel, padre de la genética, basó sus leyes de la herencia en experimentos con plantas de arveja.",
    "Barbara McClintock recibió el Premio Nobel de Medicina en 1983 por sus estudios sobre la genética del maíz.",
    "Barbara McClintock descubrió los \"genes saltarines\" (transposones) estudiando mazorcas de maíz.",
    "Luther Burbank desarrolló cientos de variedades nuevas de plantas mediante selección y cruzamiento.",
    "El Premio Mundial de la Alimentación reconoce a quienes han contribuido a mejorar la disponibilidad de alimentos.",
    // Instituciones y lugares de investigación
    "El CIMMYT (Centro Internacional de Mejoramiento de Maíz y Trigo) tiene su sede en México.",
    "El CIMMYT mantiene uno de los bancos de semillas de maíz y trigo más grandes del mundo.",
    "El CGIAR es una alianza mundial de centros de investigación agrícola dedicados a la seguridad alimentaria.",
    "AGROSAVIA es la corporación colombiana encargada de la investigación agropecuaria del país.",
    "La Universidad Nacional de Colombia tiene facultades dedicadas a la investigación agronómica.",
    "CGSpace es un repositorio digital que reúne investigaciones agrícolas de los centros del CGIAR.",
    "El IICA apoya la cooperación y el desarrollo agrícola en los países de América.",
    "La FAO, de Naciones Unidas, promueve la seguridad alimentaria a nivel mundial.",
    "El ICA regula la sanidad agropecuaria y el registro de insumos agrícolas en Colombia.",
    "La EPA regula el uso de plaguicidas y productos fitosanitarios en Estados Unidos.",
    "El IRRI (Instituto Internacional de Investigación del Arroz), en Filipinas, mejora genéticamente el arroz.",
    "La Revolución Verde del siglo XX aumentó de forma notable la producción mundial de alimentos.",
    // Ciencia agronómica en general
    "Muchos programas de mejoramiento de maíz cruzan líneas puras para producir híbridos más productivos.",
    "El \"vigor híbrido\" explica por qué los híbridos de maíz suelen rendir más que sus progenitores.",
    "La agroecología estudia los cultivos integrando suelo, clima y biodiversidad.",
    "La edafología es la ciencia que estudia el suelo y su fertilidad.",
    "La fitopatología es la rama de la ciencia que estudia las enfermedades de las plantas.",
    "La entomología agrícola estudia los insectos que afectan o benefician a los cultivos.",
    "Los bancos de germoplasma conservan semillas de miles de variedades para proteger la diversidad genética.",
    "El mejoramiento participativo involucra a los propios agricultores en la selección de nuevas variedades.",
    "La biotecnología agrícola va desde el cultivo de tejidos hasta la edición genética de plantas.",
    "Las estaciones experimentales agropecuarias permiten probar variedades en condiciones reales de campo.",
    "La ciencia de datos se usa hoy para analizar información agronómica y climática a gran escala.",
    "Los programas de extensión rural buscan llevar el conocimiento científico a las comunidades agrícolas.",
    "La investigación agrícola internacional suele compartir variedades mejoradas entre distintos países.",
    "La fisiología vegetal estudia cómo responden las plantas al estrés hídrico y térmico.",
    "La agronomía moderna combina biología, química, física y estadística.",
    "Los ensayos de campo comparan el desempeño de distintas variedades bajo las mismas condiciones.",
    "La investigación en nutrición vegetal busca optimizar el uso de fertilizantes sin dañar el ambiente.",
    "El estudio del cambio climático es cada vez más importante para planear la agricultura del futuro.",
    "Los científicos agrícolas trabajan junto a agrónomos de campo para validar sus hallazgos en la práctica.",
    "La conservación de polinizadores, como las abejas, es un tema creciente en la investigación agrícola.",
    "La agricultura urbana también es objeto de estudio científico como estrategia de seguridad alimentaria.",
    "Numerosas revistas científicas publican investigaciones sobre agronomía y fitopatología.",
    "La colaboración entre científicos de distintos países ha sido clave para enfrentar plagas globales del maíz.",
    "Las universidades con facultades de agronomía forman a los profesionales que investigan los cultivos.",
    "La selección genómica usa marcadores de ADN para acelerar el mejoramiento de cultivos.",
    "Los modelos de predicción de enfermedades ayudan a anticipar brotes antes de que se extiendan.",
    "La investigación en poscosecha busca reducir las pérdidas de grano después de la cosecha.",
    "La certificación de semillas garantiza la calidad genética y sanitaria de lo que se siembra.",
    "La cooperación científica internacional ha permitido compartir bancos de germoplasma entre países.",
    "La divulgación científica ayuda a que los avances agronómicos lleguen a quienes trabajan la tierra.",
)

// Mismos 100 datos, en inglés (misma intención del usuario: debe verse en el
// idioma activo del chat, no solo en español).
private val MENSAJES_ESPERA_EN = listOf(
    // History and origin
    "Maize is native to Mesoamerica, domesticated from a wild grass called teosinte.",
    "Maize arrived in Europe in the 16th century, after the Spanish arrived in America.",
    "Before Europeans arrived, maize was already the basis of the diet across much of the Americas.",
    "The scientific name of maize is Zea mays.",
    "Maize belongs to the grass family, along with wheat, rice, and sugarcane.",
    // Biology and genetics
    "Maize is a cross-pollinating plant: wind carries pollen from one plant to another.",
    "Each maize ear can hold hundreds of kernels, each the result of an independent fertilization.",
    "Maize has one of the largest genomes among cultivated cereals.",
    "There are thousands of maize varieties adapted to different climates and altitudes.",
    "Maize can be grown from sea level up to more than 3,000 meters of altitude.",
    "The color of maize kernels comes from pigments such as anthocyanins and carotenoids.",
    "Purple maize owes its color to a high content of anthocyanins.",
    // Agronomy and cultivation
    "Maize needs temperatures above 10 °C (50 °F) to germinate properly.",
    "Planting density for maize varies depending on the variety and the production goal.",
    "Maize is sensitive to water stress, especially during flowering.",
    "Crop rotation helps reduce the buildup of pests and diseases in the soil.",
    "Integrated pest management combines biological, cultural, and chemical methods.",
    "Timely nitrogen fertilization is key to maize yield.",
    "Maize grows well in deep, well-drained loamy soils.",
    "No-till planting helps conserve soil moisture and reduce erosion.",
    "Early monitoring of pests and diseases reduces crop losses.",
    "Rotating with legumes can improve nitrogen availability in the soil.",
    // Diseases and plant health
    "Tar spot of maize is caused by the fungus Phyllachora maydis.",
    "Northern corn leaf blight is caused by the fungus Exserohilum turcicum.",
    "Common rust of maize is caused by the fungus Puccinia sorghi.",
    "High humidity and moderate temperatures favor many maize foliar diseases.",
    "Early identification of foliar diseases allows for more effective management decisions.",
    // Production and use
    "Maize, wheat, and rice are the three most widely grown cereals in the world.",
    "Maize is used for human consumption, animal feed, and industrial processes.",
    "Corn starch is used in the food, pharmaceutical, and paper industries.",
    "Maize is a raw material for producing ethanol as a biofuel.",
    "Colombia grows maize across very different climates, from warm lowlands to cold highlands.",
    "Maize is part of the traditional diet across much of Latin America.",
    // Technology and precision agriculture
    "Precision agriculture uses sensors, drones, and satellite imagery to monitor crops.",
    "AI models can support the identification of diseases from images of leaves.",
    "Historical climate data helps plan better planting dates.",
    "Drip irrigation allows more efficient water use in demanding crops.",
    // About this project
    "This model was trained on a corpus of more than 41,000 documents in Spanish and English.",
    "The model used in this app has 1 billion parameters.",
    "Shrinking the model lets it run directly on a mobile phone.",
    "The model was fine-tuned to answer at three levels: farmer, technician, and researcher.",
    "All inference in this chatbot happens on the device itself, with no internet connection.",
    "Quantization reduces the size of the model while keeping most of its capability.",
    // Food security and context
    "Plant health is key to reducing crop losses and supporting food security.",
    "Timely access to reliable agronomic information can improve decisions in the field.",
    "Pesticide labels state the authorized doses and safety measures.",
    "In Colombia, ICA regulates the registration and use of crop protection products.",
    "In the United States, the EPA regulates the registration of pesticides.",
    "Always checking the official label is the safest way to know the correct dose.",
    "Agricultural science combines biology, chemistry, soil, and climate to improve food production.",
    // Agronomy scientists
    "Norman Borlaug, father of the Green Revolution, received the Nobel Peace Prize in 1970.",
    "Norman Borlaug developed high-yield wheat varieties that helped prevent famines.",
    "George Washington Carver promoted crop rotation to restore soils depleted by cotton.",
    "Gregor Mendel, the father of genetics, based his laws of heredity on experiments with pea plants.",
    "Barbara McClintock received the Nobel Prize in Medicine in 1983 for her studies on maize genetics.",
    "Barbara McClintock discovered \"jumping genes\" (transposons) by studying maize ears.",
    "Luther Burbank developed hundreds of new plant varieties through selection and crossbreeding.",
    "The World Food Prize recognizes those who have helped improve the availability of food.",
    // Institutions and research sites
    "CIMMYT (the International Maize and Wheat Improvement Center) is headquartered in Mexico.",
    "CIMMYT maintains one of the largest maize and wheat seed banks in the world.",
    "CGIAR is a global alliance of agricultural research centers dedicated to food security.",
    "AGROSAVIA is the Colombian corporation in charge of agricultural research in the country.",
    "The National University of Colombia has faculties dedicated to agronomic research.",
    "CGSpace is a digital repository that gathers agricultural research from CGIAR centers.",
    "IICA supports cooperation and agricultural development across the Americas.",
    "The FAO, part of the United Nations, promotes food security worldwide.",
    "ICA regulates agricultural health and the registration of farm inputs in Colombia.",
    "The EPA regulates the use of pesticides and crop protection products in the United States.",
    "IRRI (the International Rice Research Institute), in the Philippines, works on rice breeding.",
    "The Green Revolution of the 20th century significantly increased world food production.",
    // Agronomic science in general
    "Many maize breeding programs cross pure lines to produce more productive hybrids.",
    "\"Hybrid vigor\" explains why maize hybrids often outyield their parent lines.",
    "Agroecology studies crops by integrating soil, climate, and biodiversity.",
    "Soil science (edaphology) studies soil and its fertility.",
    "Plant pathology is the branch of science that studies plant diseases.",
    "Agricultural entomology studies the insects that affect or benefit crops.",
    "Germplasm banks preserve seeds from thousands of varieties to protect genetic diversity.",
    "Participatory breeding involves farmers themselves in selecting new varieties.",
    "Agricultural biotechnology ranges from tissue culture to gene editing in plants.",
    "Experimental research stations allow new varieties to be tested under real field conditions.",
    "Data science is now used to analyze agronomic and climate information at large scale.",
    "Rural extension programs aim to bring scientific knowledge to farming communities.",
    "International agricultural research often shares improved varieties between countries.",
    "Plant physiology studies how plants respond to water and heat stress.",
    "Modern agronomy combines biology, chemistry, physics, and statistics.",
    "Field trials compare how different varieties perform under the same conditions.",
    "Plant nutrition research seeks to optimize fertilizer use without harming the environment.",
    "Studying climate change is increasingly important for planning the agriculture of the future.",
    "Agricultural scientists work together with field agronomists to validate findings in practice.",
    "Pollinator conservation, such as for bees, is a growing focus of agricultural research.",
    "Urban agriculture is also studied scientifically as a food security strategy.",
    "Many scientific journals publish research on agronomy and plant pathology.",
    "International scientific collaboration has been key to tackling global maize pests and diseases.",
    "Universities with agronomy faculties train the professionals who research crops.",
    "Genomic selection uses DNA markers to speed up crop breeding.",
    "Disease prediction models help anticipate outbreaks before they spread.",
    "Post-harvest research aims to reduce grain losses after harvest.",
    "Seed certification guarantees the genetic and sanitary quality of what is planted.",
    "International scientific cooperation has allowed germplasm banks to be shared between countries.",
    "Science communication helps agronomic advances reach the people who work the land.",
)

private fun mensajesEspera(lang: Lang): List<String> =
    if (lang == Lang.ES) MENSAJES_ESPERA_ES else MENSAJES_ESPERA_EN

@Composable
private fun BurbujaEscribiendo(lang: Lang) {
    val mensajes = mensajesEspera(lang)
    var indice by remember(lang) { mutableStateOf(mensajes.indices.random()) }
    LaunchedEffect(lang) {
        while (true) {
            delay(4500)
            indice = mensajes.indices.filter { it != indice }.random()
        }
    }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        if (lang == Lang.ES) "Generando respuesta…" else "Generating response…",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Crossfade(targetState = indice, modifier = Modifier.padding(top = 6.dp)) { i ->
                    Text(
                        mensajes[i],
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
