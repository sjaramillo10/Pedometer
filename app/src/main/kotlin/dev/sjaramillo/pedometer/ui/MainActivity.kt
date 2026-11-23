package dev.sjaramillo.pedometer.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val incomingIntent = mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialIntent = intent
        setContent { PedometerRoute(this, initialIntent, incomingIntent.value) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        incomingIntent.value = intent
    }
}
