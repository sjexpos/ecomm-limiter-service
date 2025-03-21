
# Limiter service

[![GitHub release](https://img.shields.io/github/release/sjexpos/ecomm-limiter-service.svg?style=plastic)](https://github.com/sjexpos/ecomm-limiter-service/releases/latest)
[![CI workflow](https://img.shields.io/github/actions/workflow/status/sjexpos/ecomm-limiter-service/ci.yaml?branch=main&label=ci&logo=github&style=plastic)](https://github.com/sjexpos/ecomm-limiter-service/actions?workflow=CI)
[![Codecov](https://img.shields.io/codecov/c/github/sjexpos/ecomm-limiter-service?logo=codecov&style=plastic)](https://codecov.io/gh/sjexpos/ecomm-limiter-service)
[![Issues](https://img.shields.io/github/issues-search/sjexpos/ecomm-limiter-service?query=is%3Aopen&label=issues&style=plastic)](https://github.com/sjexpos/ecomm-limiter-service/issues)
[![Commits](https://img.shields.io/github/last-commit/sjexpos/ecomm-limiter-service?logo=github&style=plastic)](https://github.com/sjexpos/ecomm-limiter-service/commits)


[![Kakfa MPS Docker pulls](https://img.shields.io/docker/pulls/sjexposecomm/limiter-kafka-mps?logo=docker&style=plastic&label=Kakfa%20MPS%20pulls)](https://hub.docker.com/r/sjexposecomm/limiter-kafka-mps)
[![Kakfa MPS Docker size](https://img.shields.io/docker/image-size/sjexposecomm/limiter-kafka-mps?logo=docker&style=plastic&label=Kakfa%20MPS%20%20size)](https://hub.docker.com/r/sjexposecomm/limiter-kafka-mps/tags)


[![Processor Docker pulls](https://img.shields.io/docker/pulls/sjexposecomm/limiter-processor?logo=docker&style=plastic&label=Processor%20pulls)](https://hub.docker.com/r/sjexposecomm/limiter-processor)
[![Processor Docker size](https://img.shields.io/docker/image-size/sjexposecomm/limiter-processor?logo=docker&style=plastic&label=Processor%size)](https://hub.docker.com/r/sjexposecomm/limiter-processor/tags)

![](docs/images/arch-limiter.png)

This microservice is responsible for ...


## Modules structure

This project has the following modules:

* **cache-lib** - cache library which implements redis lock using annotation @RedisLock and gzip serializer
* **model** - objects which are shared through topics
* **kafka-mps** - message proxy service to implement push model
* **processor** - component which decide if the user has reached the rate limit

## Framework

* [Spring Boot 3.3.2](https://spring.io/projects/spring-boot/)
* [Spring Cloud 2023.0.3](https://spring.io/projects/spring-cloud)
* [Spring Kafka 3.2.3](https://spring.io/projects/spring-kafka)
* [Openapi V3](https://swagger.io/specification/)

## Requirements

* [Java 21](https://openjdk.org/install/)
* [Maven 3.8.8+](https://maven.apache.org/download.cgi)
* [AWS Cli](https://aws.amazon.com/es/cli/)
* [Docker](https://www.docker.com/)

## Check styling

### Check if styling is applied

```bash
mvn spotless:check 
```

### Apply checkstyle to all files

```bash
mvn spotless:apply 
```

*Note*: this goal is run as part as build process

## Build

```bash
mvn clean && mvn install
```

## Run Tests
```bash
mvn clean && mvn tests
```

## Runtime requeriments

* **Redis** - it must be run on port 6379. None password.
* **Kafka** - it must be run on port 9092.

### Run application
```bash
./run.sh
```

## Check message on a Kafka topic

It's possible to jump into kafka server using ssh, or connect to the container if it is running in a docker container,
and the following command will allow you to see the message which are generated by this service on topic "users-entities-update-topic"

```bash
/bin/kafka-console-consumer --bootstrap-server localhost:9092 --from-beginning --topic users-entities-update-topic
```

