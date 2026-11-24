package com.jffloresriera.maizellm.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Planes de uso. El valor `id` es exactamente el texto que se guarda en el campo
 * `plan` del documento del usuario en Firestore (colección `usuarios`). Para cambiarle
 * el plan a alguien, el administrador edita ese campo desde la consola de Firebase.
 *
 * - GRATIS: 1.000 tokens en total (de prueba, no se renuevan).
 * - PRO: 10.000 tokens por día (se reinician cada día, según la fecha del teléfono).
 * - ULTRA: uso ilimitado ("Ultra Plus").
 * - BLOQUEADO: el administrador le quitó el acceso al usuario.
 */
enum class Plan(val id: String, val nombre: String, val limite: Int?, val esDiario: Boolean) {
    GRATIS("gratis", "Gratis", 1_000, esDiario = false),
    PRO("pro", "Pro", 10_000, esDiario = true),
    ULTRA("ultra", "Ultra Plus", null, esDiario = false),
    BLOQUEADO("bloqueado", "Bloqueado", 0, esDiario = false),
    ;

    companion object {
        /** Un valor desconocido o vacío en Firestore se trata como GRATIS (el más restrictivo con uso). */
        fun desdeId(id: String?): Plan = entries.firstOrNull { it.id == id?.trim()?.lowercase() } ?: GRATIS
    }
}

/** Copia local del documento `usuarios/{uid}` de Firestore. */
data class Cuenta(
    val plan: Plan = Plan.GRATIS,
    val tokensTotales: Long = 0,
    val tokensHoy: Long = 0,
    /** Día (yyyy-MM-dd) al que corresponde `tokensHoy`. */
    val fechaConsumo: String = "",
    val planSolicitado: String? = null,
    val githubUsuario: String? = null,
)

object Cuotas {

    fun hoy(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    /** Tokens ya usados dentro del periodo que cuenta para el plan. */
    fun consumidos(cuenta: Cuenta, hoy: String = hoy()): Long = when {
        cuenta.plan.esDiario -> if (cuenta.fechaConsumo == hoy) cuenta.tokensHoy else 0
        else -> cuenta.tokensTotales
    }

    /** Tokens que todavía puede generar. `null` significa sin límite. */
    fun disponibles(cuenta: Cuenta, hoy: String = hoy()): Long? {
        val limite = cuenta.plan.limite ?: return null
        return (limite - consumidos(cuenta, hoy)).coerceAtLeast(0)
    }

    fun puedeGenerar(cuenta: Cuenta, hoy: String = hoy()): Boolean {
        val disponibles = disponibles(cuenta, hoy)
        return disponibles == null || disponibles > 0
    }
}
