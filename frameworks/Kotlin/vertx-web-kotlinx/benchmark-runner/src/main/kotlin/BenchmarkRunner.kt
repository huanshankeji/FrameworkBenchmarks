import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import main as defaultMain
import com.example.exposedvertxsqlclient.MainWrapper

/**
 * Benchmark runner that uses Testcontainers to start PostgreSQL and runs the vertx-web-kotlinx application.
 * 
 * This allows running any implementation locally for benchmarking with wrk without the full TFB Docker setup.
 * 
 * Usage:
 * 1. Run this application: 
 *    - Default implementation: `./gradlew :benchmark-runner:run`
 *    - Specific implementation: `./gradlew :benchmark-runner:run --args="exposed-vertx-sql-client"`
 * 2. In another terminal, run wrk benchmarks: `wrk -t 4 -c 100 -d 30s http://localhost:8080/json`
 * 3. Press Ctrl+C to stop
 * 
 * Supported implementations:
 * - default (vertx-pg-client)
 * - r2dbc
 * - exposed-r2dbc
 * - exposed-vertx-sql-client
 */
suspend fun runBenchmark(args: Array<String>) {
    val implementation = args.getOrNull(0) ?: "default"
    
    println("=== Vert.x-Web Kotlinx Benchmark Runner ===")
    println("Implementation: $implementation")
    println()
    
    // Create Docker network
    println("Creating Docker network...")
    val network = Network.newNetwork()
    
    // Start PostgreSQL with Testcontainers
    println("Starting PostgreSQL container...")
    val postgres = PostgreSQLContainer("postgres:18-alpine")
        .withDatabaseName("hello_world")
        .withUsername("benchmarkdbuser")
        .withPassword("benchmarkdbpass")
        .withInitScript("init-postgres.sql")
        .withNetwork(network)
        .withNetworkAliases("tfb-database")
    
    postgres.start()
    
    println("PostgreSQL started:")
    println("  Internal hostname: tfb-database:5432 (Docker network)")
    println("  External endpoint: ${postgres.host}:${postgres.firstMappedPort}")
    println()
    
    // Override database connection settings with Testcontainers host/port
    // This allows the application running on the host to connect to the containerized PostgreSQL
    System.setProperty("db.host", postgres.host)
    System.setProperty("db.port", postgres.firstMappedPort.toString())
    
    println("Database connection override:")
    println("  Host: ${postgres.host}")
    println("  Port: ${postgres.firstMappedPort}")
    println()
    
    // Start the Vert.x application
    println("Starting Vert.x application...")
    println("  Implementation: $implementation")
    println("  Application will be available at: http://localhost:8080")
    println()
    println("Endpoints:")
    println("  JSON:      http://localhost:8080/json")
    println("  Plaintext: http://localhost:8080/plaintext")
    
    // Check if it's a database-enabled implementation
    if (implementation != "without-db") {
        println("  DB:        http://localhost:8080/db")
        println("  Queries:   http://localhost:8080/queries?queries=20")
        println("  Updates:   http://localhost:8080/updates?queries=20")
        println("  Fortunes:  http://localhost:8080/fortunes")
    }
    println()
    
    // Run the appropriate main function based on implementation
    when (implementation) {
        "default" -> {
            // Default PostgreSQL implementation
            defaultMain()
        }
        "r2dbc" -> {
            // R2DBC implementation
            error("R2DBC implementation not yet supported in benchmark runner")
        }
        "exposed-r2dbc" -> {
            // Exposed R2DBC implementation
            error("Exposed R2DBC implementation not yet supported in benchmark runner")
        }
        "exposed-vertx-sql-client" -> {
            // Exposed Vert.x SQL Client implementation
            MainWrapper.run()
        }
        else -> {
            error("Unknown implementation: $implementation")
        }
    }
    
    // Note: The application runs indefinitely until killed
    // PostgreSQL container will be stopped when JVM exits
    Runtime.getRuntime().addShutdownHook(Thread {
        println("\nShutting down...")
        postgres.stop()
        network.close()
        println("Cleanup complete")
    })
}

// Entry point for Gradle application plugin
fun main(args: Array<String>): Unit = runBlocking {
    runBenchmark(args)
}
