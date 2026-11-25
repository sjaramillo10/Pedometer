package dev.sjaramillo.nemi.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class AppThemeTest {
    @Test
    fun `given stored theme values when decoded then known choices restore and other values default to system`() {
        // Given
        val values = listOf("system", "light", "dark", null, "unknown")

        // When
        val themes = values.map(AppTheme::fromPreference)

        // Then
        assertEquals(listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK, AppTheme.SYSTEM, AppTheme.SYSTEM), themes)
    }

    @Test
    fun `given explicit and system themes when Android theme changes then only system follows Android`() {
        // Given
        val themes = listOf(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK)

        // When
        val systemLight = themes.map { it.isDark(systemDark = false) }
        val systemDark = themes.map { it.isDark(systemDark = true) }

        // Then
        assertEquals(listOf(false, false, true), systemLight)
        assertEquals(listOf(true, false, true), systemDark)
    }
}
