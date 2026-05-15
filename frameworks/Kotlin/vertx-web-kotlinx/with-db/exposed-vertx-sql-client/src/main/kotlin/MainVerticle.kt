import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.ExperimentalEvscApi
import com.huanshankeji.exposedvertxsqlclient.JdbcTransactionExposedTransactionProvider
import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import database.*
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgBuilder
import io.vertx.sqlclient.SqlClient
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.buildStatement
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.select

@OptIn(ExperimentalEvscApi::class)
class MainVerticle(val exposedDatabase: Database) : CommonWithDbVerticle<DatabaseClient<SqlClient>, Unit>(),
    CommonWithDbVerticleI.ParallelOrPipelinedSelectWorlds<DatabaseClient<SqlClient>, Unit>,
    CommonWithDbVerticleI.WithoutTransaction<DatabaseClient<SqlClient>> {

    override suspend fun initDbClient(): DatabaseClient<SqlClient> {
        // Parameters are copied from the "vertx-web" and "vertx" portions.
        val sqlClient = PgBuilder.client()
            .connectingTo(
                pgConnectOptionsOf(
                    database = DATABASE,
                    host = HOST,
                    user = USER,
                    password = PASSWORD,
                    cachePreparedStatements = true,
                    pipeliningLimit = 256
                )
            )
            .build()

        return DatabaseClient(
            sqlClient,
            PgDatabaseClientConfig(JdbcTransactionExposedTransactionProvider(exposedDatabase), validateBatch = false)
        )
    }

    override suspend fun Unit.selectWorld(id: Int): World =
        dbClient.executeQuery(jdbcSelectWorldWithIdQuery(id))
            .single().toWorld()

    override suspend fun Unit.updateSortedWorlds(sortedWorlds: List<World>) {
        dbClient.executeBatchUpdate(sortedWorlds.map { world ->
            buildStatement {
                WorldTable.update({ WorldTable.id eq world.id }) {
                    it[randomNumber] = world.randomNumber
                }
            }
        })
    }

    override suspend fun Unit.selectFortunesInto(fortunes: MutableList<Fortune>) {
        dbClient.executeQuery(with(FortuneTable) { select(id, message) })
            .mapTo(fortunes) { it.toFortune() }
    }
}