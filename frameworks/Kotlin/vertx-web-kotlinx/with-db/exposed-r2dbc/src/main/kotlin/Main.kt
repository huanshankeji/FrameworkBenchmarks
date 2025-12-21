import database.r2DbcDatabaseConnect

suspend fun main() =
    commonRunVertxServer(
        "Vert.x-Web Kotlinx with Exposed R2DBC (and PostgreSQL)",
        { r2DbcDatabaseConnect() },
        ::MainVerticle
    )
