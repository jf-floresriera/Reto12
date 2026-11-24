package com.jffloresriera.maizellm.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.jffloresriera.maizellm.data.Tema

// Paleta propia del proyecto (ver Color.kt), consistente con el README y las gráficas.
// Si más adelante se compara con el estilo de otra app de referencia, solo hay que
// tocar este archivo: el resto de la UI consume MaterialTheme.colorScheme, no colores sueltos.

private val EsquemaClaro = lightColorScheme(
    primary = Azul,
    onPrimary = Color.White,
    primaryContainer = AzulContenedor,
    onPrimaryContainer = AzulOscuro,
    secondary = Verde,
    onSecondary = Color.White,
    secondaryContainer = VerdeContenedor,
    tertiary = Dorado,
    tertiaryContainer = DoradoContenedor,
    error = Rojo,
    errorContainer = RojoContenedor,
    onErrorContainer = Color(0xFF5F1611),
    background = FondoClaro,
    surface = SuperficieClara,
    surfaceVariant = Color(0xFFEFF2F6),
)

private val EsquemaOscuro = darkColorScheme(
    primary = Color(0xFF9FC7FF),
    secondary = Color(0xFF9BD8AC),
    tertiary = Color(0xFFFFD866),
    error = Color(0xFFFFB4A9),
)

@Composable
fun MaizeLlmTheme(
    tema: Tema = Tema.SISTEMA,
    content: @Composable () -> Unit,
) {
    val usarOscuro = when (tema) {
        Tema.SISTEMA -> isSystemInDarkTheme()
        Tema.CLARO -> false
        Tema.OSCURO -> true
    }
    val colorScheme = if (usarOscuro) EsquemaOscuro else EsquemaClaro
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
