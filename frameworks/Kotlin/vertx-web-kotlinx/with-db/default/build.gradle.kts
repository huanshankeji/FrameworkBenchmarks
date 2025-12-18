plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

dependencies {
    implementation(project(":common"))
    implementation(project(":with-db:common"))
    
    // Vert.x
    implementation(libs.vertx.web)
    implementation(libs.vertx.pg.client)
    implementation(libs.vertx.lang.kotlin)
    implementation(libs.vertx.lang.kotlin.coroutines)
    
    // Netty native transport
    implementation(libs.netty.transport.native.io.uring) {
        artifact {
            classifier = "linux-x86_64"
        }
    }
    
    // Kotlinx
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.html)
}

application {
    mainClass.set("MainKt")
}

tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
