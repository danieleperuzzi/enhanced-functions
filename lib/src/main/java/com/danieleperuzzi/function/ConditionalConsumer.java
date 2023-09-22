package com.danieleperuzzi.function;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface ConditionalConsumer<T> extends Consumer<T> {

    default Consumer<T> acceptIf(Predicate<T> p) {
        Objects.requireNonNull(p);
        return (T t) -> {
            if (p.test(t)) {
                this.accept(t);
            }
        };
    }

    static <T> ConditionalConsumer<T> builder(Consumer<? super T> consumer) {
        return consumer::accept;
    }
}
