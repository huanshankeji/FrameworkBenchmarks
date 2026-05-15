import database.*
import io.vertx.core.Promise
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgBuilder
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.SqlConnection
import io.vertx.sqlclient.Tuple
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// `PgConnection`s as used in the "vertx" portion offers better performance than `PgPool`s.
class MainVerticle : CommonWithDbVerticle<Pool, Unit>(),
    CommonWithDbVerticleI.ParallelOrPipelinedSelectWorlds<Pool, Unit>,
    CommonWithDbVerticleI.WithoutTransaction<Pool> {

    override suspend fun initDbClient(): Pool =
        // Parameters are copied from the "vertx-web" and "vertx" portions.
        PgBuilder.pool()
            .using(vertx)
            .connectingTo(
                pgConnectOptionsOf(
                    database = DATABASE,
                    host = HOST,
                    user = USER,
                    password = PASSWORD,
                    cachePreparedStatements = true,
                    //pipeliningLimit = 256
                )
            )
            .build()

    /*
    suspend fun <T> withTransaction(function: suspend (SqlConnection) -> T): T {
        val transaction = dbClient.begin().coAwait()
        return try {
            function(dbClient)
        } catch (e: Exception) {
            try {
                transaction.rollback().coAwait()
            } catch (rollbackE: Exception) {
                e.addSuppressed(rollbackE)
            }
            throw e
        }
    }
    */

    suspend fun <T> withTransaction(function: suspend (SqlConnection) -> T): T =
        dbClient.withTransaction {
            val promise = Promise.promise<T>()
            launch(Dispatchers.Unconfined) {
                try {
                    promise.complete(function(it))
                } catch (t: Throwable) {
                    promise.fail(t)
                }
            }
            promise.future()
        }.coAwait()


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
            it.preparedQuery(UPDATE_WORLD_SQL)
                .executeBatch(sortedWorlds.map { Tuple.of(it.randomNumber, it.id) }).coAwait()
        }
    }
}