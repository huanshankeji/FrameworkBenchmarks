plugins {
    id("vertx-web-kotlinx.kotlin-common-conventions")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":with-db:common"))
}

// This module is a placeholder for future R2DBC support
