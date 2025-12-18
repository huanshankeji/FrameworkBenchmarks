import common.Fortune
import common.World
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.select

// see "toolset/databases/postgres/create-postgres.sql"

object WorldTable : IdTable<Int>("world") {
    override val id = integer("id").entityId()

    // The name is "randomNumber" in "create-postgres.sql" but it's actually "randomnumber" in the test database.
    val randomNumber = integer("randomnumber").default(0)
}

object FortuneTable : IdTable<Int>("fortune") {
    override val id = integer("id").entityId()
    val message = varchar("message", 2048)
}

fun selectWorldWithIdQuery(id: Int) =
    WorldTable.select(WorldTable.id, WorldTable.randomNumber).where(WorldTable.id eq id)

fun ResultRow.toWorld() =
    World(this[WorldTable.id].value, this[WorldTable.randomNumber])

fun ResultRow.toFortune() =
    Fortune(this[FortuneTable.id].value, this[FortuneTable.message])
