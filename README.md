# enhanced-functions

This repo contains Java functional interfaces enhanced with more behaviours

- [Prerequisites](#Prerequisites)
- [Installation](#Installation)
- [Build library](#Build-library)
- [Launch tests](#Launch-tests)
- [Usage](#Usage)
  - [RetrySupplier](#RetrySupplier)
    - [Retry with custom exceptions thrown](#Retry-with-custom-exceptions-thrown)
    - [Retry in a separated thread](#Retry-in-a-separated-thread)
    - [Retry code that does not throw exceptions](#Retry-code-that-does-not-throw-exceptions)
  - [AsyncTask](#AsyncTask)
  - [ConditionalConsumer](#ConditionalConsumer)

## Prerequisites

- Java 8+
- Gradle 8+ (gradle wrapper included)

## Installation

Using Gradle

```
dependencies {
    implementation 'io.github.danieleperuzzi:enhanced-functions:1.3.0'
}
```

Using Maven

```xml
<dependency>
  <groupId>io.github.danieleperuzzi</groupId>
  <artifactId>enhanced-functions</artifactId>
  <version>1.3.0</version>
</dependency>
```

## Build library

To build enhanced-functions library just run gradle build task:

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

To launch the builtin test suite for enhanced-functions library just run gradle test task:

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
    // api.getResponse() returns an exception if the call hasn't completed
    ApiResponse result = RetrySupplier.builder(() -> api.getResponse())
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
    // api.getResponse() returns an exception if the call hasn't completed
    ApiResponse result = RetrySupplier.builder(() -> api.getResponse())
        .poll(time, ChronoUnit.SECONDS)
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

if the time unit is omitted then the default one will be ```MILLIS```

#### Retry with custom exceptions thrown

In case we need to customize the exception thrown on failure just pass a ```Supplier<? extends Throwable> exceptionSupplier``` 
to ```retry``` or ```poll``` methods

**retry**

```java
int numRetry = 5;

try {
    // api.getResponse() returns an exception if the call hasn't completed
    ApiResponse result = RetrySupplier.builder(() -> api.getResponse())
        .retry(numRetry, () -> new Exception("Custom Exception"))
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

**poll**

```java
long time = 5;

try {
    // api.getResponse() returns an exception if the call hasn't completed
    ApiResponse result = RetrySupplier.builder(() -> api.getResponse())
        .poll(time, ChronoUnit.SECONDS, () -> new Exception("Custom Exception"))
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

#### Retry in a separated thread

In case the computation to be performed is an heavy computation it is possible to execute it in a different thread. By default the thread is retrieved using [ForkJoinPool commonPool][ForkJoinPool-commonPool] but a custom [Executor][Executor]
can be given in input.
<br>
Also a custom error can be thrown to override the original error thrown by the computation.

The returned ```RetrySupplier```, by ```retryAsync``` or ```pollAsync``` methods, basically waits for the computation 
to complete in synchronous mode on the caller thread while the task is performed on a different one.
<br>
If the need is to get the result in asynchronous mode then [AsyncTask](#AsyncTask) may be the right choice.

For specific way to invoke async methods please refer to the [RetrySupplier][retry-supplier] class directly.

**retryAsync**

```java
int numRetry = 5;

try {
    // api.getResponse() returns an exception if the call hasn't completed
    ApiResponse result = RetrySupplier.builder(() -> api.getResponse())
        .retryAsync(numRetry)
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

**pollAsync**

Since poll async mechanism is based on time, it may happen that the computation lasts longer than the time the RetrySupplier
is instructed to wait. For this reason, if no custom exception is provided, then [TimeoutException][TimeoutException] 
is thrown

```java
long time = 5;

try {
    // api.getResponse() returns an exception if the call hasn't completed
    ApiResponse result = RetrySupplier.builder(() -> api.getResponse())
        .pollAsync(time, ChronoUnit.SECONDS)
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

#### Retry code that does not throw exceptions

It may happen that the code we want to wrap into a function to reiterate until success doesn't throw any exception. In this 
case the code necessarily returns some value so we can create logic upon that

**return null value**

Retry until the result of the computation is not ```null```

> this example uses ```retry(numRetry)``` but it is also suitable ```poll(time, ChronoUnit.SECONDS)```

```java
int numRetry = 5;

try {
    // stringProvider may return null but no exception
    String result = RetrySupplier.retryUntilNotNull(() -> stringProvider.get())
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
    // booleanProvider may return false or true but no exception
    boolean result = RetrySupplier.retryUntilTrue(() -> booleanProvider.get())
        .retry(numRetry)
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

**return any value**

> this examples use ```retry(numRetry)``` but it is also suitable ```poll(time, ChronoUnit.SECONDS)```

Retry until the result of the computation is the not-null expected result

```java
int numRetry = 5;

try {
    // stringProvider returns random strings but no exception
    String result = RetrySupplier.retryUntilEqual(() -> stringProvider.get(), "Cat")
        .retry(numRetry)
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

Retry until the result of the computation satisfies the not-null provided test

```java
int numRetry = 5;

try {
    // intProvider returns random int numbers but no exception
    Integer result = RetrySupplier.retryUntilTestOk(() -> intProvider.get(), result -> result > 10)
        .retry(numRetry)
        .get();
} catch (Throwable e) {
    e.printStackTrace();
}
```

### AsyncTask

[RetrySupplier][retry-supplier] interface always returns the computation result in a synchronous mode but we may want to get
the result posted in a callback in order to work with event-based code.
```AsyncTask``` does this job in a very simple way. By default the async computation, represented by a ```RetrySupplier```, 
is performed in a separated thread. That thread is retrieved using [ForkJoinPool commonPool][ForkJoinPool-commonPool] 
but a custom [Executor][Executor] can be given in input.

Because ```AsyncTask``` already executes ```RetrySupplier``` in a different thread it is preferable to use ```RetrySupplier``` 
synchronous methods, as ```retry``` or ```poll``` are, over their respective async methods when building complex ```RetrySupplier```.

Once the computation has completed, either in successful or failure case, the outcome is posted to the provided callback where 
it is possible to inspect the successful result or the exception.

```java
// api.getResponse() returns an exception if the call hasn't completed
RetrySupplier<ApiResponse> retrySupplier = RetrySupplier.builder(() -> api.getResponse())
    .retry(3);
     
AsyncTask.toAsync(retrySupplier, (result, throwable) -> {
    if (result != null) {
        // computation is successful
    } else {
        throwable.printStackTrace();
    }
});
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

[retry-supplier]: /lib/src/main/java/io/github/danieleperuzzi/function/RetrySupplier.java
[conditional-consumer]: /lib/src/main/java/io/github/danieleperuzzi/function/ConditionalConsumer.java
[java-supplier]: https://docs.oracle.com/javase/8/docs/api/java/util/function/Supplier.html
[java-consumer]: https://docs.oracle.com/javase/8/docs/api/java/util/function/Consumer.html
[ForkJoinPool-commonPool]: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinPool.html#commonPool--
[Executor]: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executor.html
[TimeoutException]: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/TimeoutException.html