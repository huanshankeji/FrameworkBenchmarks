plugins {
    id("org.jetbrains.kotlin.jvm")
}

repositories {
    mavenLocal() // for snapshot dependencies
    mavenCentral()
}

kotlin {
    jvmToolchain(25)
}
