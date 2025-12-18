import common.*
import database.DatabaseConfig
import io.netty.channel.unix.Errors
import io.netty.channel.unix.Errors.NativeIoException
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.http.httpServerOptionsOf
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgConnection
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.net.SocketException
import kotlin.random.Random

class MainVerticle : BaseMainVerticle() {
    // `PgConnection`s as used in the "vertx" portion offers better performance than `PgPool`s.
    lateinit var pgConnection: PgConnection
    lateinit var date: String
    lateinit var httpServer: HttpServer

    lateinit var selectWorldQuery: PreparedQuery<RowSet<Row>>
    lateinit var selectFortuneQuery: PreparedQuery<RowSet<Row>>
    lateinit var updateWorldQuery: PreparedQuery<RowSet<Row>>

    val random = Random(0)

    fun setCurrentDate() {
        date = getCurrentDate()
    }

    override suspend fun start() {
        // Parameters are copied from the "vertx-web" and "vertx" portions.
        pgConnection = PgConnection.connect(
            vertx,
            pgConnectOptionsOf(
                database = DatabaseConfig.DATABASE,
                host = DatabaseConfig.HOST,
                user = DatabaseConfig.USER,
                password = DatabaseConfig.PASSWORD,
                cachePreparedStatements = DatabaseConfig.CACHE_PREPARED_STATEMENTS,
                pipeliningLimit = DatabaseConfig.PIPELINING_LIMIT
            )
        ).coAwait()

        selectWorldQuery = pgConnection.preparedQuery(SELECT_WORLD_SQL)
        selectFortuneQuery = pgConnection.preparedQuery(SELECT_FORTUNE_SQL)
        updateWorldQuery = pgConnection.preparedQuery(UPDATE_WORLD_SQL)

        setCurrentDate()
        vertx.setPeriodic(1000) { setCurrentDate() }
        httpServer = vertx.createHttpServer(
            httpServerOptionsOf(port = 8080, http2ClearTextEnabled = false, strictThreadMode = true)
        )
            .requestHandler(Router.router(vertx).apply { routes() })
            .exceptionHandler {
                // wrk resets the connections when benchmarking is finished.
                if ((/* for native transport */it is NativeIoException && it.expectedErr() == Errors.ERRNO_ECONNRESET_NEGATIVE) ||
                    (/* for Java NIO */ it is SocketException && it.message == "Connection reset")
                )
                    return@exceptionHandler

                logger.info("Exception in HttpServer: $it")
                it.printStackTrace()
            }
            .listen().coAwait()
    }

    suspend fun selectRandomWorld() =
        selectWorldQuery.execute(Tuple.of(random.nextIntBetween1And10000())).coAwait()
            .single().toWorld()

    suspend fun selectRandomWorlds(queries: Int): List<World> =
        // This should be slightly more efficient.
        awaitAll(*Array(queries) { async { selectRandomWorld() } })

    fun Router.routes() {
        get("/db").jsonResponseCoHandler(Serializers.world, { date }) {
            selectRandomWorld()
        }

        get("/queries").jsonResponseCoHandler(Serializers.worlds, { date }) {
            val queries = it.request().getQueries()
            selectRandomWorlds(queries)
        }

        get("/fortunes").coHandlerUnconfined {
            val fortunes = mutableListOf<Fortune>()
            selectFortuneQuery.execute().coAwait()
                .mapTo(fortunes) { it.toFortune() }

            fortunes.add(Fortune(0, "Additional fortune added at request time."))
            fortunes.sortBy { it.message }

            val htmlString = buildFortunesHtml(fortunes)

            it.response().run {
                headers().run {
                    addCommonHeaders(date)
                    add(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.textHtmlCharsetUtf8)
                }
                end(htmlString) // .coAwait() intentionally omitted for better performance
            }
        }

        // Some changes to this part in the `vertx` portion in #9142 are not ported.
        get("/updates").jsonResponseCoHandler(Serializers.worlds, { date }) {
            val queries = it.request().getQueries()
            val worlds = selectRandomWorlds(queries)
            val updatedWorlds = worlds.map { it.copy(randomNumber = random.nextIntBetween1And10000()) }

            // The updated worlds need to be sorted first to avoid deadlocks.
            updateWorldQuery
                .executeBatch(updatedWorlds.sortedBy { it.id }.map { Tuple.of(it.randomNumber, it.id) }).coAwait()

            updatedWorlds
        }
    }
}
