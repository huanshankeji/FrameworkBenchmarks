import database.r2dbcDatabaseConnectPoolOptimized

// Configuration 4: shared-pool-size-512-optimized-config
// Shared pool of 512 with optimized configuration (current implementation)
suspend fun main() =
    commonRunVertxServer(
        "Vert.x-Web Kotlinx with Exposed R2DBC (and PostgreSQL) - Shared Pool Size 512 Optimized",
        { r2dbcDatabaseConnectPoolOptimized(512) },
        ::MainVerticle
    )
