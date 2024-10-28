import io.vertx.core.buffer.Buffer
import io.vertx.core.streams.WriteStream
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.runBlocking
import java.io.OutputStream

class VertxWriteStreamOutputStream(val writeStream: WriteStream<Buffer>) : OutputStream() {
    override fun write(b: Int) {
        runBlocking {
            writeStream.write(Buffer.buffer(4).apply {
                appendInt(b)
            }).coAwait()
        }
    }

    override fun write(b: ByteArray) {
        TODO()
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        TODO()
    }

    override fun close() {
        TODO()
    }

    override fun flush() {
        TODO()
    }
}

fun WriteStream<Buffer>.toOutputStream() =
    VertxWriteStreamOutputStream(this)
