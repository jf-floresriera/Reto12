package com.jffloresriera.maizellm.domain

/**
 * Señales de riesgo descritas en la sección 9.4 del documento de contexto del proyecto:
 * el modelo puede inventar cifras con unidad, y puede "citar" una fuente de memoria en
 * vez de una fuente real recuperada. Esto NO corrige la respuesta, solo la marca para
 * que la UI muestre una advertencia; la decisión de qué hacer con eso es de la capa de UI.
 *
 * El patrón es el mismo usado en colab/preparar_modelo_android.py para mantener
 * consistencia entre la evaluación hecha en Colab y lo que ve el usuario final.
 */
object DoseGuardrail {

    private val CIFRA_CON_UNIDAD = Regex(
        """\d+[.,]?\d*\s*(L/ha|kg/ha|g/ha|ml/ha|g/L|ml/L|kg|g|L|%|d[ií]as|days|ppm)""",
        RegexOption.IGNORE_CASE,
    )

    private val CITA_FUENTE = Regex("""(fuente|source)\s*:""", RegexOption.IGNORE_CASE)

    /** true si la respuesta trae una cifra con unidad que no deberíamos mostrar sin verificar. */
    fun containsUnverifiedFigure(respuesta: String): Boolean = CIFRA_CON_UNIDAD.containsMatchIn(respuesta)

    /** true si la respuesta termina citando una fuente (como fue entrenada a hacer). */
    fun citesSource(respuesta: String): Boolean = CITA_FUENTE.containsMatchIn(respuesta)
}
