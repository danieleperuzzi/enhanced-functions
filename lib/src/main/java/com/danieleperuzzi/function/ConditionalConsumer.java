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

package com.danieleperuzzi.function;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * This interface extends the standard Java <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/Consumer.html">Consumer</a>
 * functional interface and adds a method to perform the consumer only if a condition is met
 *
 * @param <T>   the parametrized type of this Consumer functional interface
 * @author      Daniele Peruzzi
 */
public interface ConditionalConsumer<T> extends Consumer<T> {

    /**
     * Creates a new Consumer that encapsulates the given one and executes it only if the input predicate is satisfied
     *
     * @param p     the predicate that determines if the given consumer is run or not
     * @return      a new Consumer that encapsulates the given one
     */
    default Consumer<T> acceptIf(Predicate<T> p) {
        Objects.requireNonNull(p);
        return (T t) -> {
            if (p.test(t)) {
                this.accept(t);
            }
        };
    }

    /**
     * Static helper method useful to create a ConditionalConsumer in more concise way.
     *
     * <pre>{@code
     * AtomicInteger counter = new AtomicInteger(0);
     * Consumer<AtomicInteger> increment = c -> c.incrementAndGet();
     *
     * ConditionalConsumer.builder(increment)
     *      .acceptIf(c -> c.get() == 1)  // check condition
     *      .accept(counter);
     *
     * int result = counter.get();  // result is 0 because it hasn't been incremented
     * }</pre>
     *
     * @param consumer  the lambda function that represent the computation
     * @return          a ConditionalConsumer instance
     * @param <T>       the parametrized type of this Consumer functional interface
     */
    static <T> ConditionalConsumer<T> builder(Consumer<? super T> consumer) {
        return consumer::accept;
    }
}
