# enhanced-function
This repo contains Java functional interfaces enhanced with more behaviours

- [Prerequisites](#Prerequisites)
- [Installation](#Installation)
- [Build library](#Build-library)
- [Launch tests](#Launch-tests)
- [Usage](#Usage)
  - [RetrySupplier](#RetrySupplier)
    - [Retry code that does not throw exceptions](#Retry-code-that-does-not-throw-exceptions)
  - [ConditionalConsumer](#ConditionalConsumer)

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

## Usage

### RetrySupplier

[RetrySupplier][retry-supplier] is a functional interface that has the same signature of the standard java [Supplier][java-supplier]
interface but has the ability to thrown exceptions - more formerly Throwables. In addiction this interface provides methods to retry itself
a defined number of times or for a certain amount of time before it throws an exception.
<br>
This interface is useful when performing operations that may thrown an exception and reiterate them until success.

**retry**

```java
int numRetry = 5;

try {
    ApiResponse result = RetrySupplier.builder(() -> api.getResponse()) // api.getResponse() returns an exception if the call hasn't completed
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
    ApiResponse result = RetrySupplier.builder(() -> api.getResponse()) // api.getResponse() returns an exception if the call hasn't completed
        .poll(time, ChronoUnit.SECONDS)
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

if the time unit is omitted then the default one will be ```MILLIS```

#### Retry code that does not throw exceptions

It may happen that the code we want to wrap into a function to reiterate until success doesn't throw any exception. In this 
case the code necessarily returns some value so we can create logic upon that

**return null value**

Retry until the result of the computation is not ```null```

> this example uses ```retry(numRetry)``` but it is also suitable ```poll(time, ChronoUnit.SECONDS)```

```java
int numRetry = 5;

try {
    String result = RetrySupplier.retryUntilNotNull(() -> stringProvider.get()) // stringProvider may return null but no exception
        .retry(numRetry)
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

**return boolean value**

Retry until the result of the computation is ```true```

> this example uses ```retry(numRetry)``` but it is also suitable ```poll(time, ChronoUnit.SECONDS)```

```java
int numRetry = 5;

try {
    boolean result = RetrySupplier.retryUntilTrue(() -> booleanProvider.get()) // booleanProvider may return false or true but no exception
        .retry(numRetry)
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

**return any value**

Retry until the result of the computation is the expected result

> this example uses ```retry(numRetry)``` but it is also suitable ```poll(time, ChronoUnit.SECONDS)```

```java
int numRetry = 5;

try {
    String result = RetrySupplier.retryUntilEqual(() -> stringProvider.get(), "Cat") // stringProvider returns random strings but no exception
        .retry(numRetry)
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

### ConditionalConsumer
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