import database.*
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgConnection
import io.vertx.sqlclient.PreparedQuery
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.Tuple
import io.vertx.sqlclient.impl.ArrayTuple
import java.util.concurrent.ConcurrentHashMap

// `PgConnection`s as used in the "vertx" portion offers better performance than `PgPool`s.
class MainVerticle : CommonWithDbVerticle<PgConnection, Unit>(),
    CommonWithDbVerticleI.ParallelOrPipelinedSelectWorlds<PgConnection, Unit>,
    CommonWithDbVerticleI.WithoutTransaction<PgConnection> {
    lateinit var selectWorldQuery: PreparedQuery<RowSet<Row>>
    lateinit var selectFortuneQuery: PreparedQuery<RowSet<Row>>
    private val updateWorldQueries = ConcurrentHashMap<Int, PreparedQuery<RowSet<Row>>>()

    private fun buildBatchUpdateSql(count: Int): String {
        var paramIndex = 1
        return buildString {
            append("UPDATE world SET randomnumber = CASE id ")
            repeat(count) {
                append("WHEN $").append(paramIndex++).append(" THEN $").append(paramIndex++).append(' ')
            }
            append("ELSE randomnumber END WHERE id IN (")
            repeat(count) {
                append("$").append(paramIndex++).append(',')
            }
            setLength(length - 1)
            append(')')
        }
    }

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
        }


    override suspend fun Unit.selectWorld(id: Int) =
        selectWorldQuery.execute(Tuple.of(id)).coAwait()
            .single().toWorld()

    override suspend fun Unit.selectFortunesInto(fortunes: MutableList<Fortune>) {
        selectFortuneQuery.execute().coAwait()
            .mapTo(fortunes) { it.toFortune() }
    }

    override suspend fun Unit.updateSortedWorlds(sortedWorlds: List<World>) {
        val updateQuery = updateWorldQueries.computeIfAbsent(sortedWorlds.size) {
            dbClient.preparedQuery(buildBatchUpdateSql(sortedWorlds.size))
        }
        val params = ArrayTuple(sortedWorlds.size * 3)
        sortedWorlds.forEach {
            params.addValue(it.id)
            params.addValue(it.randomNumber)
        }
        sortedWorlds.forEach { params.addValue(it.id) }
        updateQuery.execute(params).coAwait()
    }
}
