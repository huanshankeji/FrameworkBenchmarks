import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.Router
import kotlinx.serialization.encodeToString

class MainVerticle : CommonVerticle() {
    private val helloWorldMessage = Message("Hello, World!")
    private val jsonBuffer = Buffer.buffer(json.encodeToString(Serializers.message, helloWorldMessage))
    private val plaintextBuffer = Buffer.buffer(helloWorldMessage.message)

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
