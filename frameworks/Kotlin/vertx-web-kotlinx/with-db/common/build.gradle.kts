plugins {
    id("vertx-web-kotlinx.kotlin-common-conventions")
}

dependencies {
    api(project(":common"))
    
    // Vert.x
    implementation(libs.vertx.pg.client)
    implementation(libs.vertx.lang.kotlin)
    implementation(libs.vertx.lang.kotlin.coroutines)
}
