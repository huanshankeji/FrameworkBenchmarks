package database

const val SELECT_WORLD_SQL = "SELECT id, randomnumber FROM world WHERE id = $1"
const val UPDATE_WORLD_SQL = "UPDATE world SET randomnumber = $1 WHERE id = $2"
const val SELECT_FORTUNE_SQL = "SELECT id, message FROM fortune"
// Maximum number of queries allowed per request (matches CommonWithDbVerticle.getQueries())
const val MAX_QUERIES = 500
