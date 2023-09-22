# enhanced-function
This repo contains Java functional interfaces enhanced with more behaviours

## RetrySupplier
[RetrySupplier][retry-supplier] is a functional interface that has the same signature of the standard java [Supplier][java-supplier]
interface but has the ability to thrown exceptions - more formerly Throwables. In addiction this interface can retry itself
a defined number of times before it throws an exception

```java
int numRetry = 5;

try {
    String result = RetrySupplier.builder(() -> "Meow!")
        .retry(numRetry)
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

this interface is useful when performing operations that may thrown an exception and reiterate them until success.

## ConditionalConsumer
[ConditionalConsumer][conditional-consumer] is a functional interface that extends the standard java [Consumer][java-consumer]
interface and adds the ability to process the consumer only if specific condition is met otherwise it does nothing

**failure**
```java
AtomicInteger counter = new AtomicInteger(0);
Consumer<AtomicInteger> increment = c -> c.incrementAndGet();

ConditionalConsumer.builder(increment)
        .acceptIf(c -> c.get() == 1)  // check condition
        .accept(counter);

int result = counter.get();  // result is 0 because it hasn't been incremented
```

**success**
```java
AtomicInteger counter = new AtomicInteger(1);
Consumer<AtomicInteger> increment = c -> c.incrementAndGet();

ConditionalConsumer.builder(increment)
        .acceptIf(c -> c.get() == 1)  // check condition
        .accept(counter);

int result = counter.get();  // result is 2 because it has been incremented
```

this interface is useful when performing operations that may be not processed under certain circumstances.

[retry-supplier]: /lib/src/main/java/com/danieleperuzzi/function/RetrySupplier.java
[conditional-consumer]: /lib/src/main/java/com/danieleperuzzi/function/ConditionalConsumer.java
[java-supplier]: https://docs.oracle.com/javase/8/docs/api/java/util/function/Supplier.html
[java-consumer]: https://docs.oracle.com/javase/8/docs/api/java/util/function/Consumer.html