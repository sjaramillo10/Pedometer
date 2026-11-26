package dev.sjaramillo.nemi.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class HeaderScrollTest {
    @Test
    public void givenACardPartlyBehindTheHeader_whenRevealingIt_thenItsTopAlignsBelowTheHeader() {
        // Given
        var viewportTop = 240;
        var viewportBottom = 900;

        // When
        var delta = ScreenLayout.scrollDeltaForPinnedHeader(190, 550, viewportTop, viewportBottom);

        // Then
        assertEquals(-50, delta);
    }

    @Test
    public void givenACardBelowTheViewport_whenRevealingIt_thenItsBottomFitsOnScreen() {
        // Given
        var viewportTop = 240;
        var viewportBottom = 900;

        // When
        var delta = ScreenLayout.scrollDeltaForPinnedHeader(950, 1250, viewportTop, viewportBottom);

        // Then
        assertEquals(350, delta);
    }

    @Test
    public void givenACardTallerThanTheViewport_whenRevealingItFromAbove_thenItsTopFitsBelowTheHeader() {
        // Given
        var viewportTop = 240;
        var viewportBottom = 900;

        // When
        var delta = ScreenLayout.scrollDeltaForPinnedHeader(420, 1250, viewportTop, viewportBottom);

        // Then
        assertEquals(180, delta);
    }
}
