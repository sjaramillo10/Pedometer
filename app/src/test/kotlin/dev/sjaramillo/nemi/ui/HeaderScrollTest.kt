package dev.sjaramillo.nemi.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HeaderScrollTest {
    @Test
    fun `given a card partly behind the header when revealing it then its top aligns below the header`() {
        // Given
        val viewportTop = 240
        val viewportBottom = 900

        // When
        val delta = scrollDeltaForPinnedHeader(190, 550, viewportTop, viewportBottom)

        // Then
        assertEquals(-50, delta)
    }

    @Test
    fun `given a card below the viewport when revealing it then its bottom fits on screen`() {
        // Given
        val viewportTop = 240
        val viewportBottom = 900

        // When
        val delta = scrollDeltaForPinnedHeader(950, 1250, viewportTop, viewportBottom)

        // Then
        assertEquals(350, delta)
    }

    @Test
    fun `given a card taller than the viewport when revealing it from above then its top fits below the header`() {
        // Given
        val viewportTop = 240
        val viewportBottom = 900

        // When
        val delta = scrollDeltaForPinnedHeader(420, 1250, viewportTop, viewportBottom)

        // Then
        assertEquals(180, delta)
    }
}
