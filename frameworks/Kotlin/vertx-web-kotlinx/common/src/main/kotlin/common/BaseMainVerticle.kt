package common

import io.vertx.core.buffer.Buffer
import io.vertx.ext.web.Route
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.CoroutineRouterSupport
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.Dispatchers
import kotlinx.io.buffered
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.io.encodeToSink

abstract class BaseMainVerticle : CoroutineVerticle(), CoroutineRouterSupport {
    
    fun Route.coHandlerUnconfined(requestHandler: suspend (RoutingContext) -> Unit): Route =
        /* Some conclusions from the Plaintext test results with trailing `await()`s:
           1. `launch { /*...*/ }` < `launch(start = CoroutineStart.UNDISPATCHED) { /*...*/ }` < `launch(Dispatchers.Unconfined) { /*...*/ }`.
           1. `launch { /*...*/ }` without `context` or `start` lead to `io.netty.channel.StacklessClosedChannelException` and `io.netty.channel.unix.Errors$NativeIoException: sendAddress(..) failed: Connection reset by peer`. */
        coHandler(Dispatchers.Unconfined, requestHandler)

    inline fun <reified T : Any> Route.jsonResponseCoHandler(
        serializer: SerializationStrategy<T>,
        crossinline getDate: () -> String,
        crossinline requestHandler: suspend (RoutingContext) -> @Serializable T
    ) =
        coHandlerUnconfined {
            it.response().run {
                addJsonResponseHeaders(getDate())

                // Approach 3 - using Buffer
                end(Buffer.buffer().apply {
                    toRawSink().buffered().use { bufferedSink ->
                        @OptIn(ExperimentalSerializationApi::class)
                        json.encodeToSink(serializer, requestHandler(it), bufferedSink)
                    }
                })
            }
        }
}
