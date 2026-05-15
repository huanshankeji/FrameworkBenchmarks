import database.*
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgConnection
import io.vertx.sqlclient.*

// `PgConnection`s as used in the "vertx" portion offers better performance than `PgPool`s.
class MainVerticle : CommonWithDbVerticle<PgConnection, Unit>(),
    CommonWithDbVerticleI.ParallelOrPipelinedSelectWorlds<PgConnection, Unit>,
    CommonWithDbVerticleI.WithoutTransaction<PgConnection> {
    lateinit var selectWorldQuery: PreparedQuery<RowSet<Row>>
    lateinit var selectFortuneQuery: PreparedQuery<RowSet<Row>>
    lateinit var updateWorldQuery: PreparedQuery<RowSet<Row>>

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
                pipeliningLimit = 1
            )
        ).coAwait().apply {
            selectWorldQuery = preparedQuery(SELECT_WORLD_SQL)
            selectFortuneQuery = preparedQuery(SELECT_FORTUNE_SQL)
            updateWorldQuery = preparedQuery(UPDATE_WORLD_SQL)
        }

    suspend fun <T> withTransaction(function: suspend (SqlConnection) -> T): T {
        val transaction = dbClient.begin().coAwait()
        return try {
            val result = function(dbClient)
            transaction.commit().coAwait()
            result
        } catch (e: Exception) {
            try {
                transaction.rollback().coAwait()
            } catch (rollbackE: Exception) {
                e.addSuppressed(rollbackE)
            }
            throw e
        }
    }

    override suspend fun Unit.selectWorld(id: Int) =
        withTransaction {
            it.preparedQuery(SELECT_WORLD_SQL).execute(Tuple.of(id)).coAwait()
                .single().toWorld()
        }

    override suspend fun Unit.selectFortunesInto(fortunes: MutableList<Fortune>) {
        withTransaction {
            it.preparedQuery(SELECT_FORTUNE_SQL).execute().coAwait()
                .mapTo(fortunes) { it.toFortune() }
        }
    }

    override suspend fun Unit.updateSortedWorlds(sortedWorlds: List<World>) {
        withTransaction {
            it.preparedQuery(UPDATE_WORLD_SQL).executeBatch(sortedWorlds.map { Tuple.of(it.randomNumber, it.id) }).coAwait()
        }
    }
}