package com.jffloresriera.maizellm.data

import android.content.Context

/** Tema visual de la app. SISTEMA sigue el modo claro/oscuro del teléfono. */
enum class Tema { SISTEMA, CLARO, OSCURO }

/**
 * Envuelve SharedPreferences para las opciones que el usuario puede activar o
 * desactivar: nada de esto viene encendido "porque sí", todo es decisión suya.
 */
class Preferencias(context: Context) {
    private val prefs = context.getSharedPreferences("maize_ia_prefs", Context.MODE_PRIVATE)

    var mostrarMetricas: Boolean
        get() = prefs.getBoolean("mostrar_metricas", false)
        set(value) = prefs.edit().putBoolean("mostrar_metricas", value).apply()

    var sonidoActivado: Boolean
        get() = prefs.getBoolean("sonido_activado", true)
        set(value) = prefs.edit().putBoolean("sonido_activado", value).apply()

    var tema: Tema
        get() = Tema.entries.getOrElse(prefs.getInt("tema", 0)) { Tema.SISTEMA }
        set(value) = prefs.edit().putInt("tema", value.ordinal).apply()
}
