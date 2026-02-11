package database

// Allow overriding via system properties for Testcontainers integration
val HOST = "tfb-database"
val PORT = 5432
val host get() = System.getProperty("db.host") ?: HOST
val port get() = System.getProperty("db.port")?.toIntOrNull() ?: PORT
const val USER = "benchmarkdbuser"
const val PASSWORD = "benchmarkdbpass"
const val DATABASE = "hello_world"
