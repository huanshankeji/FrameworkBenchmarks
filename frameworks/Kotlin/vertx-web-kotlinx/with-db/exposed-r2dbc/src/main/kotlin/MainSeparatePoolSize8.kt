import database.r2dbcDatabaseConnectPoolOriginal

// Configuration 2: separate-pool-size-8
// Configuration 1 with pool size increased to 8 to roughly match total of 512 on 56-core machine
suspend fun main() =
    commonRunVertxServer(
        "Vert.x-Web Kotlinx with Exposed R2DBC (and PostgreSQL) - Separate Pool Size 8",
        {},
        { MainVerticleSeparatePool(poolSize = 8, useOptimizedConfig = false) }
    )
