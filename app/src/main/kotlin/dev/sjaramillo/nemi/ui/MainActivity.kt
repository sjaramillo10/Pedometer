package dev.sjaramillo.nemi.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.core.view.WindowCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val incomingIntent = mutableStateOf<Intent?>(null)

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
