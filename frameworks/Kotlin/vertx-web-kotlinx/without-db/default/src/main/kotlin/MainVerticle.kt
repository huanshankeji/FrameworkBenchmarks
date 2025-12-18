import common.*
import io.netty.channel.unix.Errors
import io.netty.channel.unix.Errors.NativeIoException
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.kotlin.core.http.httpServerOptionsOf
import io.vertx.kotlin.coroutines.coAwait
import java.net.SocketException

class MainVerticle : BaseMainVerticle() {
    lateinit var date: String
    lateinit var httpServer: HttpServer

    fun setCurrentDate() {
        date = getCurrentDate()
    }

    override suspend fun start() {
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

    fun Router.routes() {
        get("/json").jsonResponseCoHandler(Serializers.message, { date }) {
            Message("Hello, World!")
        }

        get("/plaintext").coHandlerUnconfined {
            it.response().run {
                headers().run {
                    addCommonHeaders(date)
                    add(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.textPlain)
                }
                end("Hello, World!") // .coAwait() intentionally omitted for better performance
            }
        }
    }
}
