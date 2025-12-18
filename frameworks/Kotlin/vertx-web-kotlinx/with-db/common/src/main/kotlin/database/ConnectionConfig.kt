package database

object DatabaseConfig {
    const val HOST = "tfb-database"
    const val DATABASE = "hello_world"
    const val USER = "benchmarkdbuser"
    const val PASSWORD = "benchmarkdbpass"
    const val CACHE_PREPARED_STATEMENTS = true
    const val PIPELINING_LIMIT = 256
}
