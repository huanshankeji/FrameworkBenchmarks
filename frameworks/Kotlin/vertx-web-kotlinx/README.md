# Vert.x-Web Kotlinx Benchmarking Test

Vert.x-Web in Kotlin with request handling implemented as much with official kotlinx libraries as possible.

This project is structured as a multi-module Gradle project with common code extracted for proper abstraction.

## Project Structure

The project follows a modular architecture:

- **`common`**: Shared code including models, serialization, HTTP utilities, and base classes
- **`without-db:default`**: JSON and plaintext endpoints (no database)
- **`with-db:common`**: Common database configuration
- **`with-db:default`**: Database endpoints using raw SQL with Vert.x PostgreSQL Client
- **`with-db:exposed-r2dbc`**: Placeholder for future R2DBC support with Exposed
- **`with-db:exposed-vertx-sql-client`**: Database endpoints using [Exposed Vert.x SQL Client](https://github.com/huanshankeji/exposed-vertx-sql-client)

Code is written to be as concise as possible with common code extracted into common functions and base classes following DRY principles. SQL client implementation details and JVM Options are adapted from [the vertx-web portion](../../Java/vertx-web) and [the vertx portion](../../Java/vertx). All requests are handled in coroutines using suspend functions. JSON serialization is implemented with kotlinx.serialization and Fortunes with kotlinx.html. The benchmark is run on the latest LTS version of JVM, 25.

## Test Type Implementation Source Code

### Without Database Tests
* [JSON](without-db/default/src/main/kotlin/MainVerticle.kt) - implemented with kotlinx.serialization
* [PLAINTEXT](without-db/default/src/main/kotlin/MainVerticle.kt)

### With Database Tests (Raw SQL)
* [DB](with-db/default/src/main/kotlin/MainVerticle.kt)
* [QUERY](with-db/default/src/main/kotlin/MainVerticle.kt)
* [UPDATE](with-db/default/src/main/kotlin/MainVerticle.kt)
* [FORTUNES](with-db/default/src/main/kotlin/MainVerticle.kt) - implemented with kotlinx.html

### With Database Tests (Exposed Vert.x SQL Client)
* [DB](with-db/exposed-vertx-sql-client/src/main/kotlin/MainVerticle.kt)
* [QUERY](with-db/exposed-vertx-sql-client/src/main/kotlin/MainVerticle.kt)
* [UPDATE](with-db/exposed-vertx-sql-client/src/main/kotlin/MainVerticle.kt)
* [FORTUNES](with-db/exposed-vertx-sql-client/src/main/kotlin/MainVerticle.kt)

## Important Libraries

The tests were run with:

* [Vert.x-Web](https://vertx.io/docs/vertx-web/java/)
* [Vert.x Reactive PostgreSQL Client](https://vertx.io/docs/vertx-pg-client/java/)
* [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)
* [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization)
* [kotlinx-io](https://github.com/Kotlin/kotlinx-io)
* [kotlinx.html](https://github.com/Kotlin/kotlinx.html)
* [Exposed Vert.x SQL Client](https://github.com/huanshankeji/exposed-vertx-sql-client) (for ORM tests)

## Test URLs

### JSON

http://localhost:8080/json

### PLAINTEXT

http://localhost:8080/plaintext

### DB

http://localhost:8080/db

### QUERY

http://localhost:8080/queries?queries=

### UPDATE

http://localhost:8080/updates?queries=

### FORTUNES

http://localhost:8080/fortunes

## Building and Running

The project uses Gradle 9.2.1 with Kotlin 2.3.0 and version catalogs.

Build a specific module:
```bash
./gradlew :without-db:default:build
./gradlew :with-db:default:build
./gradlew :with-db:exposed-vertx-sql-client:build
```

Run a specific module:
```bash
./gradlew :without-db:default:run
./gradlew :with-db:default:run
./gradlew :with-db:exposed-vertx-sql-client:run
```
