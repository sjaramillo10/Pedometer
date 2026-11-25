package dev.sjaramillo.nemi.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.sjaramillo.nemi.NemiApplication

class MainActivity : ComponentActivity() {
    private val incomingIntent = mutableStateOf<Intent?>(null)

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        val graph = (application as NemiApplication).graph
        viewModelFactory {
            initializer { graph.homeViewModel }
            initializer { graph.settingsViewModel }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        val initialIntent = intent
        setContent { NemiRoute(this, initialIntent, incomingIntent.value) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incomingIntent.value = intent
    }
}
