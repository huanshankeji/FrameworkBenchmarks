import kotlinx.serialization.Serializable

@Serializable
class Message(val message: String)

val jsonSerializationMessage = Message("Hello, World!")

@Serializable
data class World(val id: Int, val randomNumber: Int)

class Fortune(val id: Int, val message: String)
