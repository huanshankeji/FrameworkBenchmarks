plugins {
    id("buildlogic.kotlin-application-conventions")
}

dependencies {
    implementation(project(":with-db:with-db-common"))
    implementation(project(":with-db:r2dbc-common"))
    implementation(project(":with-db:exposed-common"))

    implementation(libs.exposed.r2dbc)
}

// Default main class (shared-pool-512-optimized)
application.mainClass.set("MainKt")

// Create start scripts for all 4 configurations
tasks.register<CreateStartScripts>("startScriptsSeparatePoolSize1") {
    mainClass.set("MainSeparatePoolSize1Kt")
    applicationName = "exposed-r2dbc-separate-pool-size-1"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

tasks.register<CreateStartScripts>("startScriptsSeparatePoolSize8") {
    mainClass.set("MainSeparatePoolSize8Kt")
    applicationName = "exposed-r2dbc-separate-pool-size-8"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

tasks.register<CreateStartScripts>("startScriptsSeparatePoolSize8Optimized") {
    mainClass.set("MainSeparatePoolSize8OptimizedKt")
    applicationName = "exposed-r2dbc-separate-pool-size-8-optimized"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

tasks.register<CreateStartScripts>("startScriptsSharedPool512Optimized") {
    mainClass.set("MainSharedPool512OptimizedKt")
    applicationName = "exposed-r2dbc-shared-pool-512-optimized"
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
}

tasks.named("startScripts") {
    dependsOn("startScriptsSeparatePoolSize1")
    dependsOn("startScriptsSeparatePoolSize8")
    dependsOn("startScriptsSeparatePoolSize8Optimized")
    dependsOn("startScriptsSharedPool512Optimized")
}

