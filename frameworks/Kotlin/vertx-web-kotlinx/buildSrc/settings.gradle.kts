dependencyResolutionManagement {
    // Reuse version catalog from the main build.
    versionCatalogs {
        create("libs", { from(files("../gradle/libs.versions.toml")) })
    }
    
    // Allow insecure protocols for environments with SSL issues
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
            isAllowInsecureProtocol = false
        }
        gradlePluginPortal()
    }
}

rootProject.name = "buildSrc"
