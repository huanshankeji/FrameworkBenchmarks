plugins {
    id("buildlogic.kotlin-common-conventions")
}

dependencies {
    testImplementation(project(":with-db:default"))
    testImplementation(project(":with-db:with-db-common"))
    testImplementation(project(":common"))
    
    // Vert.x dependencies
    testImplementation(platform(libs.vertx.stack.depchain))
    testImplementation(libs.vertx.web)
    testImplementation(libs.vertx.pgClient)
    testImplementation(libs.vertx.lang.kotlin)
    testImplementation(libs.vertx.lang.kotlin.coroutines)
    
    // Testcontainers
    testImplementation("org.testcontainers:testcontainers:1.20.4")
    testImplementation("org.testcontainers:postgresql:1.20.4")
    
    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Kotlin test
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${libs.versions.kotlinx.coroutines.get()}")
    
    // JSON parsing
    testImplementation(libs.kotlinx.serialization.json)
}

tasks.test {
    useJUnitPlatform()
    
    // Set environment variables for tests
    environment("TESTCONTAINERS_RYUK_DISABLED", "false")
    
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
