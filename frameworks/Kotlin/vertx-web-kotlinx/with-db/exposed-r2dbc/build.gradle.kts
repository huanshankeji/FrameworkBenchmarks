plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(project(":common"))
    implementation(project(":with-db:common"))
}

// This module is a placeholder for future R2DBC support
