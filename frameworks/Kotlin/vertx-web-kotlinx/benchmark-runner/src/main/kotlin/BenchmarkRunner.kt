import kotlinx.coroutines.runBlocking
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Benchmark runner that uses Testcontainers to start PostgreSQL and runs the vertx-web-kotlinx application.
 * 
 * This allows running the application locally for benchmarking with wrk without the full TFB Docker setup.
 * 
 * Usage:
 * 1. Run this application: `./gradlew :benchmark-runner:run`
 * 2. In another terminal, run wrk benchmarks: `wrk -t 4 -c 100 -d 30s http://localhost:8080/json`
 * 3. Press Ctrl+C to stop
 */
suspend fun main() {
    println("=== Vert.x-Web Kotlinx Benchmark Runner ===")
    println()
    
    // Create Docker network
    println("Creating Docker network...")
    val network = Network.newNetwork()
    
    // Start PostgreSQL with Testcontainers
    println("Starting PostgreSQL container...")
    val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
        .withDatabaseName("hello_world")
        .withUsername("benchmarkdbuser")
        .withPassword("benchmarkdbpass")
        .withInitScript("init-postgres.sql")
        .withNetwork(network)
        .withNetworkAliases("tfb-database")
    
    postgres.start()
    
    println("PostgreSQL started:")
    println("  Internal hostname: tfb-database:5432")
    println("  External endpoint: ${postgres.host}:${postgres.firstMappedPort}")
    println()
    
    // Start the Vert.x application
    println("Starting Vert.x application...")
    println("  Application will be available at: http://localhost:8080")
    println()
    
    // Run the actual application main function
    MainKt.main()
    
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
fun main() = runBlocking {
    main()
}
