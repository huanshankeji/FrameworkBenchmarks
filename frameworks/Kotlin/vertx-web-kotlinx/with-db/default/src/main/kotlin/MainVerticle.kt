import database.*
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.kotlin.pgclient.pgConnectOptionsOf
import io.vertx.pgclient.PgBuilder
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple

// Using pipelined `SqlClient` created with `PgBuilder.client()` for better performance
class MainVerticle : CommonWithDbVerticle<SqlClient, Unit>(),
    CommonWithDbVerticleI.ParallelOrPipelinedSelectWorlds<SqlClient, Unit>,
    CommonWithDbVerticleI.WithoutTransaction<SqlClient> {

    override suspend fun initDbClient(): SqlClient =
        // Create a pipelined SQL client with parameters
        PgBuilder.client()
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

    override suspend fun Unit.selectWorld(id: Int) =
        dbClient.preparedQuery(SELECT_WORLD_SQL)
            .execute(Tuple.of(id)).coAwait().single().toWorld()

    override suspend fun Unit.selectFortunesInto(fortunes: MutableList<Fortune>) {
        dbClient.preparedQuery(SELECT_FORTUNE_SQL)
            .execute().coAwait().mapTo(fortunes) { it.toFortune() }
    }

    override suspend fun Unit.updateSortedWorlds(sortedWorlds: List<World>) {
        dbClient.preparedQuery(UPDATE_WORLD_SQL)
            .executeBatch(sortedWorlds.map { Tuple.of(it.randomNumber, it.id) }).coAwait()
    }
}