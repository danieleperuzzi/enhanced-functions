/*
 * Copyright 2023 Daniele Peruzzi. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.danieleperuzzi.function;

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
