package dev.sjaramillo.nemi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/** Observable state owned by the main thread. */
public final class State<T> {
    private final Set<Consumer<T>> observers = new LinkedHashSet<>();
    private T value;

    public State(T initial) {
        value = initial;
    }

    public T value() {
        return value;
    }

    public void set(T next) {
        if (Objects.equals(value, next)) {
            return;
        }
        value = next;
        for (var observer : new ArrayList<>(observers)) {
            if (observers.contains(observer)) {
                observer.accept(next);
            }
        }
    }

    public Runnable observe(Consumer<T> observer) {
        observers.add(observer);
        observer.accept(value);
        return () -> observers.remove(observer);
    }
}
