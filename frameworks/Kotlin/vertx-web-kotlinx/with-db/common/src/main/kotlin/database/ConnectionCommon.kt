package database

// Allow overriding via system properties for Testcontainers integration
val HOST = System.getProperty("db.host") ?: "tfb-database"
val PORT = System.getProperty("db.port")?.toIntOrNull() ?: 5432
const val USER = "benchmarkdbuser"
const val PASSWORD = "benchmarkdbpass"
const val DATABASE = "hello_world"
