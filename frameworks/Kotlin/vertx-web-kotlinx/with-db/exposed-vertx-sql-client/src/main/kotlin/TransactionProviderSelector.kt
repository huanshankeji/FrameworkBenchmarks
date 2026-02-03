import com.huanshankeji.exposedvertxsqlclient.DatabaseExposedTransactionProvider
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.JdbcTransactionExposedTransactionProvider
import com.huanshankeji.exposedvertxsqlclient.StatementPreparationExposedTransactionProvider
import org.jetbrains.exposed.v1.jdbc.Database

/**
 * Configuration for selecting which transaction provider to use for profiling.
 * Set via TRANSACTION_PROVIDER environment variable: "jdbc" or "database"
 */
@OptIn(ExperimentalEvscApi::class)
object TransactionProviderSelector {
    enum class ProviderType {
        JDBC,
        DATABASE
    }
    
    fun getProviderType(): ProviderType {
        val envValue = System.getenv("TRANSACTION_PROVIDER")?.lowercase()
        return when (envValue) {
            "database" -> ProviderType.DATABASE
            else -> ProviderType.JDBC // default
        }
    }
    
    fun createProvider(exposedDatabase: Database): StatementPreparationExposedTransactionProvider {
        return when (getProviderType()) {
            ProviderType.JDBC -> {
                println("Using JdbcTransactionExposedTransactionProvider")
                JdbcTransactionExposedTransactionProvider(exposedDatabase)
            }
            ProviderType.DATABASE -> {
                println("Using DatabaseExposedTransactionProvider")
                DatabaseExposedTransactionProvider(exposedDatabase)
            }
        }
    }
}
