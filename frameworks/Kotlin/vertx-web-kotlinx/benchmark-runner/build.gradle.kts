plugins {
    id("buildlogic.kotlin-application-conventions")
}

dependencies {
    implementation(project(":with-db:default"))
    implementation(project(":with-db:with-db-common"))
    implementation(project(":common"))
    
    // Vert.x dependencies
    implementation(platform(libs.vertx.stack.depchain))
    implementation(libs.vertx.web)
    implementation(libs.vertx.pgClient)
    implementation(libs.vertx.lang.kotlin)
    implementation(libs.vertx.lang.kotlin.coroutines)
    
    // Testcontainers
    implementation("org.testcontainers:testcontainers:1.20.4")
    implementation("org.testcontainers:postgresql:1.20.4")
    
    // Kotlin coroutines
    implementation(libs.kotlinx.coroutines.core)
}

application.mainClass.set("BenchmarkRunnerKt")
