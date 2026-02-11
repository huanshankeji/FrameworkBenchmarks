plugins {
    id("buildlogic.kotlin-application-conventions")
}

dependencies {
    implementation(project(":with-db:default"))
    implementation(project(":with-db:exposed-vertx-sql-client"))
    implementation(project(":with-db:with-db-common"))
    implementation(project(":common"))
    
    // Vert.x dependencies
    implementation(platform(libs.vertx.stack.depchain))
    implementation(libs.vertx.web)
    implementation(libs.vertx.pgClient)
    implementation(libs.vertx.lang.kotlin)
    implementation(libs.vertx.lang.kotlin.coroutines)
    
    // Testcontainers
    implementation("org.testcontainers:testcontainers:2.0.3")
    implementation("org.testcontainers:testcontainers-postgresql:2.0.3")
    
    // PostgreSQL JDBC driver (required for init scripts)
    runtimeOnly("org.postgresql:postgresql:42.7.5")

    // Kotlin coroutines
    implementation(libs.kotlinx.coroutines.core)
}

application.mainClass.set("BenchmarkRunnerKt")
