# CHANGELOG

## 1.2.0

#### Added:

- ```retryAsync``` and ```pollAsync``` in ```RetrySupplier``` to perform the computation in a separate thread
- ```AsyncTask``` utility class to get RetrySupplier result in async mode

#### Added tests:

- ```RetrySupplier```: test for ```retryAsync``` and ```pollAsync```
- ```AsyncTask```


## 1.1.0

#### Added:

- ```RetrySupplier```: now ```retry``` and ```poll``` methods can throw custom exceptions

#### Added tests:

- ```RetrySupplier```: test to handle custom message throwing


## 1.0.0

#### Added:

- ```ConditionalConsumer``` interface that extends the default Java Consumer functional interface with more behaviours
- ```RetrySupplier``` interface that executes more times a specific code in order to get a result

#### Added tests:

- ```ConditionalConsumer```
- ```RetrySupplier```
