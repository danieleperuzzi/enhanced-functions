package com.danieleperuzzi.function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class ConditionalConsumerTest {

    @Test
    @DisplayName("predicate failure")
    public void predicateFailure() {
        AtomicInteger counter = new AtomicInteger(0);
        Consumer<AtomicInteger> increment = c -> c.incrementAndGet();

        ConditionalConsumer.builder(increment)
                .acceptIf(c -> c.get() == 1)
                .accept(counter);

        assertEquals(0, counter.get());
    }

    @Test
    @DisplayName("predicate successful")
    public void predicateSuccessful() {
        AtomicInteger counter = new AtomicInteger(1);
        Consumer<AtomicInteger> increment = c -> c.incrementAndGet();

        ConditionalConsumer.builder(increment)
                .acceptIf(c -> c.get() == 1)
                .accept(counter);

        assertEquals(2, counter.get());
    }
}
