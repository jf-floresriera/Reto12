package com.jffloresriera.maizellm.domain

/**
 * Separa el cuerpo de la respuesta de sus citas de fuente ("Fuente: ..." / "Source: ...").
 *
 * El modelo fue entrenado (SFT) para terminar casi siempre con UNA sola cita (ver
 * sección 9.4 del documento de contexto del proyecto). Esta utilidad no fuerza al
 * modelo a citar más de una fuente — solo prepara la UI para mostrar por separado
 * todas las citas que el modelo haya incluido, si llega a incluir más de una.
 */
object Citas {
    private val PATRON = Regex("""(?:Fuente|Source)\s*:\s*([^\n]+)""", RegexOption.IGNORE_CASE)

    /** @return el cuerpo del mensaje sin las líneas de cita, y la lista de citas encontradas. */
    fun extraer(texto: String): Pair<String, List<String>> {
        val referencias = PATRON.findAll(texto).map { it.groupValues[1].trim() }.toList()
        val cuerpo = PATRON.replace(texto, "").trimEnd()
        return cuerpo to referencias
    }
}
