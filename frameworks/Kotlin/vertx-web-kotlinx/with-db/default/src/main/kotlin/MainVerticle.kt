import database.*
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgConnection
import io.vertx.sqlclient.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// `PgConnection`s as used in the "vertx" portion offers better performance than `PgPool`s.
class MainVerticle : CommonWithDbVerticle<PgConnection, Unit>(),
    CommonWithDbVerticleI.ParallelOrPipelinedSelectWorlds<PgConnection, Unit> {
    lateinit var selectWorldQuery: PreparedQuery<RowSet<Row>>
    lateinit var selectFortuneQuery: PreparedQuery<RowSet<Row>>
    lateinit var updateWorldQuery: PreparedQuery<RowSet<Row>>
    private val transactionMutex = Mutex()

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
                pipeliningLimit = 2048
            )
        ).coAwait().apply {
            selectWorldQuery = preparedQuery(SELECT_WORLD_SQL)
            selectFortuneQuery = preparedQuery(SELECT_FORTUNE_SQL)
            updateWorldQuery = preparedQuery(UPDATE_WORLD_SQL)
        }

    override suspend fun <T> withOptionalTransaction(block: suspend Unit.() -> T): T =
        transactionMutex.withLock {
            val transaction = dbClient.begin().coAwait()
            try {
                val result = Unit.block()
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
        selectWorldQuery.execute(Tuple.of(id)).coAwait()
            .single().toWorld()

    override suspend fun Unit.selectFortunesInto(fortunes: MutableList<Fortune>) {
        selectFortuneQuery.execute().coAwait()
            .mapTo(fortunes) { it.toFortune() }
    }

    override suspend fun Unit.updateSortedWorlds(sortedWorlds: List<World>) {
        updateWorldQuery.executeBatch(sortedWorlds.map { Tuple.of(it.randomNumber, it.id) }).coAwait()
    }
}
