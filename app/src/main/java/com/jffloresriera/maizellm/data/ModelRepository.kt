package com.jffloresriera.maizellm.data

import dev.ffmpegkit.llama.Llama
import dev.ffmpegkit.llama.LlamaConfig
import dev.ffmpegkit.llama.LlamaModel
import dev.ffmpegkit.llama.LlamaResult

/**
 * Envuelve la librería de inferencia (AAR precompilado de llama.cpp, ver app/build.gradle.kts).
 * No hay streaming en la versión gratuita de la librería: `preguntar()` devuelve la
 * respuesta completa de una vez, no token por token. Para la UI eso significa mostrar un
 * indicador de "escribiendo…" mientras se espera, no ir revelando palabras.
 *
 * NOTA de fidelidad con la evaluación hecha en Colab: allí se usó decodificación voraz
 * (`do_sample=False`) con `repetition_penalty=1.1`. Esta librería no expone penalización
 * por repetición, solo temperatura/top-p/top-k/seed. Se deja `temperature = 0f` (voraz)
 * para acercarse lo más posible a esas condiciones, pero las respuestas en el celular
 * pueden diferir un poco de las medidas en `sft_eval_test.json` por esa diferencia.
 */
class ModelRepository {

    private var modelo: LlamaModel? = null

    suspend fun cargar(rutaModelo: String) {
        if (modelo?.isLoaded == true) return
        modelo = Llama.loadModel(
            modelPath = rutaModelo,
            config = LlamaConfig(contextSize = 2048, threads = 4, temperature = 0f),
        )
    }

    suspend fun preguntar(systemPrompt: String, pregunta: String, maxTokens: Int = 320): LlamaResult {
        val modeloActual = modelo ?: error("El modelo no está cargado todavía: llama a cargar() primero.")
        return Llama.complete(
            modeloActual,
            prompt = pregunta,
            systemPrompt = systemPrompt,
            maxTokens = maxTokens,
        )
    }

    fun liberar() {
        modelo?.let { Llama.releaseModel(it) }
        modelo = null
    }
}
