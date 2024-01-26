import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import kotlin.random.Random

@Serializable
class Message(val message: String)

val jsonSerializationMessage = Message("Hello, World!")
val messageSerializer = serializer<Message>()

@Serializable
data class World(val id: Int, val randomNumber: Int)

fun randomIntBetween1And10000() =
    Random.nextInt(1, 10001)

class Fortune(val id: Int, val message: String)
