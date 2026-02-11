plugins {
    kotlin("jvm")
}

repositories {
    mavenLocal() // for SNAPSHOT dependencies
    mavenCentral()
}

kotlin.jvmToolchain(25)
