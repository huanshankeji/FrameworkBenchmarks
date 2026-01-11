import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.Router

class MainVerticle : CommonVerticle() {
    private val helloWorldMessage = "Hello, World!"
    private val jsonBuffer = Buffer.buffer("""{"message":"$helloWorldMessage"}""")
    private val plaintextBuffer = Buffer.buffer(helloWorldMessage)

    override fun Router.routes() {
        get("/json").coHandlerUnconfined {
            it.response().run {
                headers().run {
                    addCommonHeaders()
                    add(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.applicationJson)
                }
                end(jsonBuffer)/*.coAwait()*/
            }
        }

        get("/plaintext").coHandlerUnconfined {
            it.response().run {
                headers().run {
                    addCommonHeaders()
                    add(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.textPlain)
                }
                end(plaintextBuffer)/*.coAwait()*/
            }
        }
    }
}
