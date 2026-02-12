import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.StatementPreparationExposedTransactionProvider
import com.huanshankeji.exposedvertxsqlclient.postgresql.exposed.exposedDatabaseConnectPostgresql
import database.connectionConfig
import org.jetbrains.exposed.v1.core.InternalApi
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.core.transactions.withThreadLocalTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.sql.Connection

@OptIn(ExperimentalEvscApi::class, InternalApi::class)
suspend fun main() =
    commonRunVertxServer(
        "Vert.x-Web Kotlinx with Exposed Vert.x SQL Client (and PostgreSQL)",
        {
            val database = connectionConfig.exposedDatabaseConnectPostgresql()
            object : StatementPreparationExposedTransactionProvider {
                override fun <T> statementPreparationExposedTransaction(block: Transaction.() -> T): T {
                    val transaction = transaction(database, Connection.TRANSACTION_READ_UNCOMMITTED, true) { this }
                    return withThreadLocalTransaction(transaction) { transaction.block() }
                }

                override fun <T> withExplicitOnlyStatementPreparationExposedTransaction(block: Transaction.() -> T): T {
                    TODO("Not yet implemented")
                }
            }
        },
        ::MainVerticle
    )
