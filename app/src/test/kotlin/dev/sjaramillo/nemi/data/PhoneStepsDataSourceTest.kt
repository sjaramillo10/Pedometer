package dev.sjaramillo.nemi.data

import androidx.health.connect.client.records.metadata.DataOrigin
import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneStepsDataSourceTest {
    @Test
    fun `includes legacy Android and current device origins`() {
        val deviceOrigin = "com.android.healthconnect.phone.test"

        assertEquals(
            setOf(DataOrigin("android"), DataOrigin(deviceOrigin)),
            phoneStepDataOrigins(deviceOrigin),
        )
    }

    @Test
    fun `uses legacy Android origin when current device origin is unavailable`() {
        assertEquals(setOf(DataOrigin("android")), phoneStepDataOrigins(null))
    }
}
