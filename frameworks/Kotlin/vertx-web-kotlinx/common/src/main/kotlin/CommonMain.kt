import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.impl.cpu.CpuCoreSensor
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.micrometer.MicrometerMetricsFactory
import io.vertx.micrometer.MicrometerMetricsOptions
import java.util.function.Supplier
import java.util.logging.Logger

val numProcessors = CpuCoreSensor.availableProcessors()

val logger = Logger.getLogger("Vert.x-Web Kotlinx Benchmark")
suspend fun <SharedResources> commonRunVertxServer(
    benchmarkName: String,
    createSharedResources: (Vertx) -> SharedResources,
    createVerticle: (SharedResources) -> Verticle
) {
    val serverName = "$benchmarkName benchmark server"
    logger.info("$serverName starting...")

    // Configure Micrometer metrics with a custom registry for fine-grained operation timing
    // Using Vert.x Builder API to inject our existing registry as recommended in the docs
    // See: https://vertx.io/docs/vertx-micrometer-metrics/java/#_reusing_an_existing_registry
    val metricsOptions = MicrometerMetricsOptions()
        .setEnabled(true)

    val vertx = Vertx
        .builder()
        .withMetrics(MicrometerMetricsFactory(Metrics.registry))
        .with(
            io.vertx.core.VertxOptions()
                .setEventLoopPoolSize(numProcessors)
                .setPreferNativeTransport(true)
                .setDisableTCCL(true)
                .setMetricsOptions(metricsOptions)
        )
        .build()

    logger.info("Metrics configured with Micrometer for fine-grained operation timing in updates test")

    vertx.exceptionHandler {
        logger.info("Vertx exception caught: $it")
        it.printStackTrace()
    }
    val sharedResources = createSharedResources(vertx)
    vertx.deployVerticle(
        Supplier { createVerticle(sharedResources) },
        deploymentOptionsOf(instances = numProcessors)
    ).coAwait()
    logger.info("$serverName started.")
}
