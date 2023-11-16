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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.github.danieleperuzzi.function.util.Dummy;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

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
    @DisplayName("retry custom exception thrown")
    public void retryCustomExceptionThrown() throws Throwable {
        when(dummy.getInt()).thenThrow(new RuntimeException("failure"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt())
                .retry(1, () -> new RuntimeException("Custom Exception"));

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "Custom Exception";
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
    public void retryCheckNumRetries() throws Throwable {
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
    @DisplayName("retry async zero retry")
    public void retryAsyncZeroRetry() throws Throwable {
        RetrySupplier<String> rs = RetrySupplier.builder(() -> "cat");
        String result = rs.retryAsync(0).get();

        assertNull(result);
    }

    @Test
    @DisplayName("retry async greater than zero")
    public void notZeroRetryAsync() throws Throwable {
        RetrySupplier<String> rs = RetrySupplier.builder(() -> "cat");
        String result = rs.retryAsync(1).get();

        assertEquals("cat", result);
    }

    @Test
    @DisplayName("retry async exception thrown")
    public void retryAsyncExceptionThrown() throws Throwable {
        when(dummy.getInt()).thenThrow(new RuntimeException("failure"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).retryAsync(1);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("retry async custom exception thrown")
    public void retryAsyncCustomExceptionThrown() throws Throwable {
        when(dummy.getInt()).thenThrow(new RuntimeException("failure"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt())
                .retryAsync(1, () -> new RuntimeException("Custom Exception"));

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "Custom Exception";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("retry async multiple exceptions thrown")
    public void retryAsyncMultipleExceptionsThrown() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure1"))
                .thenThrow(new RuntimeException("failure2"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).retryAsync(2);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure2";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("retry async failure before success")
    public void retryAsyncFailureBeforeSuccess() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure1"))
                .thenThrow(new RuntimeException("failure2"))
                .thenReturn(1);

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).retryAsync(2);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure2";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("retry async success before failure")
    public void retryAsyncSuccessBeforeFailure() throws Throwable {
        when(dummy.getInt())
                .thenReturn(1)
                .thenThrow(new RuntimeException("failure"));


        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).retryAsync(2);
        Integer result = rs.get();

        assertEquals(1, result);
    }

    @Test
    @DisplayName("retry async success after failure")
    public void retryAsyncSuccessAfterFailure() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"))
                .thenReturn(1);

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).retryAsync(2);
        Integer result = rs.get();

        assertEquals(1, result);
    }

    @Test
    @DisplayName("retry async check num retries")
    public void retryAsyncCheckNumRetries() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"));

        AtomicInteger counter = new AtomicInteger(0);

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> {
            counter.incrementAndGet();

            return dummy.getInt();
        }).retryAsync(2);

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
    @DisplayName("poll custom exception thrown")
    public void pollCustomExceptionThrown() throws Throwable {
        when(dummy.getInt()).thenThrow(new RuntimeException("failure"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt())
                .poll(5, ChronoUnit.SECONDS, () -> new RuntimeException("Custom Exception"));

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "Custom Exception";
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

        assertTrue(duration.toMillis() >= 5000);
    }

    @Test
    @DisplayName("poll async zero retry")
    public void pollAsyncZeroRetry() throws Throwable {
        RetrySupplier<String> rs = RetrySupplier.builder(() -> "cat");
        String result = rs.pollAsync(0, ChronoUnit.SECONDS).get();

        assertNull(result);
    }

    @Test
    @DisplayName("poll async greater than zero")
    public void notZeroPollAsync() throws Throwable {
        RetrySupplier<String> rs = RetrySupplier.builder(() -> "cat");
        String result = rs.pollAsync(5, ChronoUnit.SECONDS).get();

        assertEquals("cat", result);
    }

    @Test
    @DisplayName("poll async exception thrown")
    public void pollAsyncExceptionThrown() throws Throwable {
        when(dummy.getInt()).thenThrow(new RuntimeException("failure"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).pollAsync(5, ChronoUnit.SECONDS);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("poll async custom exception thrown")
    public void pollAsyncCustomExceptionThrown() throws Throwable {
        when(dummy.getInt()).thenThrow(new RuntimeException("failure"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt())
                .pollAsync(5, ChronoUnit.SECONDS, () -> new RuntimeException("Custom Exception"));

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "Custom Exception";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("poll async exception thrown with time less than granularity")
    public void pollAsyncTimeLessThanGranularityExceptionThrown() throws Throwable {
        when(dummy.getInt()).thenThrow(new RuntimeException("failure"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).pollAsync(50, ChronoUnit.MILLIS);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("poll async multiple exceptions thrown")
    public void pollAsyncMultipleExceptionsThrown() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure1"))
                .thenThrow(new RuntimeException("failure2"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).pollAsync(5, ChronoUnit.SECONDS);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure2";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("poll async failure before success")
    public void pollAsyncFailureBeforeSuccess() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure1"))
                .thenReturn(1);

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).pollAsync(50, ChronoUnit.MILLIS);

        Exception retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "failure1";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("poll async success before failure")
    public void pollAsyncSuccessBeforeFailure() throws Throwable {
        when(dummy.getInt())
                .thenReturn(1)
                .thenThrow(new RuntimeException("failure"));


        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).pollAsync(5, ChronoUnit.SECONDS);
        Integer result = rs.get();

        assertEquals(1, result);
    }

    @Test
    @DisplayName("poll async success after failure")
    public void pollAsyncSuccessAfterFailure() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"))
                .thenReturn(1);

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).pollAsync(5, ChronoUnit.SECONDS);
        Integer result = rs.get();

        assertEquals(1, result);
    }

    @Test
    @DisplayName("poll async long running task failure")
    public void pollAsyncLongRunningTaskFailure() throws Throwable {
        Exception retryFailure;
        String expectedMessage;
        String actualMessage;

        Supplier<Integer> sleep = () -> {
            try {
                Thread.sleep(7000);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return 0;
        };

        // if the computation hasn't failed at least once and the computation takes long time
        // then TimeoutException is thrown
        RetrySupplier<Integer> rs1 = RetrySupplier.builder(sleep::get).pollAsync(5, ChronoUnit.SECONDS);

        retryFailure = assertThrows(TimeoutException.class, () -> {
            Integer result = rs1.get();
        });

        actualMessage = retryFailure.getMessage();

        assertNull(actualMessage);

        // if the computation hasn't failed at least once and the computation takes long time
        // but the user has specified a custom error then the custom error is thrown
        RetrySupplier<Integer> rs2 = RetrySupplier.builder(sleep::get).pollAsync(5, ChronoUnit.SECONDS, () -> new Exception("Custom error"));

        retryFailure = assertThrows(Exception.class, () -> {
            Integer result = rs2.get();
        });

        expectedMessage = "Custom error";
        actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);


        // if the computation has failed at least once then that error is thrown even
        // if the following times the computation is interrupted by TimeoutException
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"))
                .thenAnswer(invocation -> sleep.get());

        RetrySupplier<Integer> rs3 = RetrySupplier.builder(() -> dummy.getInt()).pollAsync(5, ChronoUnit.SECONDS);

        retryFailure = assertThrows(RuntimeException.class, () -> {
            Integer result = rs3.get();
        });

        expectedMessage = "failure";
        actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);

        // if the computation has failed at least once but the user has specified
        // a custom error then the custom error is thrown even if the following
        // times the computation is interrupted by TimeoutException
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"))
                .thenAnswer(invocation -> sleep.get());

        RetrySupplier<Integer> rs4 = RetrySupplier.builder(() -> dummy.getInt()).pollAsync(5, ChronoUnit.SECONDS, () -> new Exception("Custom error"));

        retryFailure = assertThrows(Exception.class, () -> {
            Integer result = rs4.get();
        });

        expectedMessage = "Custom error";
        actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("poll async check time elapsed")
    public void pollAsyncCheckTimeElapsed() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"));

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).pollAsync(5, ChronoUnit.SECONDS);

        Instant before = Instant.now();
        assertThrows(RuntimeException.class, rs::get);
        Instant after = Instant.now();

        Duration duration = Duration.between(before, after);

        assertTrue(duration.toMillis() >= 5000);
    }

    @Test
    @DisplayName("retry non throwing code until not null exception thrown")
    public void retryNonThrowingCodeUntilNotNullExceptionThrown() {
        when(dummy.getInt())
                .thenReturn(null);

        RetrySupplier<Integer> rs = RetrySupplier.retryUntilNotNull(() -> dummy.getInt())
                .retry(5);

        Exception retryFailure = assertThrows(Exception.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "expected data is null";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("retry non throwing code until not null")
    public void retryNonThrowingCodeUntilNotNull() throws Throwable {
        when(dummy.getInt())
                .thenReturn(null)
                .thenReturn(2);

        RetrySupplier<Integer> rs = RetrySupplier.retryUntilNotNull(() -> dummy.getInt())
                .retry(2);

        Integer result = rs.get();

        assertEquals(2, result);
    }

    @Test
    @DisplayName("retry non throwing code until true exception thrown")
    public void retryNonThrowingCodeUntilTrueExceptionThrown() {
        when(dummy.getBoolean())
                .thenReturn(false);

        RetrySupplier<Boolean> rs = RetrySupplier.retryUntilTrue(() -> dummy.getBoolean())
                .retry(5);

        Exception retryFailure = assertThrows(Exception.class, () -> {
            Boolean result = rs.get();
        });

        String expectedMessage = "expected data is false";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("retry non throwing code until true")
    public void retryNonThrowingCodeUntilTrue() throws Throwable {
        when(dummy.getBoolean())
                .thenReturn(false)
                .thenReturn(true);

        RetrySupplier<Boolean> rs = RetrySupplier.retryUntilTrue(() -> dummy.getBoolean())
                .retry(2);

        Boolean result = rs.get();

        assertTrue(result);
    }

    @Test
    @DisplayName("retry non throwing code until equal exception thrown")
    public void retryNonThrowingCodeUntilEqualExceptionThrown() {
        RetrySupplier<Integer> rs = RetrySupplier.retryUntilEqual(() -> dummy.getInt(), 2)
                .retry(5);

        Exception retryFailure = assertThrows(Exception.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "expected data and actual data are not equal";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }

    @Test
    @DisplayName("retry non throwing code until equal")
    public void retryNonThrowingCodeUntilEqual() throws Throwable {
        when(dummy.getString())
                .thenReturn("this")
                .thenReturn("is")
                .thenReturn("a")
                .thenReturn("test");

        RetrySupplier<String> rs = RetrySupplier.retryUntilEqual(() -> dummy.getString(), "test");

        String result = rs.retry(4)
                .get();

        assertEquals("test", result);
    }

    @Test
    @DisplayName("poll non throwing code until equal exception thrown")
    public void pollNonThrowingCodeUntilEqualExceptionThrown() {
        RetrySupplier<Integer> rs = RetrySupplier.retryUntilEqual(() -> dummy.getInt(), 2)
                .poll(5, ChronoUnit.SECONDS);

        Exception retryFailure = assertThrows(Exception.class, () -> {
            Integer result = rs.get();
        });

        String expectedMessage = "expected data and actual data are not equal";
        String actualMessage = retryFailure.getMessage();

        assertEquals(expectedMessage, actualMessage);
    }
}
