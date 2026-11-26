package dev.sjaramillo.nemi.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import dev.sjaramillo.nemi.NemiApplication

class MainActivity : ComponentActivity() {
    private lateinit var route: NemiRoute

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory by lazy {
        val nemi = application as NemiApplication
        viewModelFactory {
            initializer { nemi.createHomeViewModel() }
            initializer { nemi.createSettingsViewModel() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.enableEdgeToEdge(window)
        route = NemiRoute(this, savedInstanceState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        route.handleIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        route.saveState(outState)
    }
}
