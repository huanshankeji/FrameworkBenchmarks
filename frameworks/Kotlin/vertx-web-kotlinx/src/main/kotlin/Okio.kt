import io.vertx.core.streams.WriteStream
import io.vertx.kotlin.coroutines.coAwait
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.Sink
import okio.Timeout
import io.vertx.core.buffer.Buffer as VertxBuffer
import okio.Buffer as OkioBuffer

@JvmInline
value class VertxBufferWriteStreamSink(val writeStream: WriteStream<VertxBuffer>) : Sink {
    override fun write(source: OkioBuffer, byteCount: Long) {
        runBlocking {
            writeStream.write(VertxBuffer.buffer(source.readByteArray(byteCount))).coAwait()
        }
    }

    override fun flush() {}

    //private val timeout = Timeout()
    override fun timeout(): Timeout =
        Timeout.NONE //timeout

    override fun close() {
        writeStream.end()
    }
}

// not used currently
fun WriteStream<VertxBuffer>.toSink(): Sink =
    VertxBufferWriteStreamSink(this)


@JvmInline
value class VertxBufferSink(val vertxBuffer: VertxBuffer) : Sink {
    override fun write(source: Buffer, byteCount: Long) {
        vertxBuffer.appendBytes(source.readByteArray(byteCount))
    }

    override fun flush() {}

    override fun timeout(): Timeout =
        Timeout.NONE //timeout

    override fun close() {}
}

fun VertxBuffer.toSink(): Sink =
    VertxBufferSink(this)
