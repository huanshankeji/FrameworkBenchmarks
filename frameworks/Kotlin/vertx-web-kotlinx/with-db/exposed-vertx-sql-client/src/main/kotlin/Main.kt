import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi

@OptIn(ExperimentalEvscApi::class)
suspend fun main() =
    commonRunVertxServer(
        "Vert.x-Web Kotlinx with Exposed Vert.x SQL Client (and PostgreSQL)",
        { },
        { MainVerticle() }
    )
