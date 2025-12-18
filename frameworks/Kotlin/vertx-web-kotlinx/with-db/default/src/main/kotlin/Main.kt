import io.vertx.core.Vertx
import io.vertx.core.impl.cpu.CpuCoreSensor
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.vertxOptionsOf
import io.vertx.kotlin.coroutines.coAwait
import java.util.function.Supplier
import java.util.logging.Logger

const val SERVER_NAME = "Vert.x-Web Kotlinx with PostgreSQL Benchmark server"
val numProcessors = CpuCoreSensor.availableProcessors()

val logger = Logger.getLogger("Vert.x-Web Kotlinx PostgreSQL Benchmark")

suspend fun main() {
    logger.info("$SERVER_NAME starting...")
    val vertx = Vertx.vertx(
        vertxOptionsOf(
            eventLoopPoolSize = numProcessors, preferNativeTransport = true, disableTCCL = true
        )
    )
    vertx.exceptionHandler {
        logger.info("Vertx exception caught: $it")
        it.printStackTrace()
    }
    vertx.deployVerticle(
        Supplier { MainVerticle() },
        deploymentOptionsOf(instances = numProcessors)
    ).coAwait()
    logger.info("$SERVER_NAME started.")
}
