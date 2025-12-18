tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    // Only apply to projects that have plugins applied (not parent projects)
    plugins.withId("org.jetbrains.kotlin.jvm") {
        dependencies {
            // All modules use these common dependencies
            val implementation by configurations
            implementation(platform(rootProject.libs.vertx.stack.depchain))
        }
    }
}
