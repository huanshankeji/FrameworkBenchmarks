import database.*
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgConnection
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple

// `PgConnection`s as used in the "vertx" portion offers better performance than `PgPool`s.
class MainVerticle : CommonWithDbVerticle<PgConnection, Unit>(),
    CommonWithDbVerticleI.ParallelOrPipelinedSelectWorlds<PgConnection, Unit>,
    CommonWithDbVerticleI.WithoutTransaction<PgConnection> {
    lateinit var selectWorldQuery: PreparedQuery<RowSet<Row>>
    lateinit var selectFortuneQuery: PreparedQuery<RowSet<Row>>
    // Array of prepared queries for aggregated updates, indexed by (number of worlds - 1)
    private val aggregatedUpdateWorldQueries = arrayOfNulls<PreparedQuery<RowSet<Row>>>(MAX_QUERIES)

    override suspend fun initDbClient(): PgConnection =
        // Parameters are copied from the "vertx-web" and "vertx" portions.
        PgConnection.connect(
            vertx,
            pgConnectOptionsOf(
                database = DATABASE,
                host = HOST,
                user = USER,
                password = PASSWORD,
                cachePreparedStatements = true,
                pipeliningLimit = 256
            )
        ).coAwait().apply {
            selectWorldQuery = preparedQuery(SELECT_WORLD_SQL)
            selectFortuneQuery = preparedQuery(SELECT_FORTUNE_SQL)
            // Pre-prepare aggregated update queries for all possible batch sizes (1-MAX_QUERIES)
            for (i in 1..MAX_QUERIES) {
                aggregatedUpdateWorldQueries[i - 1] = preparedQuery(buildAggregatedUpdateQuery(i))
            }
        }

    /**
     * Builds an aggregated UPDATE query that updates multiple rows in a single statement.
     * This is more efficient than using executeBatch as it reduces network round trips.
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


    override suspend fun Unit.selectWorld(id: Int) =
        selectWorldQuery.execute(Tuple.of(id)).coAwait()
            .single().toWorld()

    override suspend fun Unit.selectFortunesInto(fortunes: MutableList<Fortune>) {
        selectFortuneQuery.execute().coAwait()
            .mapTo(fortunes) { it.toFortune() }
    }

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
}