package com.danieleperuzzi.function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.danieleperuzzi.function.util.Dummy;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RetrySupplierTest {

    @Mock
    private Dummy dummy;

    @Test
    @DisplayName("zero retry")
    public void zeroRetry() throws Throwable {
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
    @DisplayName("exception thrown")
    public void exceptionThrown() throws Throwable {
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
    @DisplayName("multiple exceptions thrown")
    public void multipleExceptionsThrown() throws Throwable {
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
    @DisplayName("failure before success")
    public void failureBeforeSuccess() throws Throwable {
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
    @DisplayName("success before failure")
    public void successBeforeFailure() throws Throwable {
        when(dummy.getInt())
                .thenReturn(1)
                .thenThrow(new RuntimeException("failure"));


        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).retry(2);
        Integer result = rs.get();

        assertEquals(1, result);
    }

    @Test
    @DisplayName("success after failure")
    public void successAfterFailure() throws Throwable {
        when(dummy.getInt())
                .thenThrow(new RuntimeException("failure"))
                .thenReturn(1);

        RetrySupplier<Integer> rs = RetrySupplier.builder(() -> dummy.getInt()).retry(2);
        Integer result = rs.get();

        assertEquals(1, result);
    }
}
