package com.jffloresriera.maizellm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jffloresriera.maizellm.data.InstallProgress
import com.jffloresriera.maizellm.data.ModelAssetInstaller
import com.jffloresriera.maizellm.data.ModelRepository
import com.jffloresriera.maizellm.data.Preferencias
import com.jffloresriera.maizellm.data.Tema
import com.jffloresriera.maizellm.domain.Citas
import com.jffloresriera.maizellm.domain.DoseGuardrail
import com.jffloresriera.maizellm.domain.Lang
import com.jffloresriera.maizellm.domain.Level
import com.jffloresriera.maizellm.domain.SystemPrompts
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Autor { USUARIO, ASISTENTE }

data class Mensaje(
    val autor: Autor,
    val texto: String,
    val referencias: List<String> = emptyList(),
    val cifraSinVerificar: Boolean = false,
    val citaFuente: Boolean = false,
    // Métricas de generación (tiempo/velocidad), solo presentes en mensajes del
    // asistente. Vienen directo del motor de inferencia, no son estimadas.
    val tokensGenerados: Int? = null,
    val tokensPorSegundo: Float? = null,
    val tiempoGeneracionMs: Long? = null,
)

sealed interface EstadoModelo {
    data object Instalando : EstadoModelo
    data class InstalandoConProgreso(val porcentaje: Int) : EstadoModelo
    data object Cargando : EstadoModelo
    data object Listo : EstadoModelo
    data class Error(val mensaje: String) : EstadoModelo
}

