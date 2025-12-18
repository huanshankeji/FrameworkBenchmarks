tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    repositories {
        mavenLocal() // for snapshot dependencies
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    
    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
        jvmToolchain(25)
    }
    
    dependencies {
        // All modules use these common dependencies
        val implementation by configurations
        implementation(platform(rootProject.libs.vertx.stack.depchain))
    }
}
