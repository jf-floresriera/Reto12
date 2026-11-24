package com.jffloresriera.maizellm.data

/**
 * Instancias compartidas entre ViewModels: el chat necesita consultar la misma cuenta
 * (plan y tokens) que la pantalla de sesión. Firebase se inicializa solo al arrancar la
 * app a partir de google-services.json, así que basta con crearlas de forma perezosa.
 */
object Servicios {
    val auth: AuthRepository by lazy { AuthRepository() }
    val cuenta: CuentaRepository by lazy { CuentaRepository() }
}
