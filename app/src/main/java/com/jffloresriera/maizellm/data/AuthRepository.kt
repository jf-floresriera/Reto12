package com.jffloresriera.maizellm.data

import android.app.Activity
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.tasks.await

/** Datos públicos del perfil de GitHub que se guardan en Firestore. Nunca hay contraseña aquí. */
data class PerfilGitHub(
    val uid: String,
    val githubUsuario: String?,
    val nombre: String?,
    val email: String?,
    val fotoUrl: String?,
)

/**
 * Autenticación delegada en GitHub a través de Firebase Authentication.
 *
 * Flujo: la app abre una pestaña del navegador (Custom Tab) con la página de inicio de
 * sesión de GitHub. GitHub pide usuario y contraseña (y 2FA si lo tiene activado), y
 * devuelve a Firebase un código de autorización. Firebase lo canjea por un token usando
 * el client secret de la OAuth App, que vive en la consola de Firebase y no dentro del APK.
 * La app solo recibe el resultado: un usuario de Firebase con su uid y su perfil público.
 *
 * La sesión queda guardada en el teléfono: después del primer inicio de sesión,
 * `usuarioActual` está disponible aunque no haya Internet.
 */
class AuthRepository(private val auth: FirebaseAuth = FirebaseAuth.getInstance()) {

    val usuarioActual: FirebaseUser? get() = auth.currentUser

    /** Si Android destruyó la Activity mientras el navegador estaba abierto, el resultado queda pendiente aquí. */
    suspend fun resultadoPendiente(): PerfilGitHub? = auth.pendingAuthResult?.await()?.let(::aPerfil)

    suspend fun iniciarSesionConGitHub(activity: Activity): PerfilGitHub {
        val proveedor = OAuthProvider.newBuilder("github.com")
            // read:user para el nombre de usuario y user:email para el correo aunque sea privado.
            .setScopes(listOf("read:user", "user:email"))
            .build()
        val resultado = auth.startActivityForSignInWithProvider(activity, proveedor).await()
        return aPerfil(resultado)
    }

    fun cerrarSesion() = auth.signOut()

    private fun aPerfil(resultado: AuthResult): PerfilGitHub {
        val usuario = resultado.user ?: error("GitHub no devolvió un usuario")
        return PerfilGitHub(
            uid = usuario.uid,
            // El "login" de GitHub (p. ej. jf-floresriera) solo llega en el momento del inicio de sesión.
            githubUsuario = resultado.additionalUserInfo?.username,
            nombre = usuario.displayName,
            email = usuario.email,
            fotoUrl = usuario.photoUrl?.toString(),
        )
    }
}
