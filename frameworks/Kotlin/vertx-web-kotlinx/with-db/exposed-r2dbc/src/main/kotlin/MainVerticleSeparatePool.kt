import database.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.statements.BatchUpdateStatement
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.statements.toExecutable
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/*
MainVerticle variant that creates its own connection pool per verticle instance.
Used for separate-pool benchmark configurations.
 */
class MainVerticleSeparatePool(
    private val poolSize: Int,
    private val useOptimizedConfig: Boolean
) : CommonWithDbVerticle<R2dbcDatabase, R2dbcTransaction>(),
    CommonWithDbVerticleI.SequentialSelectWorlds<R2dbcDatabase, R2dbcTransaction> {
    
    override suspend fun initDbClient(): R2dbcDatabase =
        if (useOptimizedConfig)
            r2dbcDatabaseConnectPoolOptimized(poolSize)
        else
            r2dbcDatabaseConnectPoolOriginal(poolSize)

    override val httpServerStrictThreadMode get() = false

    override suspend fun <T> withOptionalTransaction(block: suspend R2dbcTransaction.() -> T): T =
        suspendTransaction(dbClient) { block() }

    override suspend fun R2dbcTransaction.selectWorld(id: Int): World =
        r2dbcSelectWorldWithIdQuery(id).single().toWorld()

    override suspend fun R2dbcTransaction.updateSortedWorlds(sortedWorlds: List<World>) {
        val batch = BatchUpdateStatement(WorldTable)
        sortedWorlds.forEach { world ->
            batch.addBatch(EntityID(world.id, WorldTable))
            batch[WorldTable.randomNumber] = world.randomNumber
        }
        batch.toExecutable().execute(this)
    }

    override suspend fun R2dbcTransaction.selectFortunesInto(fortunes: MutableList<Fortune>) {
        FortuneTable.select(FortuneTable.id, FortuneTable.message)
            .map { it.toFortune() }.toList(fortunes)
    }
}
