pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "vertx-web-kotlinx"

// Enable version catalog
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Include all modules
include("common")
include("without-db:default")
include("with-db:common")
include("with-db:default")
include("with-db:exposed-r2dbc")
include("with-db:exposed-vertx-sql-client")
