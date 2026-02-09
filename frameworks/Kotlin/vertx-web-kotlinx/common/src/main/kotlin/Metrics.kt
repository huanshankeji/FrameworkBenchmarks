import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import io.micrometer.core.instrument.logging.LoggingRegistryConfig
import java.time.Duration

/**
 * Global meter registry for metrics collection.
 * Uses LoggingMeterRegistry to output metrics to console for benchmarking analysis.
 * 
 * The registry is injected into Vert.x using MicrometerMetricsFactory as recommended
 * in the Vert.x Micrometer docs: https://vertx.io/docs/vertx-micrometer-metrics/java/#_reusing_an_existing_registry
 */
object Metrics {
    private val config = object : LoggingRegistryConfig {
        override fun get(key: String): String? = null
        // Log metrics every 10 seconds during benchmarking
        override fun step(): Duration = Duration.ofSeconds(10)
    }

    val registry: MeterRegistry = LoggingMeterRegistry(config, Clock.SYSTEM) { msg -> logger.info(msg) }

    // Timers for the updates endpoint operations
    val selectRandomWorldsTimer: Timer = Timer.builder("updates.selectRandomWorlds")
        .description("Time to select random worlds in the updates endpoint")
        .register(registry)

    val updateSortedWorldsTimer: Timer = Timer.builder("updates.updateSortedWorlds")
        .description("Time to update sorted worlds in the updates endpoint")
        .register(registry)

    // Fine-grained timers for executeBatchUpdate internal operations
    val executeBatchUpdateBuildStatementsTimer: Timer = Timer.builder("updates.executeBatchUpdate.buildStatements")
        .description("Time to build update statements for batch execution")
        .register(registry)

    val executeBatchUpdateExecuteTimer: Timer = Timer.builder("updates.executeBatchUpdate.execute")
        .description("Time to execute the batch update")
        .register(registry)

    val executeBatchUpdateTotalTimer: Timer = Timer.builder("updates.executeBatchUpdate.total")
        .description("Total time for executeBatchUpdate operation")
        .register(registry)
}

/**
 * Extension function to record time for a suspending block.
 * Micrometer's Timer.recordCallable doesn't work with suspend functions,
 * so we need a custom implementation that properly handles suspension.
 *
 * Note: The timing will include any suspension time, which is the desired behavior
 * for measuring the wall-clock time of async operations.
 */
suspend inline fun <T> Timer.recordSuspend(crossinline block: suspend () -> T): T {
    val sample = Timer.start()
    try {
        return block()
    } finally {
        sample.stop(this)
    }
}
