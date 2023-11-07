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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * This interface is similar to the standard Java <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/Supplier.html">Supplier</a>
 * functional interface but has the ability to throw an Exception while computing as this may happen.
 * In addiction this interface has default methods that help in retrying the computing n-times or for a certain amount of time until success.
 *
 * @param <T>   the parametrized type of this RetrySupplier functional interface
 * @author      Daniele Peruzzi
 */
@FunctionalInterface
public interface RetrySupplier<T> {

    /**
     * This is the method that acts like the standard Java <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/Supplier.html">Supplier</a>
     * functional interface. This method can throw exception.
     *
     * @return              the result of the successful computation
     * @throws Throwable    the error of the computation as RetrySupplier handles cases in which code fails
     */
    T get() throws Throwable;

    /**
     * Creates a new RetrySupplier that iterates itself a predefined number of times before throwing an Exception. On success
     * it returns the result of the computation.
     *
     * @param numRetry  the number of the retries until failure
     * @return          a RetrySupplier that can retry itself until success or throw an exception on failure
     */
    default RetrySupplier<T> retry(int numRetry) {
        return () -> {
            Throwable error = null;

            for (int i = 0; i < numRetry; i++) {
                try {
                    return this.get();
                } catch (Throwable t) {
                    t.printStackTrace();
                    error = t;
                }
            }

            if (error != null) {
                throw error;
            }

            return null;
        };
    }

    /**
     * Creates a new RetrySupplier that iterates itself for a certain amount of time before throwing an Exception. On success
     * it returns the result of the computation.
     *
     * @param time          the amount of time until failure
     * @param chronoUnit    the time unit
     * @return              a RetrySupplier that can retry itself until success or throw an exception on failure
     */
    default RetrySupplier<T> poll(long time, ChronoUnit chronoUnit) {
        return () -> {
            Throwable error = null;
            Instant deadline = Instant.now().plus(time, chronoUnit);

            while (Instant.now().isBefore(deadline)){
                try {
                    return this.get();
                } catch (Throwable t) {
                    t.printStackTrace();
                    error = t;
                }

                Thread.sleep(500); // make sure iterations are performed not before 500 milliseconds each other
            }

            if (error != null) {
                throw error;
            }

            return null;
        };
    }

    /**
     * Creates a new RetrySupplier that iterates itself for a certain amount of milliseconds before throwing an Exception. On success
     * it returns the result of the computation.
     *
     * @param timeMillis    the amount of time in milliseconds until failure
     * @return              a RetrySupplier that can retry itself until success or throw an exception on failure
     */
    default RetrySupplier<T> poll(long timeMillis) {
        return poll(timeMillis, ChronoUnit.MILLIS);
    }

    /**
     * Static helper method useful to create a RetrySupplier in more concise way.
     *
     * <pre>{@code
     * String result = RetrySupplier.builder(() -> "Meow!")
     *      .retry(3)
     *      .get();
     *
     * String result = RetrySupplier.builder(() -> "Meow!")
     *      .poll(5, ChronoUnit.SECONDS)
     *      .get();
     * }</pre>
     *
     * @param supplier  the lambda function that represent the computation
     * @return          a RetrySupplier instance
     * @param <T>       the parametrized type of this RetrySupplier functional interface
     */
    static <T> RetrySupplier<T> builder(RetrySupplier<? extends T> supplier) {
        return supplier::get;
    }

    /**
     * Static helper method useful to create a RetrySupplier starting from a code that doesn't throw exception on failure but
     * it returns null until the correct value is returned
     *
     * @param supplier  the lambda function that represent the computation
     * @return          a RetrySupplier instance
     * @param <T>       the parametrized type of this RetrySupplier functional interface
     */
    static <T> RetrySupplier<T> retryUntilNotNull(Supplier<? extends T> supplier) {
        return () -> {
            T result = supplier.get();

            if (Objects.isNull(result)) {
                throw new Exception("expected data is null");
            }

            return result;
        };
    }

    /**
     * Static helper method useful to create a RetrySupplier starting from a code that doesn't throw exception on failure but
     * it returns false until it then returns true on success
     *
     * @param supplier  the lambda function that represent the computation
     * @return          a RetrySupplier instance
     */
    static RetrySupplier<Boolean> retryUntilTrue(Supplier<Boolean> supplier) {
        return () -> {
            boolean result = supplier.get();

            if (!result) {
                throw new Exception("expected data is false");
            }

            return result;
        };
    }

    /**
     * Static helper method useful to create a RetrySupplier starting from a code that doesn't throw exception on failure but
     * it simply returns values. The goal is to test if a specific value is returned
     *
     * @param supplier          the lambda function that represent the computation
     * @param expectedResult    the expected result that the computation should return
     * @return                  a RetrySupplier instance
     * @param <T>               the parametrized type of this RetrySupplier functional interface
     */
    static <T> RetrySupplier<T> retryUntilEqual(Supplier<? extends T> supplier, T expectedResult) {
        return () -> {
            T result = supplier.get();

            if (!result.equals(expectedResult)) {
                throw new Exception("expected data and actual data are not equal");
            }

            return result;
        };
    }
}
