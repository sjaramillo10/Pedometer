package dev.sjaramillo.nemi.ui;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;
import java.util.Arrays;

public class AppThemeTest {
    @Test
    public void givenStoredThemeValues_whenDecoded_thenKnownChoicesRestoreAndOtherValuesDefaultToSystem() {
        // Given
        var values = Arrays.asList("system", "light", "dark", null, "unknown");

        // When
        var themes = values.stream().map(AppTheme::fromPreference).toList();

        // Then
        assertEquals(List.of(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK, AppTheme.SYSTEM, AppTheme.SYSTEM), themes);
    }

    @Test
    public void givenExplicitAndSystemThemes_whenAndroidThemeChanges_thenOnlySystemFollowsAndroid() {
        // Given
        var themes = List.of(AppTheme.SYSTEM, AppTheme.LIGHT, AppTheme.DARK);

        // When
        var systemLight = themes.stream().map(theme -> theme.isDark(false)).toList();
        var systemDark = themes.stream().map(theme -> theme.isDark(true)).toList();

        // Then
        assertEquals(List.of(false, false, true), systemLight);
        assertEquals(List.of(true, false, true), systemDark);
    }
}
