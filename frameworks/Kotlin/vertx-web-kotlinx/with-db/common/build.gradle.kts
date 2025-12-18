plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":common"))
    
    // Vert.x
    implementation(libs.vertx.pg.client)
    implementation(libs.vertx.lang.kotlin)
    implementation(libs.vertx.lang.kotlin.coroutines)
}
