package database

import org.jetbrains.exposed.v1.core.vendors.PostgreSQLDialect
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabaseConfig

fun r2DbcDatabaseConnect() =
    R2dbcDatabase.connect(connectionFactoryOptimized, R2dbcDatabaseConfig {
        explicitDialect = PostgreSQLDialect()
    })

fun r2dbcDatabaseConnectPool(connectionPoolSize: Int) =
    R2dbcDatabase.connect(connectionPoolOptimized(connectionPoolSize), R2dbcDatabaseConfig {
        explicitDialect = PostgreSQLDialect()
    })

// Configuration variants for benchmarking
fun r2dbcDatabaseConnectPoolOriginal(connectionPoolSize: Int) =
    R2dbcDatabase.connect(connectionPoolOriginal(connectionPoolSize), R2dbcDatabaseConfig {
        explicitDialect = PostgreSQLDialect()
    })

fun r2dbcDatabaseConnectPoolOptimized(connectionPoolSize: Int) =
    R2dbcDatabase.connect(connectionPoolOptimized(connectionPoolSize), R2dbcDatabaseConfig {
        explicitDialect = PostgreSQLDialect()
    })
