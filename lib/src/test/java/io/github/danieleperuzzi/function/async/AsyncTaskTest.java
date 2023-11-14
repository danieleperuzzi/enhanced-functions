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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.github.danieleperuzzi.function.util.Dummy;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AsyncTaskTest {

    @Mock
    private Dummy dummy;

    @Test
    @DisplayName("to async success")
    public void toAsyncSuccess() throws Exception {
        when(dummy.getInt())
                .thenReturn(1);

        AtomicInteger atomicResult = new AtomicInteger();
        AtomicReference<Throwable> atomicThrowable = new AtomicReference<>();


        AsyncTask.toAsync(() -> dummy.getInt(), (result, throwable) -> {
            atomicResult.set(result);
            atomicThrowable.set(throwable);
        });

        Thread.sleep(3000);

        assertEquals(1, atomicResult.get());
        assertNull(atomicThrowable.get());
    }

    @Test
    @DisplayName("to async failure")
    public void toAsyncFailure() throws Exception {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"));

        AtomicReference<Integer> atomicResult = new AtomicReference<>();
        AtomicReference<Throwable> atomicThrowable = new AtomicReference<>();


        AsyncTask.toAsync(() -> dummy.getInt(), (result, throwable) -> {
            atomicResult.set(result);
            atomicThrowable.set(throwable);
        });

        Thread.sleep(3000);

        assertNull(atomicResult.get());
        assertEquals("failure", atomicThrowable.get().getMessage());
    }

    @Test
    @DisplayName("to async retry success after failure")
    public void toAsyncRetrySuccessAfterFailure() throws Exception {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"))
                .thenThrow(new RuntimeException("failure"))
                .thenReturn(1);

        AtomicInteger atomicResult = new AtomicInteger();
        AtomicReference<Throwable> atomicThrowable = new AtomicReference<>();


        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt())
                .retry(3);

        AsyncTask.toAsync(rs, (result, throwable) -> {
            atomicResult.set(result);
            atomicThrowable.set(throwable);
        });

        Thread.sleep(3000);

        assertEquals(1, atomicResult.get());
        assertNull(atomicThrowable.get());
    }

    @Test
    @DisplayName("to async poll success after failure")
    public void toAsyncPollSuccessAfterFailure() throws Exception {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"))
                .thenThrow(new RuntimeException("failure"))
                .thenReturn(1);

        AtomicInteger atomicResult = new AtomicInteger();
        AtomicReference<Throwable> atomicThrowable = new AtomicReference<>();


        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt())
                .poll(3, ChronoUnit.SECONDS);

        AsyncTask.toAsync(rs, (result, throwable) -> {
            atomicResult.set(result);
            atomicThrowable.set(throwable);
        });

        Thread.sleep(3000);

        assertEquals(1, atomicResult.get());
        assertNull(atomicThrowable.get());
    }
}
