plugins {
    id("vertx-web-kotlinx.kotlin-application-conventions")
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":common"))
    
    // Vert.x
    implementation(libs.vertx.web)
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
}
