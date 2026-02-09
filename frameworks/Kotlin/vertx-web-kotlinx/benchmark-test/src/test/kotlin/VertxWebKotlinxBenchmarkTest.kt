import database.*
import io.vertx.core.Vertx
import io.vertx.core.impl.cpu.CpuCoreSensor
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.vertxOptionsOf
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.*
import org.testcontainers.containers.Network
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.function.Supplier
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test suite for vertx-web-kotlinx-postgresql using Testcontainers.
 * 
 * This replaces the need for `./tfb --test vertx-web-kotlinx-postgresql` by:
 * 1. Using Testcontainers to automatically manage PostgreSQL lifecycle
 * 2. Starting the actual Vert.x application
 * 3. Testing all benchmark endpoints
 * 4. Performing simple load testing to measure performance (wrk-style)
 * 
 * The PostgreSQL container is started with network alias "tfb-database" so the
 * application can connect to it using its hardcoded hostname.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VertxWebKotlinxBenchmarkTest {
    
    private lateinit var network: Network
    private lateinit var postgres: PostgreSQLContainer<*>
    private lateinit var vertx: Vertx
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    
    private val baseUrl = "http://localhost:8080"
    
    @BeforeAll
    fun setup() = runBlocking {
        println("\n=== Setting up test environment ===")
        
        // Create a Docker network for the containers
        println("Creating Docker network...")
        network = Network.newNetwork()
        
        // Start PostgreSQL with Testcontainers on the network with alias "tfb-database"
        println("Starting PostgreSQL container...")
        postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
            .withDatabaseName("hello_world")
            .withUsername("benchmarkdbuser")
            .withPassword("benchmarkdbpass")
            .withInitScript("init-postgres.sql")
            .withNetwork(network)
            .withNetworkAliases("tfb-database") // This allows the app to connect to it as "tfb-database"
        
        postgres.start()
        
        println("PostgreSQL started:")
        println("  Host (external): ${postgres.host}:${postgres.firstMappedPort}")
        println("  Host (internal): tfb-database:5432")
        println("  JDBC URL: ${postgres.jdbcUrl}")
        
        println("\nStarting Vert.x application...")
        val numProcessors = CpuCoreSensor.availableProcessors()
        vertx = Vertx.vertx(
            vertxOptionsOf(
                eventLoopPoolSize = numProcessors,
                preferNativeTransport = true,
                disableTCCL = true
            )
        )
        
        vertx.exceptionHandler {
            println("Vertx exception: $it")
            it.printStackTrace()
        }
        
        // Deploy MainVerticle - it will connect to "tfb-database" on the network
        val verticle = MainVerticle()
        vertx.deployVerticle(
            Supplier { verticle },
            deploymentOptionsOf(instances = 1) // Use 1 instance for testing
        ).coAwait()
        
        println("Vert.x application started on port 8080")
        
        // Wait for server to be fully ready
        Thread.sleep(2000)
        println("=== Test environment ready ===\n")
    }
    
    @AfterAll
    fun tearDown() = runBlocking {
        println("\n=== Cleaning up test environment ===")
        vertx.close().coAwait()
        postgres.stop()
        network.close()
        println("=== Cleanup complete ===\n")
    }
    
    private fun get(path: String): HttpResponse<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .GET()
            .build()
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }
    
    @Test
    fun `test JSON endpoint`() {
        println("\n--- Testing JSON endpoint ---")
        val response = get("/json")
        assertEquals(200, response.statusCode(), "JSON endpoint should return 200")
        assertEquals("application/json", response.headers().firstValue("content-type").orElse(""))
        
        val json = Json.parseToJsonElement(response.body()).jsonObject
        assertNotNull(json["message"])
        assertEquals("Hello, World!", json["message"]?.jsonPrimitive?.content)
        println("✓ JSON endpoint working correctly")
    }
    
    @Test
    fun `test plaintext endpoint`() {
        println("\n--- Testing plaintext endpoint ---")
        val response = get("/plaintext")
        assertEquals(200, response.statusCode(), "Plaintext endpoint should return 200")
        assertEquals("text/plain", response.headers().firstValue("content-type").orElse(""))
        assertEquals("Hello, World!", response.body())
        println("✓ Plaintext endpoint working correctly")
    }
    
    @Test
    fun `test db endpoint`() {
        println("\n--- Testing DB endpoint ---")
        val response = get("/db")
        assertEquals(200, response.statusCode(), "DB endpoint should return 200")
        assertEquals("application/json", response.headers().firstValue("content-type").orElse(""))
        
        val json = Json.parseToJsonElement(response.body()).jsonObject
        assertNotNull(json["id"])
        assertNotNull(json["randomNumber"])
        
        val id = json["id"]?.jsonPrimitive?.content?.toIntOrNull()
        val randomNumber = json["randomNumber"]?.jsonPrimitive?.content?.toIntOrNull()
        
        assertNotNull(id, "ID should be present")
        assertNotNull(randomNumber, "RandomNumber should be present")
        assertTrue(id in 1..10000, "ID should be between 1 and 10000")
        assertTrue(randomNumber in 1..10000, "RandomNumber should be between 1 and 10000")
        println("✓ DB endpoint working correctly (id=$id, randomNumber=$randomNumber)")
    }
    
    @Test
    fun `test queries endpoint with default count`() {
        println("\n--- Testing Queries endpoint (default) ---")
        val response = get("/queries")
        assertEquals(200, response.statusCode(), "Queries endpoint should return 200")
        assertEquals("application/json", response.headers().firstValue("content-type").orElse(""))
        
        val json = Json.parseToJsonElement(response.body()).jsonArray
        assertTrue(json.size in 1..500, "Should return 1-500 results")
        println("✓ Queries endpoint working correctly (returned ${json.size} results)")
    }
    
    @Test
    fun `test queries endpoint with specific count`() {
        println("\n--- Testing Queries endpoint (queries=5) ---")
        val response = get("/queries?queries=5")
        assertEquals(200, response.statusCode(), "Queries endpoint should return 200")
        
        val json = Json.parseToJsonElement(response.body()).jsonArray
        assertEquals(5, json.size, "Should return exactly 5 results")
        
        json.forEach { element ->
            val obj = element.jsonObject
            assertNotNull(obj["id"])
            assertNotNull(obj["randomNumber"])
        }
        println("✓ Queries endpoint working correctly with parameter")
    }
    
    @Test
    fun `test updates endpoint`() {
        println("\n--- Testing Updates endpoint ---")
        val response = get("/updates?queries=3")
        assertEquals(200, response.statusCode(), "Updates endpoint should return 200")
        assertEquals("application/json", response.headers().firstValue("content-type").orElse(""))
        
        val json = Json.parseToJsonElement(response.body()).jsonArray
        assertEquals(3, json.size, "Should return exactly 3 results")
        println("✓ Updates endpoint working correctly")
    }
    
    @Test
    fun `test fortunes endpoint`() {
        println("\n--- Testing Fortunes endpoint ---")
        val response = get("/fortunes")
        assertEquals(200, response.statusCode(), "Fortunes endpoint should return 200")
        
        val contentType = response.headers().firstValue("content-type").orElse("")
        assertTrue(contentType.startsWith("text/html"), "Content type should be text/html")
        
        val body = response.body()
        assertTrue(body.contains("<!DOCTYPE html>"), "Should contain DOCTYPE")
        assertTrue(body.contains("<table>"), "Should contain table")
        assertTrue(body.contains("Fortunes"), "Should contain 'Fortunes' text")
        
        // Verify XSS protection - script tag should be escaped
        assertTrue(body.contains("&lt;script&gt;") || !body.contains("<script>alert"), 
            "XSS test script should be escaped")
        println("✓ Fortunes endpoint working correctly with XSS protection")
    }
    
    @Nested
    @DisplayName("Load Testing (wrk-style)")
    inner class LoadTests {
        
        @Test
        fun `load test JSON endpoint`() {
            performLoadTest("/json", requests = 1000, concurrency = 10, name = "JSON")
        }
        
        @Test
        fun `load test plaintext endpoint`() {
            performLoadTest("/plaintext", requests = 1000, concurrency = 10, name = "Plaintext")
        }
        
        @Test
        fun `load test db endpoint`() {
            performLoadTest("/db", requests = 500, concurrency = 10, name = "DB")
        }
        
        @Test
        fun `load test queries endpoint`() {
            performLoadTest("/queries?queries=5", requests = 300, concurrency = 5, name = "Queries")
        }
        
        @Test
        fun `load test updates endpoint`() {
            performLoadTest("/updates?queries=3", requests = 200, concurrency = 5, name = "Updates")
        }
        
        /**
         * Performs a simple load test similar to wrk.
         * This is not as sophisticated as wrk but provides basic performance metrics.
         */
        private fun performLoadTest(path: String, requests: Int, concurrency: Int, name: String) {
            println("\n=== Load Testing: $name ===")
            println("Endpoint: $path")
            println("Requests: $requests, Concurrency: $concurrency")
            
            val startTime = System.currentTimeMillis()
            var successCount = 0
            var errorCount = 0
            val latencies = mutableListOf<Long>()
            
            // Simple concurrent load test using threads
            val threads = (1..concurrency).map {
                Thread {
                    val requestsPerThread = requests / concurrency
                    repeat(requestsPerThread) {
                        try {
                            val reqStart = System.nanoTime()
                            val response = get(path)
                            val reqEnd = System.nanoTime()
                            
                            synchronized(latencies) {
                                if (response.statusCode() == 200) {
                                    successCount++
                                    latencies.add((reqEnd - reqStart) / 1_000_000) // Convert to ms
                                } else {
                                    errorCount++
                                    println("  Error: HTTP ${response.statusCode()}")
                                }
                            }
                        } catch (e: Exception) {
                            synchronized(latencies) {
                                errorCount++
                                println("  Exception: ${e.message}")
                            }
                        }
                    }
                }
            }
            
            threads.forEach { it.start() }
            threads.forEach { it.join() }
            
            val totalTime = System.currentTimeMillis() - startTime
            
            if (latencies.isNotEmpty()) {
                val sortedLatencies = latencies.sorted()
                val avgLatency = latencies.average()
                val minLatency = sortedLatencies.first()
                val maxLatency = sortedLatencies.last()
                val p50 = sortedLatencies[sortedLatencies.size / 2]
                val p95 = sortedLatencies[(sortedLatencies.size * 0.95).toInt()]
                val p99 = sortedLatencies[(sortedLatencies.size * 0.99).toInt()]
                val throughput = (successCount.toDouble() / totalTime) * 1000
                
                println("\nResults:")
                println("  Total time: ${totalTime}ms")
                println("  Success: $successCount, Errors: $errorCount")
                println("  Throughput: ${"%.2f".format(throughput)} req/s")
                println("  Latency:")
                println("    Min: ${minLatency}ms")
                println("    Avg: ${"%.2f".format(avgLatency)}ms")
                println("    Max: ${maxLatency}ms")
                println("    P50: ${p50}ms")
                println("    P95: ${p95}ms")
                println("    P99: ${p99}ms")
                
                assertTrue(successCount > 0, "Should have at least some successful requests")
                assertTrue(errorCount < successCount * 0.1, "Error rate should be less than 10%")
            } else {
                println("  No successful requests!")
                kotlin.test.fail("Load test failed: no successful requests")
            }
        }
    }
}
