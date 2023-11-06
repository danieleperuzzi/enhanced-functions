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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.danieleperuzzi.function.util.Dummy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RetrySupplierTest {

    @Mock
    private Dummy dummy;

    @Test
    @DisplayName("retry zero retry")
    public void retryZeroRetry() throws Throwable {
        RetrySupplier<String> rs = RetrySupplier.builder(() -> "cat");
        String result = rs.retry(0).get();

        assertNull(result);
    }

    @Test
    @DisplayName("retry greater than zero")
    public void notZeroRetry() throws Throwable {
        RetrySupplier<String> rs = RetrySupplier.builder(() -> "cat");
        String result = rs.retry(1).get();

        assertEquals("cat", result);
    }

    @Test
    @DisplayName("retry exception thrown")
    public void retryExceptionThrown() throws Throwable {
        when(dummy.getInt()).thenThrow(new RuntimeException("failure"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).retry(1);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("retry multiple exceptions thrown")
    public void retryMultipleExceptionsThrown() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure1"))
                .thenThrow(new RuntimeException("failure2"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).retry(2);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure2";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("retry failure before success")
    public void retryFailureBeforeSuccess() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure1"))
                .thenThrow(new RuntimeException("failure2"))
                .thenReturn(1);

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).retry(2);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure2";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("retry success before failure")
    public void retrySuccessBeforeFailure() throws Throwable {
        when(dummy.getInt())
                .thenReturn(1)
                .thenThrow(new RuntimeException("failure"));


        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).retry(2);
        Integer result = rs.get();

        assertEquals(1, result);
    }

    @Test
    @DisplayName("retry success after failure")
    public void retrySuccessAfterFailure() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"))
                .thenReturn(1);

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).retry(2);
        Integer result = rs.get();

        assertEquals(1, result);
    }

    @Test
    @DisplayName("retry check num retries")
    public void retryCheckTimeElapsed() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"));

        AtomicInteger counter = new AtomicInteger(0);

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> {
            counter.incrementAndGet();

            return dummy.getInt();
        }).retry(2);

        assertThrows(RuntimeException.class, rs::get);

        assertEquals(2, counter.get());
    }

    @Test
    @DisplayName("poll zero retry")
    public void pollZeroRetry() throws Throwable {
        RetrySupplier<String> rs = RetrySupplier.builder(() -> "cat");
        String result = rs.poll(0, ChronoUnit.SECONDS).get();

        assertNull(result);
    }

    @Test
    @DisplayName("poll greater than zero")
    public void notZeroPoll() throws Throwable {
        RetrySupplier<String> rs = RetrySupplier.builder(() -> "cat");
        String result = rs.poll(5, ChronoUnit.SECONDS).get();

        assertEquals("cat", result);
    }

    @Test
    @DisplayName("poll exception thrown")
    public void pollExceptionThrown() throws Throwable {
        when(dummy.getInt()).thenThrow(new RuntimeException("failure"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).poll(5, ChronoUnit.SECONDS);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("poll exception thrown with time less than granularity")
    public void pollTimeLessThanGranularityExceptionThrown() throws Throwable {
        when(dummy.getInt()).thenThrow(new RuntimeException("failure"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).poll(50, ChronoUnit.MILLIS);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("poll multiple exceptions thrown")
    public void pollMultipleExceptionsThrown() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure1"))
                .thenThrow(new RuntimeException("failure2"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).poll(5, ChronoUnit.SECONDS);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure2";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("poll failure before success")
    public void pollFailureBeforeSuccess() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure1"))
                .thenReturn(1);

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).poll(50, ChronoUnit.MILLIS);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure1";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("poll success before failure")
    public void pollSuccessBeforeFailure() throws Throwable {
        when(dummy.getInt())
                .thenReturn(1)
                .thenThrow(new RuntimeException("failure"));


        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).poll(5, ChronoUnit.SECONDS);
        Integer result = rs.get();

        assertEquals(1, result);
    }

    @Test
    @DisplayName("poll success after failure")
    public void pollSuccessAfterFailure() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"))
                .thenReturn(1);

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).poll(5, ChronoUnit.SECONDS);
        Integer result = rs.get();

        assertEquals(1, result);
    }

    @Test
    @DisplayName("poll check time elapsed")
    public void pollCheckTimeElapsed() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).poll(5, ChronoUnit.SECONDS);

        Instant before = Instant.now();
        assertThrows(RuntimeException.class, rs::get);
        Instant after = Instant.now();

        Duration duration = Duration.between(before, after);

        assertTrue(duration.toSeconds() >= 5);
    }
}
