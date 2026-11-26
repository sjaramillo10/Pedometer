package dev.sjaramillo.nemi.ui;

import static org.junit.Assert.assertEquals;

import android.content.SharedPreferences;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class NemiPreferencesTest {
    @Test
    public void givenAStoredGoal_whenAdjustmentsArriveWithoutObserving_thenEachUsesAndPersistsTheLatestValue() {
        // Given
        var stored = new HashMap<String, Object>(Map.of("goal", 7_550, "theme", "dark"));
        var preferences = inMemoryPreferences(stored);
        var first = new NemiPreferences(preferences);

        // When
        first.adjustGoal(100);
        first.adjustGoal(100);
        first.adjustGoal(-100);
        var reopened = new NemiPreferences(preferences);

        // Then
        assertEquals(7_650, first.goal.value().intValue());
        assertEquals(7_650, reopened.goal.value().intValue());
        assertEquals(AppTheme.DARK, reopened.theme.value());
    }

    static SharedPreferences inMemoryPreferences(Map<String, Object> stored) {
        var editor = proxy(SharedPreferences.Editor.class, (instance, method, args) -> switch (method.getName()) {
            case "putInt", "putString" -> {
                stored.put((String) args[0], args[1]);
                yield instance;
            }
            case "apply" -> null;
            default -> throw new AssertionError("Unexpected editor method " + method.getName());
        });
        return proxy(SharedPreferences.class, (instance, method, args) -> switch (method.getName()) {
            case "getInt", "getString" -> stored.getOrDefault(args[0], args[1]);
            case "edit" -> editor;
            default -> throw new AssertionError("Unexpected preferences method " + method.getName());
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }
}
