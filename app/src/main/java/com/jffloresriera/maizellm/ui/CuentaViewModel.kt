package com.jffloresriera.maizellm.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.jffloresriera.maizellm.data.PerfilGitHub
import com.jffloresriera.maizellm.data.Servicios
import com.jffloresriera.maizellm.domain.Cuenta
import com.jffloresriera.maizellm.domain.Plan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UsuarioSesion(val uid: String, val nombre: String?, val email: String?)

sealed interface EstadoSesion {
    data object SinSesion : EstadoSesion
    data object Autenticando : EstadoSesion
    data class Activa(val usuario: UsuarioSesion) : EstadoSesion
}

/** Mensaje que se muestra al terminar el intento de autenticación (exitoso o fallido). */
data class AvisoAutenticacion(val exitoso: Boolean, val detalle: String)

data class CuentaUiState(
    val sesion: EstadoSesion = EstadoSesion.SinSesion,
    val cuenta: Cuenta? = null,
    val aviso: AvisoAutenticacion? = null,
    val mensajePlan: String? = null,
)

class CuentaViewModel : ViewModel() {

    private val auth = Servicios.auth
    private val cuentas = Servicios.cuenta

    private val _uiState = MutableStateFlow(CuentaUiState())
    val uiState: StateFlow<CuentaUiState> = _uiState.asStateFlow()

    init {
        val usuario = auth.usuarioActual
        if (usuario != null) {
            // Sesión guardada de antes: no hace falta Internet para seguir usando la app.
            activarSesion(UsuarioSesion(usuario.uid, usuario.displayName, usuario.email))
        } else {
            viewModelScope.launch {
                try {
                    auth.resultadoPendiente()?.let { completarRegistro(it) }
                } catch (e: Exception) {
                    fallo(e)
                }
            }
        }
        viewModelScope.launch {
            cuentas.cuenta.collect { cuenta -> _uiState.update { it.copy(cuenta = cuenta) } }
        }
    }

    fun iniciarSesionConGitHub(activity: Activity) {
        if (_uiState.value.sesion == EstadoSesion.Autenticando) return
        _uiState.update { it.copy(sesion = EstadoSesion.Autenticando, aviso = null) }
        viewModelScope.launch {
            try {
                completarRegistro(auth.iniciarSesionConGitHub(activity))
            } catch (e: Exception) {
                fallo(e)
            }
        }
    }

    private suspend fun completarRegistro(perfil: PerfilGitHub) {
        try {
            cuentas.registrarOActualizar(perfil)
        } catch (e: Exception) {
            // GitHub sí autenticó, pero sin registro en la base no se puede controlar el
            // plan; se cierra la sesión para no dejar al usuario a medias.
            auth.cerrarSesion()
            throw e
        }
        activarSesion(UsuarioSesion(perfil.uid, perfil.nombre, perfil.email))
        val quien = perfil.githubUsuario?.let { "@$it" } ?: perfil.nombre ?: perfil.email ?: "usuario"
        _uiState.update {
            it.copy(aviso = AvisoAutenticacion(true, "Iniciaste sesión con GitHub como $quien."))
        }
    }

    private fun activarSesion(usuario: UsuarioSesion) {
        cuentas.escuchar(usuario.uid)
        _uiState.update { it.copy(sesion = EstadoSesion.Activa(usuario)) }
    }

    private fun fallo(e: Exception) {
        _uiState.update {
            it.copy(sesion = EstadoSesion.SinSesion, aviso = AvisoAutenticacion(false, describirError(e)))
        }
    }

    fun descartarAviso() = _uiState.update { it.copy(aviso = null) }

    fun cerrarSesion() {
        cuentas.detener()
        auth.cerrarSesion()
        _uiState.update { CuentaUiState(sesion = EstadoSesion.SinSesion) }
    }

    fun registrarSolicitud(plan: Plan) = cuentas.solicitarPlan(plan)

    fun refrescarPlan() {
        viewModelScope.launch {
            val ok = cuentas.refrescarDesdeServidor()
            _uiState.update {
                it.copy(
                    mensajePlan = if (ok) {
                        "Plan actualizado desde el servidor."
                    } else {
                        "Sin conexión. El plan se actualizará cuando haya Internet."
                    },
                )
            }
        }
    }

    fun descartarMensajePlan() = _uiState.update { it.copy(mensajePlan = null) }

    private fun describirError(e: Exception): String = when {
        e is FirebaseNetworkException || e.cause is FirebaseNetworkException ->
            "No hay conexión a Internet. El registro con GitHub necesita Internet la primera vez."
        e is FirebaseAuthUserCollisionException ->
            "Ya existe una cuenta con ese correo registrada con otro método de acceso."
        e is FirebaseAuthException && e.errorCode == "ERROR_WEB_CONTEXT_CANCELED" ->
            "Se canceló el inicio de sesión en GitHub."
        e is FirebaseAuthException ->
            "GitHub o Firebase rechazaron el acceso (${e.errorCode}): ${e.localizedMessage}"
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE ->
            "No se pudo contactar la base de datos. Revisa tu conexión e inténtalo de nuevo."
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "La base de datos rechazó el registro (revisa las reglas de Firestore)."
        else -> e.localizedMessage ?: "Error desconocido"
    }
}
