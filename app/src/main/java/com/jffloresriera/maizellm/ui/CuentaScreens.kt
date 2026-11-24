package com.jffloresriera.maizellm.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jffloresriera.maizellm.R
import com.jffloresriera.maizellm.domain.ContactoPagos
import com.jffloresriera.maizellm.domain.Cuenta
import com.jffloresriera.maizellm.domain.Cuotas
import com.jffloresriera.maizellm.domain.Plan
import java.text.NumberFormat
import java.util.Locale

private fun Context.buscarActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.buscarActivity()
    else -> null
}

private fun formatear(n: Long): String = NumberFormat.getIntegerInstance(Locale("es", "CO")).format(n)

/** Pantalla de inicio: la app no pide ni guarda contraseñas, solo redirige a GitHub. */
@Composable
fun PantallaInicioSesion(autenticando: Boolean, onIniciarSesion: (Activity) -> Unit) {
    val contexto = LocalContext.current
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.Unspecified,
            )
            Text("Agro IA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Chatbot de sanidad del maíz que corre en tu celular.",
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Button(
                onClick = { contexto.buscarActivity()?.let(onIniciarSesion) },
                enabled = !autenticando,
                modifier = Modifier.padding(top = 32.dp).fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF24292F), contentColor = Color.White),
            ) {
                if (autenticando) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    Text("Esperando a GitHub…", modifier = Modifier.padding(start = 12.dp))
                } else {
                    Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                    Text("Iniciar sesión con GitHub", modifier = Modifier.padding(start = 12.dp))
                }
            }
            Text(
                "Se abrirá la página de GitHub para que ingreses tus credenciales. " +
                    "Agro IA no ve ni guarda tu contraseña. Solo necesitas Internet para este paso; " +
                    "después el chat funciona sin conexión.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                "Plan Gratis: ${formatear(Plan.GRATIS.limite!!.toLong())} tokens de prueba.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Mensaje de resultado de la autenticación: exitoso o fallido. */
@Composable
fun DialogoResultadoAutenticacion(aviso: AvisoAutenticacion, onCerrar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCerrar,
        icon = {
            Icon(
                if (aviso.exitoso) Icons.Filled.CheckCircle else Icons.Filled.Error,
                contentDescription = null,
                tint = if (aviso.exitoso) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(if (aviso.exitoso) "Autenticación exitosa" else "Autenticación fallida") },
        text = { Text(aviso.detalle) },
        confirmButton = {
            TextButton(onClick = onCerrar) { Text(if (aviso.exitoso) "Continuar" else "Cerrar") }
        },
    )
}

/** Franja con el plan y los tokens restantes; al tocarla se abre el detalle del plan. */
@Composable
fun BarraPlan(cuenta: Cuenta?, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            if (cuenta == null) {
                Text("Verificando tu plan…", style = MaterialTheme.typography.labelMedium)
                return@Column
            }
            val disponibles = Cuotas.disponibles(cuenta)
            val limite = cuenta.plan.limite
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.WorkspacePremium, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    "Plan ${cuenta.plan.nombre}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp).weight(1f),
                )
                Text(
                    when {
                        cuenta.plan == Plan.BLOQUEADO -> "Sin acceso"
                        disponibles == null -> "Uso ilimitado"
                        cuenta.plan.esDiario -> "${formatear(disponibles)} tokens hoy"
                        else -> "${formatear(disponibles)} tokens restantes"
                    },
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (limite != null && limite > 0) {
                LinearProgressIndicator(
                    progress = { (Cuotas.consumidos(cuenta).toFloat() / limite).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
    }
}

/** Reemplaza la caja de texto del chat cuando no quedan tokens o el acceso está bloqueado. */
@Composable
fun AvisoSinTokens(cuenta: Cuenta?, onVerPlanes: () -> Unit) {
    val texto = when {
        cuenta == null ->
            "No encontramos tu registro en este teléfono. Conéctate a Internet un momento para verificar tu plan."
        cuenta.plan == Plan.BLOQUEADO ->
            "El administrador desactivó tu acceso. Contáctalo por WhatsApp si crees que es un error."
        cuenta.plan.esDiario ->
            "Usaste los ${formatear(cuenta.plan.limite!!.toLong())} tokens de hoy. Mañana se renuevan, " +
                "o puedes pasar a Ultra Plus."
        else ->
            "Usaste tus ${formatear(cuenta.plan.limite!!.toLong())} tokens de prueba. Mejora tu plan para seguir usando Agro IA."
    }
    Surface(color = MaterialTheme.colorScheme.errorContainer, shadowElevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                texto,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            if (cuenta != null) {
                Button(onClick = onVerPlanes) { Text("Ver planes") }
            }
        }
    }
}

/**
 * Detalle del plan y opciones para mejorarlo. El pago es SIMULADO: se abre WhatsApp
 * con un mensaje para el administrador, que luego cambia el plan en Firebase.
 */
@Composable
fun DialogoPlanes(
    cuenta: Cuenta?,
    usuario: UsuarioSesion,
    mensajePlan: String?,
    onSolicitar: (Plan) -> Unit,
    onRefrescar: () -> Unit,
    onCerrar: () -> Unit,
) {
    val contexto = LocalContext.current
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Mi plan") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                cuenta?.githubUsuario?.let { Text("GitHub: @$it", style = MaterialTheme.typography.bodyMedium) }
                usuario.email?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (cuenta != null) {
                    Text(
                        "Plan actual: ${cuenta.plan.nombre}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Text(
                        "Tokens usados en total: ${formatear(cuenta.tokensTotales)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (cuenta.plan.esDiario) {
                        Text(
                            "Tokens usados hoy: ${formatear(Cuotas.consumidos(cuenta))}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    cuenta.planSolicitado?.let { id ->
                        if (id != cuenta.plan.id) {
                            Text(
                                "Solicitud pendiente: plan ${Plan.desdeId(id).nombre}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                TarjetaPlan(Plan.GRATIS, "${formatear(1_000)} tokens en total, de prueba.", cuenta, null)
                TarjetaPlan(Plan.PRO, "${formatear(10_000)} tokens cada día.", cuenta) {
                    solicitar(contexto, Plan.PRO, cuenta, usuario, onSolicitar)
                }
                TarjetaPlan(Plan.ULTRA, "Uso ilimitado.", cuenta) {
                    solicitar(contexto, Plan.ULTRA, cuenta, usuario, onSolicitar)
                }

                Text(
                    "El pago todavía no se hace dentro de la app. Al elegir un plan se abre WhatsApp " +
                        "para coordinar el pago con el administrador al ${ContactoPagos.numeroVisible}. " +
                        "Cuando lo active, toca \"Actualizar\" con Internet.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                mensajePlan?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onCerrar) { Text("Cerrar") } },
        dismissButton = { TextButton(onClick = onRefrescar) { Text("Actualizar") } },
    )
}

@Composable
private fun TarjetaPlan(plan: Plan, descripcion: String, cuenta: Cuenta?, onElegir: (() -> Unit)?) {
    val esActual = cuenta?.plan == plan
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (esActual) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(plan.nombre, fontWeight = FontWeight.Bold)
                Text(descripcion, style = MaterialTheme.typography.bodySmall)
            }
            when {
                esActual -> Text("Actual", style = MaterialTheme.typography.labelMedium)
                onElegir != null -> OutlinedButton(onClick = onElegir) { Text("Solicitar") }
            }
        }
    }
}

private fun solicitar(
    contexto: Context,
    plan: Plan,
    cuenta: Cuenta?,
    usuario: UsuarioSesion,
    onSolicitar: (Plan) -> Unit,
) {
    onSolicitar(plan)
    val texto = ContactoPagos.mensaje(plan, cuenta?.githubUsuario, usuario.email, usuario.uid)
    val intent = Intent(Intent.ACTION_VIEW, ContactoPagos.enlaceWhatsApp(texto))
    try {
        contexto.startActivity(intent)
    } catch (e: Exception) {
        // Sin WhatsApp ni navegador: al menos se deja el texto listo para compartir.
        contexto.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, texto) },
                "Enviar solicitud de plan",
            ),
        )
    }
}
