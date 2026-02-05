import io.micrometer.core.instrument.Clock
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.logging.LoggingMeterRegistry
import io.micrometer.core.instrument.logging.LoggingRegistryConfig
import java.time.Duration

/**
 * Global meter registry for metrics collection.
 * Uses LoggingMeterRegistry to output metrics to console for benchmarking analysis.
 * Can be initialized with an external registry (e.g., from Vert.x BackendRegistries) or use a standalone registry.
 */
object Metrics {
    private val standaloneConfig = object : LoggingRegistryConfig {
        override fun get(key: String): String? = null
        // Log metrics every 10 seconds during benchmarking
        override fun step(): Duration = Duration.ofSeconds(10)
    }

    private val standaloneRegistry: MeterRegistry = LoggingMeterRegistry(standaloneConfig, Clock.SYSTEM) { msg -> logger.info(msg) }

    @Volatile
    private var _registry: MeterRegistry = standaloneRegistry

    val registry: MeterRegistry get() = _registry

    /**
     * Initialize with an external registry (e.g., from Vert.x BackendRegistries).
     * This should be called early, before the timers are used.
     */
    fun initializeWithRegistry(externalRegistry: MeterRegistry) {
        _registry = externalRegistry
        // Re-register the timers with the new registry
        initializeTimers()
        logger.info("Metrics initialized with external registry: ${externalRegistry.javaClass.simpleName}")
    }

    // Timers for the updates endpoint operations
    lateinit var selectRandomWorldsTimer: Timer
        private set

    lateinit var updateSortedWorldsTimer: Timer
        private set

    // Fine-grained timers for executeBatchUpdate internal operations
    lateinit var executeBatchUpdateBuildStatementsTimer: Timer
        private set

    lateinit var executeBatchUpdateExecuteTimer: Timer
        private set

    lateinit var executeBatchUpdateTotalTimer: Timer
        private set

    init {
        initializeTimers()
    }

    private fun initializeTimers() {
        selectRandomWorldsTimer = Timer.builder("updates.selectRandomWorlds")
            .description("Time to select random worlds in the updates endpoint")
            .register(registry)

        updateSortedWorldsTimer = Timer.builder("updates.updateSortedWorlds")
            .description("Time to update sorted worlds in the updates endpoint")
            .register(registry)

        executeBatchUpdateBuildStatementsTimer = Timer.builder("updates.executeBatchUpdate.buildStatements")
            .description("Time to build update statements for batch execution")
            .register(registry)

        executeBatchUpdateExecuteTimer = Timer.builder("updates.executeBatchUpdate.execute")
            .description("Time to execute the batch update")
            .register(registry)

        executeBatchUpdateTotalTimer = Timer.builder("updates.executeBatchUpdate.total")
            .description("Total time for executeBatchUpdate operation")
            .register(registry)
    }
}

/**
 * Extension function to record time for a suspending block.
 * Micrometer's Timer.recordCallable doesn't work with suspend functions,
 * so we need a custom implementation.
 */
inline fun <T> Timer.recordSuspend(block: () -> T): T {
    val sample = Timer.start()
    try {
        return block()
    } finally {
        sample.stop(this)
    }
}
