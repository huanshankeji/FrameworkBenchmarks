package database

import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.postgresql.client.SSLMode
import io.r2dbc.spi.ConnectionFactory
import java.time.Duration

// not used currently
// Note that this URL doesn't have `USER` and `PASSWORD`
const val POSTGRESQL_R2DBC_URL = "r2dbc:postgresql://$HOST:5432/$DATABASE"

val connectionFactory: ConnectionFactory = PostgresqlConnectionFactory(
    PostgresqlConnectionConfiguration.builder()
        .host(HOST)
        .port(5432)
        .database(DATABASE)
        .username(USER)
        .password(PASSWORD)
        .sslMode(SSLMode.DISABLE)
        .tcpKeepAlive(true)
        .tcpNoDelay(true)
        .build()
)

fun connectionPoolConfiguration(size: Int) =
    ConnectionPoolConfiguration.builder(connectionFactory)
        .initialSize(size)
        .maxSize(size)
        .maxIdleTime(Duration.ofSeconds(30))
        .maxAcquireTime(Duration.ofSeconds(5))
        .validationQuery("SELECT 1")
        .build()

fun connectionPool(size: Int) =
    ConnectionPool(connectionPoolConfiguration(size))
