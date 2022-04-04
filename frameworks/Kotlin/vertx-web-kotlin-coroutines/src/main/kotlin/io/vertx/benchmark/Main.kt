package io.vertx.benchmark

import io.vertx.core.Vertx
import io.vertx.core.impl.cpu.CpuCoreSensor
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.vertxOptionsOf
import io.vertx.kotlin.coroutines.await

suspend fun main() {
    Vertx.vertx(vertxOptionsOf(preferNativeTransport = true))
        .deployVerticle({ App() }, deploymentOptionsOf(instances = CpuCoreSensor.availableProcessors())).await()
    println("Verticles deployed")
}
