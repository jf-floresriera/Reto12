package com.jffloresriera.maizellm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jffloresriera.maizellm.ui.ChatScreen
import com.jffloresriera.maizellm.ui.ChatViewModel
import com.jffloresriera.maizellm.ui.CuentaViewModel
import com.jffloresriera.maizellm.ui.DialogoResultadoAutenticacion
import com.jffloresriera.maizellm.ui.EstadoSesion
import com.jffloresriera.maizellm.ui.PantallaInicioSesion
import com.jffloresriera.maizellm.ui.theme.MaizeLlmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Un solo ViewModel para toda la pantalla: el tema necesita leer la
            // preferencia guardada, y ChatScreen necesita ese mismo estado — si
            // se crearan dos instancias, el modelo se cargaría dos veces.
            // El modelo empieza a prepararse mientras el usuario inicia sesión.
            val viewModel: ChatViewModel = viewModel()
            val estado by viewModel.uiState.collectAsState()
            val cuentaViewModel: CuentaViewModel = viewModel()
            val estadoCuenta by cuentaViewModel.uiState.collectAsState()

            MaizeLlmTheme(tema = estado.tema) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    when (val sesion = estadoCuenta.sesion) {
                        is EstadoSesion.Activa ->
                            ChatScreen(usuario = sesion.usuario, cuentaViewModel = cuentaViewModel, viewModel = viewModel)
                        else ->
                            PantallaInicioSesion(
                                autenticando = sesion == EstadoSesion.Autenticando,
                                onIniciarSesion = cuentaViewModel::iniciarSesionConGitHub,
                            )
                    }
                    estadoCuenta.aviso?.let { aviso ->
                        DialogoResultadoAutenticacion(aviso = aviso, onCerrar = cuentaViewModel::descartarAviso)
                    }
                }
            }
        }
    }
}