data class ChatUiState(
    val estadoModelo: EstadoModelo = EstadoModelo.Instalando,
    val lang: Lang = Lang.ES,
    val level: Level = Level.AGRICULTOR,
    val mensajes: List<Mensaje> = emptyList(),
    val generandoRespuesta: Boolean = false,
    // Texto que se va revelando poco a poco una vez que el modelo ya terminó de
    // generar la respuesta completa (ver nota en enviarPregunta: no es streaming
    // real del modelo, es una animación de "tecleo" sobre la respuesta ya lista).
    val textoEnProgreso: String? = null,
    // Preferencias del usuario: todas empiezan en un valor por defecto razonable,
    // pero el usuario puede cambiarlas; se guardan en SharedPreferences.
    val mostrarMetricas: Boolean = false,
    val sonidoActivado: Boolean = true,
    val tema: Tema = Tema.SISTEMA,
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val installer = ModelAssetInstaller(application)
    private val repository = ModelRepository()
    private val preferencias = Preferencias(application)

    private val _uiState = MutableStateFlow(
        ChatUiState(
            mostrarMetricas = preferencias.mostrarMetricas,
            sonidoActivado = preferencias.sonidoActivado,
            tema = preferencias.tema,
        ),
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Evento de "sonido" (ver nota en ChatScreen): se dispara cuando la respuesta
    // ya quedó completa, para que la UI reproduzca un tono corto (si está activado).
    private val _eventosSonido = Channel<Unit>(Channel.CONFLATED)
    val eventosSonido: Flow<Unit> = _eventosSonido.receiveAsFlow()

    init {
        prepararModelo()
    }

    private fun prepararModelo() {
        viewModelScope.launch {
            try {
                installer.install().collect { progreso ->
                    when (progreso) {
                        is InstallProgress.Copying -> {
                            val pct = if (progreso.bytesTotales > 0) {
                                (progreso.bytesCopiados * 100 / progreso.bytesTotales).toInt()
                            } else 0
                            _uiState.update { it.copy(estadoModelo = EstadoModelo.InstalandoConProgreso(pct)) }
                        }
                        is InstallProgress.Listo -> {
                            _uiState.update { it.copy(estadoModelo = EstadoModelo.Cargando) }
                            repository.cargar(progreso.rutaModelo)
                            _uiState.update { it.copy(estadoModelo = EstadoModelo.Listo) }
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(estadoModelo = EstadoModelo.Error(e.message ?: "Error desconocido al preparar el modelo"))
                }
            }
        }
    }

    fun cambiarIdioma(lang: Lang) = _uiState.update { it.copy(lang = lang) }

    fun cambiarNivel(level: Level) = _uiState.update { it.copy(level = level) }

    fun cambiarMostrarMetricas(valor: Boolean) {
        preferencias.mostrarMetricas = valor
        _uiState.update { it.copy(mostrarMetricas = valor) }
    }

    fun cambiarSonido(valor: Boolean) {
        preferencias.sonidoActivado = valor
        _uiState.update { it.copy(sonidoActivado = valor) }
    }

    fun cambiarTema(valor: Tema) {
        preferencias.tema = valor
        _uiState.update { it.copy(tema = valor) }
    }

    fun enviarPregunta(pregunta: String) {
        if (pregunta.isBlank() || _uiState.value.estadoModelo != EstadoModelo.Listo) return

        val estadoActual = _uiState.value
        _uiState.update {
            it.copy(
                mensajes = it.mensajes + Mensaje(Autor.USUARIO, pregunta),
                generandoRespuesta = true,
            )
        }

        viewModelScope.launch {
            try {
                val systemPrompt = SystemPrompts.forAndroid(estadoActual.lang, estadoActual.level)
                // La librería gratuita no da streaming real del modelo: la respuesta
                // completa llega de una sola vez aquí. Lo que sigue (revelarEnPantalla)
                // es solo una animación visual sobre el texto ya generado.
                val resultado = repository.preguntar(systemPrompt, pregunta)
                revelarEnPantalla(
                    textoCompleto = resultado.text,
                    tokensGenerados = resultado.tokensGenerated,
                    tokensPorSegundo = resultado.tokensPerSecond,
                    tiempoGeneracionMs = resultado.generateTimeMs,
                )
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        mensajes = it.mensajes + Mensaje(
                            autor = Autor.ASISTENTE,
                            texto = "No pude generar una respuesta: ${e.message}",
                        ),
                        generandoRespuesta = false,
                        textoEnProgreso = null,
                    )
                }
            }
        }
    }

    /**
     * Simula el efecto de "tecleo" revelando el texto ya generado de a pocos
     * caracteres, en vez de mostrarlo todo de golpe. Al terminar, lo convierte
     * en un Mensaje definitivo con las señales de riesgo y las métricas de
     * generación ya calculadas.
     */
    private suspend fun revelarEnPantalla(
        textoCompleto: String,
        tokensGenerados: Int,
        tokensPorSegundo: Float,
        tiempoGeneracionMs: Long,
    ) {
        val tamanoBloque = 3
        var i = 0
        while (i < textoCompleto.length) {
            i = (i + tamanoBloque).coerceAtMost(textoCompleto.length)
            _uiState.update { it.copy(textoEnProgreso = textoCompleto.substring(0, i)) }
            delay(16)
        }
        val (cuerpo, referencias) = Citas.extraer(textoCompleto)
        _uiState.update {
            it.copy(
                mensajes = it.mensajes + Mensaje(
                    autor = Autor.ASISTENTE,
                    texto = cuerpo,
                    referencias = referencias,
                    cifraSinVerificar = DoseGuardrail.containsUnverifiedFigure(textoCompleto),
                    citaFuente = referencias.isNotEmpty(),
                    tokensGenerados = tokensGenerados,
                    tokensPorSegundo = tokensPorSegundo,
                    tiempoGeneracionMs = tiempoGeneracionMs,
                ),
                generandoRespuesta = false,
                textoEnProgreso = null,
            )
        }
        if (_uiState.value.sonidoActivado) {
            _eventosSonido.trySend(Unit)
        }
    }

    fun borrarConversacion() = _uiState.update { it.copy(mensajes = emptyList()) }

    /** Texto plano de toda la conversación, para compartir o descargar. */
    fun construirTranscripcion(): String {
        val estado = _uiState.value
        val mensajes = estado.mensajes
        if (mensajes.isEmpty()) return "Todavía no hay conversación con Agro IA."
        return buildString {
            appendLine("Conversación con Agro IA")
            appendLine("Idioma: ${etiquetaIdioma(estado.lang)} · Nivel: ${etiquetaNivelTranscripcion(estado.level)}")
            appendLine()
            mensajes.forEach { m ->
                appendLine(if (m.autor == Autor.USUARIO) "Tú:" else "Agro IA:")
                appendLine(m.texto)
                appendLine()
            }
        }
    }

    private fun etiquetaIdioma(lang: Lang): String = if (lang == Lang.ES) "Español" else "Inglés"

    private fun etiquetaNivelTranscripcion(level: Level): String = when (level) {
        Level.AGRICULTOR -> "Agricultor"
        Level.TECNICO -> "Técnico"
        Level.CIENTIFICO -> "Científico"
    }

    override fun onCleared() {
        repository.liberar()
        super.onCleared()
    }
}
