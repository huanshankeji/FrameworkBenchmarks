package io.vertx.benchmark

import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.vertxOptionsOf
import io.vertx.kotlin.coroutines.await

suspend fun main() {
    Vertx.vertx(vertxOptionsOf(preferNativeTransport = true))
        .deployVerticle({ App() }, deploymentOptionsOf(instances = VertxOptions.DEFAULT_EVENT_LOOP_POOL_SIZE)).await()
    println("Verticles deployed")
}
