import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.impl.cpu.CpuCoreSensor
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.vertxOptionsOf
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.micrometer.MicrometerMetricsOptions
import io.vertx.micrometer.backends.BackendRegistries
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

    // Configure Micrometer metrics with a named registry for fine-grained operation timing
    // The registry name allows us to access it via BackendRegistries after Vertx initialization
    val metricsRegistryName = "vertx-kotlinx-benchmark"
    val metricsOptions = MicrometerMetricsOptions()
        .setRegistryName(metricsRegistryName)
        .setEnabled(true)

    val vertx = Vertx.vertx(
        vertxOptionsOf(
            eventLoopPoolSize = numProcessors, preferNativeTransport = true, disableTCCL = true,
            metricsOptions = metricsOptions
        )
    )

    // Get the registry created by Vert.x and register our custom timers
    // The registry is available after Vertx is created
    val registry = BackendRegistries.getNow(metricsRegistryName)
    if (registry != null) {
        Metrics.initializeWithRegistry(registry)
        logger.info("Metrics configured with Micrometer for fine-grained operation timing in updates test")
    } else {
        logger.warning("Could not get metrics registry from BackendRegistries, using standalone LoggingMeterRegistry")
    }

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
