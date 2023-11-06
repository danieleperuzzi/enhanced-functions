# enhanced-function
This repo contains Java functional interfaces enhanced with more behaviours

## Prerequisites
- Java 17+
- Gradle 8+ (gradle wrapper included)

## Installation
Using Gradle

```
dependencies {
    implementation 'com.danieleperuzzi:enhanced-function:1.0.0'
}
```

## Build library
To build enhanced-function library just run gradle build task:

on Linux
```
./gradlew build
```

on Windows
```
./gradlew.bat build
```

you can also build the library using your machine gradle installation but please be sure gradle version is at least 8.

## Launch tests
To launch the builtin test suite for enhanced-function library just run gradle test task:

on Linux
```
./gradlew test
```

on Windows
```
./gradlew.bat test
```

you can also test the library using your machine gradle installation but please be sure gradle version is at least 8.

## RetrySupplier
[RetrySupplier][retry-supplier] is a functional interface that has the same signature of the standard java [Supplier][java-supplier]
interface but has the ability to thrown exceptions - more formerly Throwables. In addiction this interface provides methods to retry itself
a defined number of times or for a certain amount of time before it throws an exception.
<br>
This interface is useful when performing operations that may thrown an exception and reiterate them until success.

**retry**

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

**poll**

```java
long time = 5;

try {
    String result = RetrySupplier.builder(() -> "Meow!")
        .poll(time, ChronoUnit.SECONDS)
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

if the time unit is omitted then the default one will be ```MILLIS```

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