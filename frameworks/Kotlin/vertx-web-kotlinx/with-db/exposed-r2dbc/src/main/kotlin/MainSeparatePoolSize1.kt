import database.r2dbcDatabaseConnectPoolOriginal

// Configuration 1: separate-pool-size-1
// Original code with one connection pool with size 1 per Verticle
suspend fun main() =
    commonRunVertxServer(
        "Vert.x-Web Kotlinx with Exposed R2DBC (and PostgreSQL) - Separate Pool Size 1",
        {},
        { MainVerticleSeparatePool(poolSize = 1, useOptimizedConfig = false) }
    )
