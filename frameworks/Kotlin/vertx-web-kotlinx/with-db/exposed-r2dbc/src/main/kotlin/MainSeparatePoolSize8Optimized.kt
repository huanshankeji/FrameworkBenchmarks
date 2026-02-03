import database.r2dbcDatabaseConnectPoolOptimized

// Configuration 3: separate-pool-size-8-optimized-config
// Configuration 2 with optimized ConnectionFactory and connectionPoolConfiguration
suspend fun main() =
    commonRunVertxServer(
        "Vert.x-Web Kotlinx with Exposed R2DBC (and PostgreSQL) - Separate Pool Size 8 Optimized",
        {},
        { MainVerticleSeparatePool(poolSize = 8, useOptimizedConfig = true) }
    )
