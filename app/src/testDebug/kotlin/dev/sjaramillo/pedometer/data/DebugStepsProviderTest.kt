package dev.sjaramillo.pedometer.data

import android.content.SharedPreferences
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy
import java.time.LocalDate
import javax.inject.Provider

class DebugStepsProviderTest {
    @Test
    fun `real is default and selection survives provider recreation`() {
        val preferences = preferences()
        val provider = provider(preferences)
        assertFalse(provider.isFake)

        provider.setFakeSteps(true)
        assertTrue(provider(preferences).isFake)

        provider.setFakeSteps(false)
        assertFalse(provider(preferences).isFake)
    }

    @Test
    fun `fake mode never constructs Health Connect reader`() =
        runBlocking {
            val provider = provider(preferences())
            provider.setFakeSteps(true)
            val day = LocalDate.ofEpochDay(0)

            assertEquals(StepsAccess.Available, provider.access())
            assertEquals(mapOf(day to 1500L), provider.readDailySteps(day, day))
        }

    @Test
    fun `switching back selects real reader`() =
        runBlocking {
            val provider = provider(preferences())
            provider.setFakeSteps(true)
            provider.setFakeSteps(false)
            var selectedReal = false
            try {
                provider.access()
            } catch (error: IllegalStateException) {
                selectedReal = error.message == "Real reader selected"
            }
            assertTrue(selectedReal)
        }

    private fun provider(preferences: SharedPreferences): DebugStepsProvider =
        DebugStepsProvider(preferences, Provider { error("Real reader selected") }, FakeStepsProvider())

    private fun preferences(): SharedPreferences {
        val values = mutableMapOf<String, Boolean>()
        val editor =
            Proxy.newProxyInstance(
                SharedPreferences.Editor::class.java.classLoader,
                arrayOf(SharedPreferences.Editor::class.java),
            ) { proxy, method, args ->
                when (method.name) {
                    "putBoolean" -> {
                        values[args[0] as String] = args[1] as Boolean
                        proxy
                    }
                    "apply" -> null
                    else -> error("Unexpected editor call: ${method.name}")
                }
            } as SharedPreferences.Editor
        return Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, args ->
            when (method.name) {
                "getBoolean" -> values[args[0] as String] ?: args[1] as Boolean
                "edit" -> editor
                else -> error("Unexpected preferences call: ${method.name}")
            }
        } as SharedPreferences
    }
}
