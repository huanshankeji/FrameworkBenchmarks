import com.huanshankeji.exposedvertxsqlclient.DatabaseClient
import com.huanshankeji.exposedvertxsqlclient.postgresql.PgDatabaseClientConfig
import com.huanshankeji.exposedvertxsqlclient.postgresql.vertx.pgclient.createPgConnection
import database.*
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.pgclient.PgConnection
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.select

class MainVerticle(val exposedDatabase: Database) : CommonWithDbVerticle<DatabaseClient<PgConnection>, Unit>(),
    CommonWithDbVerticleI.ParallelOrPipelinedSelectWorlds<DatabaseClient<PgConnection>, Unit>,
    CommonWithDbVerticleI.WithoutTransaction<DatabaseClient<PgConnection>> {
    // Array of prepared queries for aggregated updates, indexed by (number of worlds - 1)
    private val aggregatedUpdateWorldQueries = arrayOfNulls<PreparedQuery<RowSet<Row>>>(MAX_QUERIES)

    override suspend fun initDbClient(): DatabaseClient<PgConnection> {
        // Parameters are copied from the "vertx-web" and "vertx" portions.
        val pgConnection = createPgConnection(vertx, connectionConfig, {
            cachePreparedStatements = true
            pipeliningLimit = 256
        })
        // Pre-prepare aggregated update queries for all possible batch sizes (1-MAX_QUERIES)
        for (i in 1..MAX_QUERIES) {
            aggregatedUpdateWorldQueries[i - 1] = pgConnection.preparedQuery(buildAggregatedUpdateQuery(i))
        }
        return DatabaseClient(pgConnection, exposedDatabase, PgDatabaseClientConfig(validateBatch = false))
    }

    /**
     * Builds an aggregated UPDATE query that updates multiple rows in a single statement.
     * This is more efficient than using executeBatchUpdate as it reduces network round trips.
     * 
     * Example for len=2:
     * UPDATE world SET randomnumber = CASE id WHEN $1 THEN $2 WHEN $3 THEN $4 ELSE randomnumber END WHERE id IN ($1,$3)
     */
    private fun buildAggregatedUpdateQuery(len: Int): String {
        return buildString {
            append("UPDATE world SET randomnumber = CASE id")
            for (i in 0 until len) {
                val offset = (i * 2) + 1
                append(" WHEN $").append(offset).append(" THEN $").append(offset + 1)
            }
            append(" ELSE randomnumber END WHERE id IN ($1")
            for (i in 1 until len) {
                val offset = (i * 2) + 1
                append(",$").append(offset)
            }
            append(")")
        }
    }

    override suspend fun Unit.selectWorld(id: Int): World =
        dbClient.executeQuery(jdbcSelectWorldWithIdQuery(id))
            .single().toWorld()

    override suspend fun Unit.updateSortedWorlds(sortedWorlds: List<World>) {
        // Build parameters list: [id1, randomNumber1, id2, randomNumber2, ...]
        val params = mutableListOf<Int>()
        for (world in sortedWorlds) {
            params.add(world.id)
            params.add(world.randomNumber)
        }
        // Use the pre-prepared aggregated query for this batch size
        val query = aggregatedUpdateWorldQueries[sortedWorlds.size - 1]
            ?: error("No prepared query for batch size ${sortedWorlds.size}")
        query.execute(Tuple.wrap(params)).coAwait()
    }

    override suspend fun Unit.selectFortunesInto(fortunes: MutableList<Fortune>) {
        dbClient.executeQuery(with(FortuneTable) { select(id, message) })
            .mapTo(fortunes) { it.toFortune() }
    }
}