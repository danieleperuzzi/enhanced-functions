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

package io.github.danieleperuzzi.function.async;

import io.github.danieleperuzzi.function.RetrySupplier;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * Simple utility class that performs operations on a separate thread and posts the result to a specific callback interface
 * in async way in order to work well with async code.
 *
 * @author      Daniele Peruzzi
 */
public class AsyncTask {

    /**
     * Simple method used to work with {@link RetrySupplier}. On success or failure computation the result is posted to the given
     * input callback.
     * <br>
     * In conjunction with {@link RetrySupplier} it is possible to create a {@link RetrySupplier} that iterates itself several times
     * to try to get the result and then, through this method, post the result in async mode to the caller.
     *
     * <pre>{@code
     * RetrySupplier<ApiResponse> retrySupplier = RetrySupplier.builder(() -> api.getResponse()) // api.getResponse() returns an exception if the call hasn't completed
     *      .retry(3);
     *
     * AsyncTask.toAsync(retrySupplier, (result, throwable) -> {
     *             if (result != null) {
     *                  // computation is successful
     *             } else {
     *                  throwable.printStackTrace();
     *             }
     *         });
     * }</pre>
     *
     * As by design the given {@link RetrySupplier} is executed in a separate thread then it is preferable to use {@link RetrySupplier#retry(int, Supplier)}
     * or {@link RetrySupplier#poll(long, ChronoUnit, Supplier)} over their respective async methods when building complex {@link RetrySupplier}.
     *
     * If a specific executor would be used than it is possible to supply it as input.
     *
     * @param supplier      the {@link RetrySupplier} that represent the async computation
     * @param executor      the executor to use for asynchronous execution
     * @param callback      the callback used to handle the success or failure computation result
     * @param <T>           the object type of the computation result
     */
    static <T> void toAsync(RetrySupplier<? extends T> supplier, Executor executor, Callback<T> callback) {
        Runnable task = () -> {
            T result = null;

            try {
                result = supplier.get();
            } catch (Throwable t) {
                callback.post(null, t);
            }

            if (result != null) {
                callback.post(result, null);
            }
        };

        if (executor != null) {
            CompletableFuture.runAsync(task, executor);
        } else {
            CompletableFuture.runAsync(task);
        }
    }

    /**
     * Simple method used to work with {@link RetrySupplier}. On success or failure computation the result is posted to the given
     * input callback.
     * <br>
     * In conjunction with {@link RetrySupplier} it is possible to create a {@link RetrySupplier} that iterates itself several times
     * to try to get the result and then, through this method, post the result in async mode to the caller.
     *
     * <pre>{@code
     * RetrySupplier<ApiResponse> retrySupplier = RetrySupplier.builder(() -> api.getResponse()) // api.getResponse() returns an exception if the call hasn't completed
     *      .retry(3);
     *
     * AsyncTask.toAsync(retrySupplier, (result, throwable) -> {
     *             if (result != null) {
     *                  // computation is successful
     *             } else {
     *                  throwable.printStackTrace();
     *             }
     *         });
     * }</pre>
     *
     * As by design the given {@link RetrySupplier} is executed in a separate thread then it is preferable to use {@link RetrySupplier#retry(int, Supplier)}
     * or {@link RetrySupplier#poll(long, ChronoUnit, Supplier)} over their respective async methods when building complex {@link RetrySupplier}.
     *
     * The task is executed using {@link ForkJoinPool#commonPool()}.
     *
     * @param supplier      the {@link RetrySupplier} that represent the async computation
     * @param callback      the callback used to handle the success or failure computation result
     * @param <T>           the object type of the computation result
     */
    static <T> void toAsync(RetrySupplier<? extends T> supplier, Callback<T> callback) {
        toAsync(supplier, null, callback);
    }

    /**
     * The interface that represents the callback to the AsyncTask.
     *
     * @param <T>   the object type of the computation result
     * @author      Daniele Peruzzi
     */
    interface Callback<T> {

        /**
         * The method used to post the AsyncTask computation result to the caller.
         *
         * @param result        the result of the computation, null if an error was thrown
         * @param throwable     the error thrown during the computation, null if the computation succeeded
         */
        void post(T result, Throwable throwable);
    }
}
