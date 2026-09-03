package dev.sjaramillo.pedometer.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import dev.sjaramillo.pedometer.R

/** Displays the Health Connect permission-usage privacy policy. */
class HealthConnectPrivacyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                setPadding(48, 48, 48, 48)
                setText(R.string.health_connect_privacy_policy)
            },
        )
    }
}
