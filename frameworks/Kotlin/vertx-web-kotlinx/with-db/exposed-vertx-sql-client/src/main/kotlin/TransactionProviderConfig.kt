import com.huanshankeji.exposedvertxsqlclient.JdbcTransactionExposedTransactionProvider
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Configuration for selecting which transaction provider to use in benchmarks.
 * This allows easy switching between different provider implementations for performance comparison.
 */
object TransactionProviderConfig {
    enum class ProviderType {
        JDBC,           // Use JdbcTransactionExposedTransactionProvider
        DATABASE        // Use DatabaseExposedTransactionProvider (if available)
    }
    
    /**
     * Current provider type - change this to switch implementations
     * Set via environment variable TRANSACTION_PROVIDER or defaults to JDBC
     */
    val currentProvider: ProviderType = System.getenv("TRANSACTION_PROVIDER")?.let {
        try {
            ProviderType.valueOf(it.uppercase())
        } catch (e: IllegalArgumentException) {
            println("Warning: Invalid TRANSACTION_PROVIDER value '$it', defaulting to JDBC")
            ProviderType.JDBC
        }
    } ?: ProviderType.JDBC
    
    /**
     * Creates the appropriate transaction provider based on configuration
     */
    fun createProvider(database: Database): Any {
        return when (currentProvider) {
            ProviderType.JDBC -> {
                println("Using JdbcTransactionExposedTransactionProvider")
                JdbcTransactionExposedTransactionProvider(database)
            }
            ProviderType.DATABASE -> {
                println("Using DatabaseExposedTransactionProvider")
                // Note: DatabaseExposedTransactionProvider is not available in the current library version
                // This would need to be implemented or a different alternative used
                // For now, fall back to JDBC
                println("Warning: DatabaseExposedTransactionProvider not available, using JDBC")
                JdbcTransactionExposedTransactionProvider(database)
            }
        }
    }
}
