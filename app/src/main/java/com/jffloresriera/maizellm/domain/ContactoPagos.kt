package com.jffloresriera.maizellm.domain

import android.net.Uri
import com.jffloresriera.maizellm.BuildConfig

/**
 * Pago SIMULADO: la app todavía no cobra. Para mejorar el plan, el usuario abre un chat
 * de WhatsApp con el administrador con un mensaje ya escrito que incluye su uid (el id
 * del documento en Firestore), así el administrador lo encuentra y le cambia el plan.
 *
 * El número se configura en gradle.properties (`agroia.whatsappPagos`), en formato
 * internacional sin "+" ni espacios, p. ej. 573001234567.
 */
object ContactoPagos {

    val numero: String = BuildConfig.WHATSAPP_PAGOS

    /** Número legible para mostrar en pantalla: +57 3001234567. */
    val numeroVisible: String = if (numero.length > 2) "+${numero.take(2)} ${numero.drop(2)}" else numero

    fun mensaje(plan: Plan, githubUsuario: String?, email: String?, uid: String): String = buildString {
        append("Hola, quiero mejorar mi plan de Agro IA al plan ${plan.nombre}")
        append(if (plan == Plan.PRO) " (10.000 tokens diarios).\n" else " (uso ilimitado).\n")
        githubUsuario?.let { append("Usuario de GitHub: @$it\n") }
        email?.let { append("Correo: $it\n") }
        append("ID de usuario: $uid")
    }

    fun enlaceWhatsApp(texto: String): Uri =
        Uri.parse("https://wa.me/$numero?text=${Uri.encode(texto)}")
}
