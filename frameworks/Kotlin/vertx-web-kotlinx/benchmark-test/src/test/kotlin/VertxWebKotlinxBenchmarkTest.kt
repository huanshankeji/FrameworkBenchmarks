import database.*
import io.vertx.core.Vertx
import io.vertx.core.impl.cpu.CpuCoreSensor
import io.vertx.kotlin.core.deploymentOptionsOf
import io.vertx.kotlin.core.vertxOptionsOf
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgConnection
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.*
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class VertxWebKotlinxBenchmarkTest {
    
    private lateinit var postgres: PostgreSQLContainer<*>
    private lateinit var vertx: Vertx
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()
    
    private val baseUrl = "http://localhost:8080"
    
    @BeforeAll
    fun setup() = runBlocking {
        // Start PostgreSQL with Testcontainers
        postgres = PostgreSQLContainer(DockerImageName.parse("postgres:18-alpine"))
            .withDatabaseName("hello_world")
            .withUsername("benchmarkdbuser")
            .withPassword("benchmarkdbpass")
            .withInitScript("init-postgres.sql")
        
        postgres.start()
        
        println("PostgreSQL started at ${postgres.jdbcUrl}")
        println("Host: ${postgres.host}, Port: ${postgres.firstMappedPort}")
        
        // Override database connection settings
        System.setProperty("db.host", postgres.host)
        System.setProperty("db.port", postgres.firstMappedPort.toString())
        
        // Start Vert.x server
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
        
        // Deploy the verticle with overridden DB settings
        val verticle = createVerticleWithCustomDb()
        vertx.deployVerticle(
            Supplier { verticle },
            deploymentOptionsOf(instances = 1) // Use 1 instance for testing
        ).coAwait()
        
        println("Vert.x server started on port 8080")
        
        // Wait for server to be ready
        Thread.sleep(2000)
    }
    
    private fun createVerticleWithCustomDb(): MainVerticle {
        // Create a custom MainVerticle that connects to the test database
        return object : MainVerticle() {
            override suspend fun initDbClient(): PgConnection {
                return PgConnection.connect(
                    vertx,
                    pgConnectOptionsOf(
                        database = postgres.databaseName,
                        host = postgres.host,
                        port = postgres.firstMappedPort,
                        user = postgres.username,
                        password = postgres.password,
                        cachePreparedStatements = true,
                        pipeliningLimit = 256
                    )
                ).coAwait().apply {
                    selectWorldQuery = preparedQuery(SELECT_WORLD_SQL)
                    selectFortuneQuery = preparedQuery(SELECT_FORTUNE_SQL)
                    updateWorldQuery = preparedQuery(UPDATE_WORLD_SQL)
                }
            }
        }
    }
    
    @AfterAll
    fun tearDown() = runBlocking {
        vertx.close().coAwait()
        postgres.stop()
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
        val response = get("/json")
        assertEquals(200, response.statusCode())
        assertEquals("application/json", response.headers().firstValue("content-type").orElse(""))
        
        val json = Json.parseToJsonElement(response.body()).jsonObject
        assertNotNull(json["message"])
        assertEquals("Hello, World!", json["message"]?.jsonPrimitive?.content)
    }
    
    @Test
    fun `test plaintext endpoint`() {
        val response = get("/plaintext")
        assertEquals(200, response.statusCode())
        assertEquals("text/plain", response.headers().firstValue("content-type").orElse(""))
        assertEquals("Hello, World!", response.body())
    }
    
    @Test
    fun `test db endpoint`() {
        val response = get("/db")
        assertEquals(200, response.statusCode())
        assertEquals("application/json", response.headers().firstValue("content-type").orElse(""))
        
        val json = Json.parseToJsonElement(response.body()).jsonObject
        assertNotNull(json["id"])
        assertNotNull(json["randomNumber"])
        
        val id = json["id"]?.jsonPrimitive?.content?.toIntOrNull()
        val randomNumber = json["randomNumber"]?.jsonPrimitive?.content?.toIntOrNull()
        
        assertNotNull(id)
        assertNotNull(randomNumber)
        assertTrue(id in 1..10000)
        assertTrue(randomNumber in 1..10000)
    }
    
    @Test
    fun `test queries endpoint with default count`() {
        val response = get("/queries")
        assertEquals(200, response.statusCode())
        assertEquals("application/json", response.headers().firstValue("content-type").orElse(""))
        
        val body = response.body()
        assertTrue(body.startsWith("["))
        assertTrue(body.contains("\"id\""))
        assertTrue(body.contains("\"randomNumber\""))
    }
    
    @Test
    fun `test queries endpoint with specific count`() {
        val response = get("/queries?queries=5")
        assertEquals(200, response.statusCode())
        
        val json = Json.parseToJsonElement(response.body())
        assertTrue(json.toString().contains("\"id\""))
        assertTrue(json.toString().contains("\"randomNumber\""))
    }
    
    @Test
    fun `test updates endpoint`() {
        val response = get("/updates?queries=3")
        assertEquals(200, response.statusCode())
        assertEquals("application/json", response.headers().firstValue("content-type").orElse(""))
        
        val json = Json.parseToJsonElement(response.body())
        assertTrue(json.toString().contains("\"id\""))
        assertTrue(json.toString().contains("\"randomNumber\""))
    }
    
    @Test
    fun `test fortunes endpoint`() {
        val response = get("/fortunes")
        assertEquals(200, response.statusCode())
        
        val contentType = response.headers().firstValue("content-type").orElse("")
        assertTrue(contentType.startsWith("text/html"))
        
        val body = response.body()
        assertTrue(body.contains("<!DOCTYPE html>"))
        assertTrue(body.contains("<table>"))
        assertTrue(body.contains("Fortunes"))
        
        // Verify XSS protection - script tag should be escaped
        assertTrue(body.contains("&lt;script&gt;") || body.contains("escaped"))
    }
    
    @Nested
    @DisplayName("Load Testing")
    inner class LoadTests {
        
        @Test
        fun `load test JSON endpoint`() {
            performLoadTest("/json", requests = 1000, concurrency = 10)
        }
        
        @Test
        fun `load test db endpoint`() {
            performLoadTest("/db", requests = 500, concurrency = 10)
        }
        
        @Test
        fun `load test queries endpoint`() {
            performLoadTest("/queries?queries=5", requests = 300, concurrency = 5)
        }
        
        private fun performLoadTest(path: String, requests: Int, concurrency: Int) {
            println("\n=== Load Testing $path ===")
            println("Requests: $requests, Concurrency: $concurrency")
            
            val startTime = System.currentTimeMillis()
            var successCount = 0
            var errorCount = 0
            val latencies = mutableListOf<Long>()
            
            // Simple concurrent load test
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
                                }
                            }
                        } catch (e: Exception) {
                            synchronized(latencies) {
                                errorCount++
                            }
                        }
                    }
                }
            }
            
            threads.forEach { it.start() }
            threads.forEach { it.join() }
            
            val totalTime = System.currentTimeMillis() - startTime
            val avgLatency = latencies.average()
            val p50 = latencies.sorted()[latencies.size / 2]
            val p95 = latencies.sorted()[(latencies.size * 0.95).toInt()]
            val p99 = latencies.sorted()[(latencies.size * 0.99).toInt()]
            val throughput = (successCount.toDouble() / totalTime) * 1000
            
            println("Total time: ${totalTime}ms")
            println("Success: $successCount, Errors: $errorCount")
            println("Throughput: ${"%.2f".format(throughput)} req/s")
            println("Latency - Avg: ${"%.2f".format(avgLatency)}ms, P50: ${p50}ms, P95: ${p95}ms, P99: ${p99}ms")
            
            assertTrue(successCount > 0, "Should have at least some successful requests")
            assertTrue(errorCount < successCount * 0.1, "Error rate should be less than 10%")
        }
    }
}
