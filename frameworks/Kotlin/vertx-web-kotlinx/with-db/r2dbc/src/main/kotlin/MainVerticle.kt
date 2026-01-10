import database.*
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.spi.Connection
import io.r2dbc.spi.Readable
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactive.collect

/*
`ParallelOrPipelinedSelectWorlds` leads to `io.r2dbc.postgresql.client.ReactorNettyClient$RequestQueueException: [08006] Cannot exchange messages because the request queue limit is exceeded`.
https://github.com/pgjdbc/r2dbc-postgresql/issues/360#issuecomment-869422327 offers a workaround, but it doesn't seem like the officially recommended approach.
The PostgreSQL R2DBC driver doesn't seem to have full support for pipelining and multiplexing as discussed in https://github.com/pgjdbc/r2dbc-postgresql/pull/28.
 */
class MainVerticle : CommonWithDbVerticle<ConnectionPool, Connection>(),
    CommonWithDbVerticleI.SequentialSelectWorlds<ConnectionPool, Connection> {
    override suspend fun initDbClient(): ConnectionPool =
        // Use a pool size of 1 per verticle, similar to exposed-r2dbc
        connectionPool(1)

    override suspend fun stop() {
        dbClient.dispose()
    }

    override val httpServerStrictThreadMode get() = false
    //override val coHandlerCoroutineContext: CoroutineContext get() = EmptyCoroutineContext

    override suspend fun <T> withOptionalTransaction(block: suspend Connection.() -> T): T {
        val connection = dbClient.create().awaitSingle()
        return try {
            connection.block()
        } finally {
            connection.close().awaitFirst()
        }
    }

    override suspend fun Connection.selectWorld(id: Int): World =
        createStatement(SELECT_WORLD_SQL).bind(0, id).execute()
            .awaitSingle()
            .map(Readable::toWorld)
            .awaitSingle()

    override suspend fun Connection.updateSortedWorlds(sortedWorlds: List<World>) {
        // Begin transaction for the batch update
        beginTransaction().awaitFirst()
        try {
            for (world in sortedWorlds) {
                createStatement(UPDATE_WORLD_SQL)
                    .bind(0, world.randomNumber)
                    .bind(1, world.id)
                    .execute()
                    .awaitFirst()
                    .rowsUpdated
                    .awaitFirst()
            }
            commitTransaction().awaitFirst()
        } catch (e: Exception) {
            try {
                rollbackTransaction().awaitFirst()
            } catch (_: Exception) {
                // Ignore rollback errors to preserve the original exception
            }
            throw e
        }
    }

    override suspend fun Connection.selectFortunesInto(fortunes: MutableList<Fortune>) {
        createStatement(SELECT_FORTUNE_SQL).execute()
            .awaitSingle()
            .map(Readable::toFortune)
            //.asFlow().toList(fortunes)
            .collect(fortunes::add)
    }
}