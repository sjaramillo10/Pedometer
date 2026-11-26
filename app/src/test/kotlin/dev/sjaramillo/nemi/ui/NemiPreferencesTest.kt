package dev.sjaramillo.nemi.ui

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy

class NemiPreferencesTest {
    @Test
    fun `given a stored goal when adjustments arrive without observing then each uses and persists the latest value`() {
        // Given
        val stored = mutableMapOf<String, Any>("goal" to 7_550, "theme" to "dark")
        val preferences = inMemoryPreferences(stored)
        val first = NemiPreferences(preferences)

        // When
        first.adjustGoal(100)
        first.adjustGoal(100)
        first.adjustGoal(-100)
        val reopened = NemiPreferences(preferences)

        // Then
        assertEquals(7_650, first.goal.value)
        assertEquals(7_650, reopened.goal.value)
        assertEquals(AppTheme.DARK, reopened.theme.value)
    }

    private fun inMemoryPreferences(stored: MutableMap<String, Any>): SharedPreferences {
        val editor =
            proxy<SharedPreferences.Editor> { instance, method, args ->
                when (method.name) {
                    "putInt" -> {
                        stored[args[0] as String] = args[1] as Int
                        instance
                    }
                    "apply" -> null
                    else -> error("Unexpected editor method ${method.name}")
                }
            }
        return proxy<SharedPreferences> { _, method, args ->
            when (method.name) {
                "getInt", "getString" -> stored[args[0] as String] ?: args[1]
                "edit" -> editor
                else -> error("Unexpected preferences method ${method.name}")
            }
        }
    }

    private inline fun <reified T> proxy(handler: InvocationHandler): T =
        T::class.java.cast(Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java), handler))
}
