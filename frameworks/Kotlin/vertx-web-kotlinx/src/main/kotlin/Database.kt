import io.vertx.sqlclient.Row

const val SELECT_WORLD_SQL = "SELECT id, randomnumber from WORLD WHERE id = $1"
const val SELECT_WORLD_WHERE_EQ_ANY_SQL = "SELECT id, randomnumber from WORLD WHERE id = ANY ($1)"
const val UPDATE_WORLD_SQL = "UPDATE world SET randomnumber = $1 WHERE id = $2"
const val SELECT_FORTUNE_SQL = "SELECT id, message FROM fortune"


fun Row.toWorld() =
    World(getInteger(0), getInteger(1))

fun Row.toFortune() =
    Fortune(getInteger(0), getString(1))
