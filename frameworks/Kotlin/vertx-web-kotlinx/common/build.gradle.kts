plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    // Vert.x
    implementation(libs.vertx.web)
    implementation(libs.vertx.lang.kotlin)
    implementation(libs.vertx.lang.kotlin.coroutines)
    
    // Kotlinx
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.json.io)
    implementation(libs.kotlinx.io.core)
    implementation(libs.kotlinx.html)
    implementation(libs.kotlinx.datetime)
}
