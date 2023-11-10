# CHANGELOG

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
