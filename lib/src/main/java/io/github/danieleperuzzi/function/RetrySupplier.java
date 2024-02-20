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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
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

    Logger logger = LoggerFactory.getLogger(RetrySupplier.class);

    /**
     * This is the method that acts like the standard Java <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/Supplier.html">Supplier</a>
     * functional interface. This method can throw an exception.
     *
     * @return              the result of the successful computation
     * @throws Throwable    the error of the computation as RetrySupplier handles cases in which code fails
     */
    T get() throws Throwable;

    /**
     * Creates a new RetrySupplier that iterates itself a predefined number of times before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier or a custom exception through exceptionSupplier.
     *
     * @param numRetry              the number of the retries until failure
     * @param exceptionSupplier     a supplier that returns a custom exception to throw in case of failure
     * @return                      a RetrySupplier that can retry itself until success or throw an exception on failure
     */
    default RetrySupplier<T> retry(int numRetry, Supplier<? extends Throwable> exceptionSupplier) {
        return () -> {
            Throwable error = null;

            for (int i = 0; i < numRetry; i++) {
                try {
                    return this.get();
                } catch (Throwable t) {
                    logger.error("unable to get the result of the computation");
                    error = t;
                }
            }

            if (error != null && exceptionSupplier == null) {
                throw error;
            }

            if (error != null) {
                throw exceptionSupplier.get();
            }

            return null;
        };
    }

    /**
     * Creates a new RetrySupplier that iterates itself a predefined number of times before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier.
     *
     * @param numRetry  the number of the retries until failure
     * @return          a RetrySupplier that can retry itself until success or throw an exception on failure
     */
    default RetrySupplier<T> retry(int numRetry) {
        return retry(numRetry, null);
    }

    /**
     * Creates a new RetrySupplier that iterates itself a predefined number of times before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier or a custom exception through exceptionSupplier.
     *
     * The computation is performed on the given executor in a separated thread.
     *
     * @param numRetry              the number of the retries until failure
     * @param executor              the executor to use for asynchronous execution
     * @param exceptionSupplier     a supplier that returns a custom exception to throw in case of failure
     * @return                      a RetrySupplier that can retry itself on a separated thread until success or throw
     *                              an exception on failure
     */
    default RetrySupplier<T> retryAsync(int numRetry, Executor executor, Supplier<? extends Throwable> exceptionSupplier) {
        return () -> {
            T result;
            AtomicReference<Throwable> errorReference = new AtomicReference<>();

            Supplier<T> futureSupplier = () -> {
                try {
                    return this.get();
                } catch (Throwable t) {
                    logger.error("unable to get the result of the computation");
                    errorReference.set(t);
                }

                return null;
            };

            for (int i = 0; i < numRetry; i++) {
                CompletableFuture<T> futureResult = executor != null ? CompletableFuture.supplyAsync(futureSupplier, executor) : CompletableFuture.supplyAsync(futureSupplier);

                result = futureResult.get();

                if (result != null) {
                    return result;
                }
            }

            Throwable error = errorReference.get();

            if (error != null && exceptionSupplier == null) {
                throw error;
            }

            if (error != null) {
                throw exceptionSupplier.get();
            }

            return null;
        };
    }

    /**
     * Creates a new RetrySupplier that iterates itself a predefined number of times before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier or a custom exception through exceptionSupplier.
     *
     * The computation is executed using {@link ForkJoinPool#commonPool()}
     *
     * @param numRetry              the number of the retries until failure
     * @param exceptionSupplier     a supplier that returns a custom exception to throw in case of failure
     * @return                      a RetrySupplier that can retry itself on a separated thread until success or throw
     *                              an exception on failure
     */
    default RetrySupplier<T> retryAsync(int numRetry, Supplier<? extends Throwable> exceptionSupplier) {
        return retryAsync(numRetry, null, exceptionSupplier);
    }

    /**
     * Creates a new RetrySupplier that iterates itself a predefined number of times before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier.
     *
     * The computation is executed using {@link ForkJoinPool#commonPool()}
     *
     * @param numRetry      the number of the retries until failure
     * @return              a RetrySupplier that can retry itself on a separated thread until success or throw
     *                      an exception on failure
     */
    default RetrySupplier<T> retryAsync(int numRetry) {
        return retryAsync(numRetry, null, null);
    }

    /**
     * Creates a new RetrySupplier that iterates itself for a certain amount of time before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier or a custom exception through exceptionSupplier.
     *
     * @param time                  the amount of time until failure
     * @param chronoUnit            the time unit
     * @param exceptionSupplier     a supplier that returns a custom exception to throw in case of failure
     * @return                      a RetrySupplier that can retry itself until success or throw an exception on failure
     */
    default RetrySupplier<T> poll(long time, ChronoUnit chronoUnit, Supplier<? extends Throwable> exceptionSupplier) {
        return () -> {
            Throwable error = null;
            Instant deadline = Instant.now().plus(time, chronoUnit);

            while (Instant.now().isBefore(deadline)) {
                try {
                    return this.get();
                } catch (Throwable t) {
                    logger.error("unable to get the result of the computation");
                    error = t;
                }

                Thread.sleep(500); // make sure iterations are performed not before 500 milliseconds each other
            }

            if (error != null && exceptionSupplier == null) {
                throw error;
            }

            if (error != null) {
                throw exceptionSupplier.get();
            }

            return null;
        };
    }

    /**
     * Creates a new RetrySupplier that iterates itself for a certain amount of time before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier.
     *
     * @param time          the amount of time until failure
     * @param chronoUnit    the time unit
     * @return              a RetrySupplier that can retry itself until success or throw an exception on failure
     */
    default RetrySupplier<T> poll(long time, ChronoUnit chronoUnit) {
        return poll(time, chronoUnit, null);
    }

    /**
     * Creates a new RetrySupplier that iterates itself for a certain amount of milliseconds before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier or a custom exception through exceptionSupplier.
     *
     * @param timeMillis            the amount of time in milliseconds until failure
     * @param exceptionSupplier     a supplier that returns a custom exception to throw in case of failure
     * @return                      a RetrySupplier that can retry itself until success or throw an exception on failure
     */
    default RetrySupplier<T> poll(long timeMillis, Supplier<? extends Throwable> exceptionSupplier) {
        return poll(timeMillis, ChronoUnit.MILLIS, exceptionSupplier);
    }

    /**
     * Creates a new RetrySupplier that iterates itself for a certain amount of milliseconds before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier.
     *
     * @param timeMillis    the amount of time in milliseconds until failure
     * @return              a RetrySupplier that can retry itself until success or throw an exception on failure
     */
    default RetrySupplier<T> poll(long timeMillis) {
        return poll(timeMillis, ChronoUnit.MILLIS, null);
    }

    /**
     * Creates a new RetrySupplier that iterates itself for a certain amount of time before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier only if the computation failed at least once, otherwise
     * that means the computation never ended in the given time so {@link java.util.concurrent.TimeoutException} is thrown. In case
     * exceptionSupplier is defined then a custom exception is thrown in any case.
     * This behaviour happens because the returned RetrySupplier can stop itself to wait for the computation to complete even if the
     * duration of the computation lasts more than the time the RetrySupplier is instructed to wait. For this reason,
     * if no exceptionSupplier is given in input and the computation never failed once, then {@link java.util.concurrent.TimeoutException} is thrown.
     *
     * The computation is performed on the given executor in a separated thread.
     *
     * @param time                  the amount of time until failure
     * @param chronoUnit            the time unit
     * @param executor              the executor to use for asynchronous execution
     * @param exceptionSupplier     a supplier that returns a custom exception to throw in case of failure
     * @return                      a RetrySupplier that can retry itself on a separated thread until success or throw
     *                              an exception on failure
     */
    default RetrySupplier<T> pollAsync(long time, ChronoUnit chronoUnit, Executor executor, Supplier<? extends Throwable> exceptionSupplier) {
        return () -> {
            T result = null;

            AtomicReference<Throwable> errorReference = new AtomicReference<>();
            Throwable error;
            Throwable timeoutException = null;

            Supplier<T> futureSupplier = () -> {
                try {
                    return this.get();
                } catch (Throwable t) {
                    logger.error("unable to get the result of the computation");
                    errorReference.set(t);
                }

                return null;
            };

            Instant deadline = Instant.now().plus(time, chronoUnit);

            while (Instant.now().isBefore(deadline)) {
                CompletableFuture<T> futureResult = executor != null ? CompletableFuture.supplyAsync(futureSupplier, executor) : CompletableFuture.supplyAsync(futureSupplier);

                try {
                    long timeMillisLeft = Duration.between(Instant.now(), deadline).toMillis();
                    result = futureResult.get(timeMillisLeft, TimeUnit.MILLISECONDS);
                } catch (Throwable t) {
                    timeoutException = t;
                }

                if (result != null) {
                    return result;
                }

                Thread.sleep(500); // make sure iterations are performed not before 500 milliseconds each other
            }

            error = errorReference.get();

            // throw the supplied exception regardless the real cause of the failure
            if (exceptionSupplier != null) {
                throw exceptionSupplier.get();
            }

            // throw the last failure exception
            if (error != null) {
                throw error;
            }

            // if no result was achieved and no error was thrown then the only failure
            // reason is that the supplied computation never ended at least once and
            // it was interrupted by the timer
            if (timeoutException != null) {
                throw timeoutException;
            }

            return null;
        };
    }

    /**
     * Creates a new RetrySupplier that iterates itself for a certain amount of time before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier only if the computation failed at least once, otherwise
     * that means the computation never ended in the given time so {@link java.util.concurrent.TimeoutException} is thrown. In case
     * exceptionSupplier is defined then a custom exception is thrown in any case.
     * This behaviour happens because the returned RetrySupplier can stop itself to wait for the computation to complete even if the
     * duration of the computation lasts more than the time the RetrySupplier is instructed to wait. For this reason,
     * if no exceptionSupplier is given in input and the computation never failed once, then {@link java.util.concurrent.TimeoutException} is thrown.
     *
     * The computation is executed using {@link ForkJoinPool#commonPool()}
     *
     * @param time                  the amount of time until failure
     * @param chronoUnit            the time unit
     * @param exceptionSupplier     a supplier that returns a custom exception to throw in case of failure
     * @return                      a RetrySupplier that can retry itself on a separated thread until success or throw
     *                              an exception on failure
     */
    default RetrySupplier<T> pollAsync(long time, ChronoUnit chronoUnit, Supplier<? extends Throwable> exceptionSupplier) {
        return pollAsync(time, chronoUnit, null, exceptionSupplier);
    }

    /**
     * Creates a new RetrySupplier that iterates itself for a certain amount of time before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier only if the computation failed at least once, otherwise
     * that means the computation never ended in the given time so {@link java.util.concurrent.TimeoutException} is thrown.
     * This behaviour happens because the returned RetrySupplier can stop itself to wait for the computation to complete even if the
     * duration of the computation lasts more than the time the RetrySupplier is instructed to wait. For this reason,
     * if the computation never failed once, then {@link java.util.concurrent.TimeoutException} is thrown.
     *
     * The computation is executed using {@link ForkJoinPool#commonPool()}
     *
     * @param time                  the amount of time until failure
     * @param chronoUnit            the time unit
     * @return                      a RetrySupplier that can retry itself on a separated thread until success or throw
     *                              an exception on failure
     */
    default RetrySupplier<T> pollAsync(long time, ChronoUnit chronoUnit) {
        return pollAsync(time, chronoUnit, null, null);
    }

    /**
     * Creates a new RetrySupplier that iterates itself for a certain amount of milliseconds before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier only if the computation failed at least once, otherwise
     * that means the computation never ended in the given time so {@link java.util.concurrent.TimeoutException} is thrown. In case
     * exceptionSupplier is defined then a custom exception is thrown in any case.
     * This behaviour happens because the returned RetrySupplier can stop itself to wait for the computation to complete even if the
     * duration of the computation lasts more than the time the RetrySupplier is instructed to wait. For this reason,
     * if no exceptionSupplier is given in input and the computation never failed once, then {@link java.util.concurrent.TimeoutException} is thrown.
     *
     * The computation is executed using {@link ForkJoinPool#commonPool()}
     *
     * @param timeMillis            the amount of time in milliseconds until failure
     * @param exceptionSupplier     a supplier that returns a custom exception to throw in case of failure
     * @return                      a RetrySupplier that can retry itself on a separated thread until success or throw
     *                              an exception on failure
     */
    default RetrySupplier<T> pollAsync(long timeMillis, Supplier<? extends Throwable> exceptionSupplier) {
        return pollAsync(timeMillis, ChronoUnit.MILLIS, null, exceptionSupplier);
    }

    /**
     * Creates a new RetrySupplier that iterates itself for a certain amount of milliseconds before throwing an Exception. On success
     * it returns the result of the computation.
     * On failure throws the last exception thrown by the RetrySupplier only if the computation failed at least once, otherwise
     * that means the computation never ended in the given time so {@link java.util.concurrent.TimeoutException} is thrown.
     * This behaviour happens because the returned RetrySupplier can stop itself to wait for the computation to complete even if the
     * duration of the computation lasts more than the time the RetrySupplier is instructed to wait. For this reason,
     * if the computation never failed once, then {@link java.util.concurrent.TimeoutException} is thrown.
     *
     * The computation is executed using {@link ForkJoinPool#commonPool()}
     *
     * @param timeMillis            the amount of time in milliseconds until failure
     * @return                      a RetrySupplier that can retry itself on a separated thread until success or throw
     *                              an exception on failure
     */
    default RetrySupplier<T> pollAsync(long timeMillis) {
        return pollAsync(timeMillis, ChronoUnit.MILLIS, null, null);
    }

    /**
     * Static helper method useful to create a RetrySupplier in more concise way.
     *
     * <pre>{@code
     * // api.getResponse() returns an exception if the call hasn't completed
     * ApiResponse result = RetrySupplier.builder(() -> api.getResponse())
     *      .retry(3)
     *      .get();
     *
     * // api.getResponse() returns an exception if the call hasn't completed
     * ApiResponse result = RetrySupplier.builder(() -> api.getResponse())
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
     * <pre>{@code
     * // stringProvider may return null but no exception
     * String result = RetrySupplier.retryUntilNotNull(() -> stringProvider.get())
     *      .retry(5)
     *      .get();
     *
     * // stringProvider may return null but no exception
     * String result = RetrySupplier.retryUntilNotNull(() -> stringProvider.get())
     *      .poll(5, ChronoUnit.SECONDS)
     *      .get();
     * }</pre>
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
     * it simply returns false. When the execution succeeded it then returns true
     *
     * <pre>{@code
     * // booleanProvider may return false or true but no exception
     * boolean result = RetrySupplier.retryUntilTrue(() -> booleanProvider.get())
     *      .retry(5)
     *      .get();
     *
     * // booleanProvider may return false or true but no exception
     * boolean result = RetrySupplier.retryUntilTrue(() -> booleanProvider.get())
     *      .poll(5, ChronoUnit.SECONDS)
     *      .get();
     * }</pre>
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
     * <pre>{@code
     * // stringProvider returns random strings but no exception
     * String result = RetrySupplier.retryUntilEqual(() -> stringProvider.get(), "Cat")
     *      .retry(5)
     *      .get();
     *
     * // stringProvider returns random strings but no exception
     * String result = RetrySupplier.retryUntilEqual(() -> stringProvider.get(), "Cat")
     *      .poll(5, ChronoUnit.SECONDS)
     *      .get();
     * }</pre>
     *
     * @param supplier          the lambda function that represent the computation
     * @param expectedResult    the not null expected result that the computation should return
     * @return                  a RetrySupplier instance
     * @param <T>               the parametrized type of this RetrySupplier functional interface
     */
    static <T> RetrySupplier<T> retryUntilEqual(Supplier<? extends T> supplier, T expectedResult) {
        return () -> {
            Objects.requireNonNull(expectedResult, "expected result is null");

            T result = supplier.get();

            if (result == null || !result.equals(expectedResult)) {
                throw new Exception("expected data and actual data are not equal");
            }

            return result;
        };
    }

    /**
     * Static helper method useful to create a RetrySupplier starting from a code that doesn't throw exception on failure but
     * it simply returns values. The goal is to apply a specific test to the value that is returned
     *
     * <pre>{@code
     * // intProvider returns random int numbers but no exception
     * Integer result = RetrySupplier.retryUntilTestOk(() -> intProvider.get(), result -> result > 10)
     *      .retry(5)
     *      .get();
     *
     * // intProvider returns random int numbers but no exception
     * Integer result = RetrySupplier.retryUntilTestOk(() -> intProvider.get(), result -> result > 10)
     *      .poll(5, ChronoUnit.SECONDS)
     *      .get();
     * }</pre>
     *
     * @param supplier          the lambda function that represent the computation
     * @param test              the not null {@link Predicate} to be satisfied by the result
     *                          that the computation should return
     * @return                  a RetrySupplier instance
     * @param <T>               the parametrized type of this RetrySupplier functional interface
     */
    static <T> RetrySupplier<T> retryUntilTestOk(Supplier<? extends T> supplier, Predicate<? super T> test) {
        return () -> {
            Objects.requireNonNull(test, "test is null");

            T result = supplier.get();

            if (!test.test(result)) {
                throw new Exception("test is not satisfied");
            }

            return result;
        };
    }
}
