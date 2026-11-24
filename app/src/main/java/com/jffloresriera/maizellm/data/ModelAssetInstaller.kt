package com.jffloresriera.maizellm.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File

/** Resultado de instalar el modelo: dónde quedó, o cuánto se ha copiado todavía. */
sealed interface InstallProgress {
    data class Copying(val bytesCopiados: Long, val bytesTotales: Long) : InstallProgress
    data class Listo(val rutaModelo: String) : InstallProgress
}

/**
 * El .gguf se empaqueta en `assets/models/`, pero la librería de inferencia necesita una
 * ruta de archivo real en disco (no puede leer directo de los assets del APK). Esta clase
 * lo copia una sola vez a almacenamiento interno de la app, con progreso, la primera vez
 * que se abre la app.
 */
class ModelAssetInstaller(private val context: Context) {

    companion object {
        private const val ASSET_PATH = "models/maize_llama32_1b_Q4_K_M.gguf"
        private const val NOMBRE_ARCHIVO_DESTINO = "maize_llama32_1b_Q4_K_M.gguf"
    }

    fun install(): Flow<InstallProgress> = flow {
        val destino = File(File(context.filesDir, "models").apply { mkdirs() }, NOMBRE_ARCHIVO_DESTINO)

        if (destino.exists() && destino.length() > 0) {
            emit(InstallProgress.Listo(destino.absolutePath))
            return@flow
        }

        val afd = context.assets.openFd(ASSET_PATH)
        val total = afd.length
        afd.close()

        context.assets.open(ASSET_PATH).use { entrada ->
            destino.outputStream().use { salida ->
                val buffer = ByteArray(1 shl 20) // 1 MiB
                var copiados = 0L
                var leidos: Int
                while (true) {
                    leidos = entrada.read(buffer)
                    if (leidos <= 0) break
                    salida.write(buffer, 0, leidos)
                    copiados += leidos
                    emit(InstallProgress.Copying(copiados, total))
                }
            }
        }
        emit(InstallProgress.Listo(destino.absolutePath))
    }.flowOn(Dispatchers.IO)
}
