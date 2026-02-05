plugins {
    id("org.gradle.kotlin.kotlin-dsl") version "5.1.2"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.0")
}
