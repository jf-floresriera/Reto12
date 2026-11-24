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
import com.jffloresriera.maizellm.ui.theme.MaizeLlmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Un solo ViewModel para toda la pantalla: el tema necesita leer la
            // preferencia guardada, y ChatScreen necesita ese mismo estado — si
            // se crearan dos instancias, el modelo se cargaría dos veces.
            val viewModel: ChatViewModel = viewModel()
            val estado by viewModel.uiState.collectAsState()

            MaizeLlmTheme(tema = estado.tema) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatScreen(viewModel = viewModel)
                }
            }
        }
    }
}
