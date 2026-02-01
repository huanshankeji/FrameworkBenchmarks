import database.r2dbcDatabaseConnectPool

suspend fun main() =
    commonRunVertxServer(
        "Vert.x-Web Kotlinx with Exposed R2DBC (and PostgreSQL)",
        { r2dbcDatabaseConnectPool(512) },
        ::MainVerticle
    )
