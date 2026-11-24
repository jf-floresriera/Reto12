package com.jffloresriera.maizellm.data

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.jffloresriera.maizellm.domain.Cuenta
import com.jffloresriera.maizellm.domain.Cuotas
import com.jffloresriera.maizellm.domain.Plan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Registro de usuarios, plan y consumo de tokens en Cloud Firestore.
 *
 * Cada usuario es un documento `usuarios/{uid}`. Desde la consola de Firebase el
 * administrador ve la lista completa y cambia el campo `plan` ("gratis", "pro",
 * "ultra" o "bloqueado"). Las reglas de seguridad (firestore.rules) impiden que la
 * app del usuario cambie su propio plan o baje su contador de tokens.
 *
 * Funcionamiento sin Internet: Firestore guarda en el teléfono una copia del documento.
 * Las lecturas salen de esa copia y las escrituras (el consumo de tokens) quedan en cola
 * y se envían solas cuando vuelve la conexión. Por eso solo el registro inicial necesita
 * Internet. Un cambio de plan hecho por el administrador llega la próxima vez que el
 * teléfono tenga conexión con la app abierta.
 */
class CuentaRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    private val _cuenta = MutableStateFlow<Cuenta?>(null)

    /** `null` mientras no se ha leído el documento (ni siquiera de la copia local). */
    val cuenta: StateFlow<Cuenta?> = _cuenta.asStateFlow()

    private var escucha: ListenerRegistration? = null
    private var uidActual: String? = null

    private fun documento(uid: String) = db.collection(COLECCION).document(uid)

    /**
     * Se llama justo después de autenticarse con GitHub (con Internet). Crea el registro
     * con el plan gratis si es la primera vez; si ya existía, solo actualiza el perfil y
     * la fecha del último acceso, sin tocar el plan ni el consumo.
     */
    suspend fun registrarOActualizar(perfil: PerfilGitHub) {
        val doc = documento(perfil.uid)
        val existente = doc.get(Source.SERVER).await()
        val datosPerfil = buildMap<String, Any?> {
            put("uid", perfil.uid)
            perfil.githubUsuario?.let { put("githubUsuario", it) }
            put("nombre", perfil.nombre)
            put("email", perfil.email)
            put("fotoUrl", perfil.fotoUrl)
            put("proveedor", "github.com")
            put("ultimoAcceso", FieldValue.serverTimestamp())
        }
        if (existente.exists()) {
            doc.set(datosPerfil, SetOptions.merge()).await()
        } else {
            doc.set(
                datosPerfil + mapOf(
                    "plan" to Plan.GRATIS.id,
                    "tokensTotales" to 0L,
                    "tokensHoy" to 0L,
                    "fechaConsumo" to Cuotas.hoy(),
                    "fechaRegistro" to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
    }

    /** Empieza a seguir el documento del usuario (desde la copia local y, si hay red, desde el servidor). */
    fun escuchar(uid: String) {
        if (uid == uidActual && escucha != null) return
        detener()
        uidActual = uid
        escucha = documento(uid).addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            if (snapshot.exists()) _cuenta.value = aCuenta(snapshot)
        }
    }

    /** Suma los tokens generados en una respuesta. Funciona sin conexión (queda en cola). */
    fun registrarConsumo(tokens: Int) {
        val uid = uidActual ?: return
        val actual = _cuenta.value ?: return
        if (tokens <= 0) return
        val hoy = Cuotas.hoy()
        val n = tokens.toLong()
        val cambios: Map<String, Any> = if (actual.fechaConsumo == hoy) {
            mapOf("tokensHoy" to FieldValue.increment(n), "tokensTotales" to FieldValue.increment(n))
        } else {
            mapOf("fechaConsumo" to hoy, "tokensHoy" to n, "tokensTotales" to FieldValue.increment(n))
        }
        // No se espera el resultado: sin Internet la tarea no termina hasta que vuelva la
        // conexión, pero la copia local ya refleja el cambio y el contador de la UI se actualiza.
        documento(uid).update(cambios)
    }

    /** Deja constancia en Firestore de que el usuario pidió un plan (el pago se coordina por WhatsApp). */
    fun solicitarPlan(plan: Plan) {
        val uid = uidActual ?: return
        documento(uid).update(
            mapOf("planSolicitado" to plan.id, "fechaSolicitud" to FieldValue.serverTimestamp()),
        )
    }

    /** Fuerza una lectura desde el servidor. Devuelve false si no hay conexión. */
    suspend fun refrescarDesdeServidor(): Boolean {
        val uid = uidActual ?: return false
        return try {
            val snapshot = documento(uid).get(Source.SERVER).await()
            if (snapshot.exists()) _cuenta.value = aCuenta(snapshot)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun detener() {
        escucha?.remove()
        escucha = null
        uidActual = null
        _cuenta.value = null
    }

    private fun aCuenta(s: DocumentSnapshot) = Cuenta(
        plan = Plan.desdeId(s.getString("plan")),
        tokensTotales = s.getLong("tokensTotales") ?: 0,
        tokensHoy = s.getLong("tokensHoy") ?: 0,
        fechaConsumo = s.getString("fechaConsumo") ?: "",
        planSolicitado = s.getString("planSolicitado"),
        githubUsuario = s.getString("githubUsuario"),
    )

    companion object {
        const val COLECCION = "usuarios"
    }
}
