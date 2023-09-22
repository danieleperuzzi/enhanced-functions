package com.danieleperuzzi.function;

@FunctionalInterface
public interface RetrySupplier<T> {
    T get() throws Throwable;

    default RetrySupplier<T> retry(int numRetry) throws Throwable {
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

    static <T> RetrySupplier<T> builder(RetrySupplier<? extends T> supplier) {
        return supplier::get;
    }
}
