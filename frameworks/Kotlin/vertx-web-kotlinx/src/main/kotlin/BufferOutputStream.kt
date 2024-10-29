import io.vertx.core.buffer.Buffer
import java.io.OutputStream

class BufferOutputStream(val buffer: Buffer) : OutputStream() {
    override fun write(b: Int) {
        buffer.appendByte(b.toByte())
    }

    override fun write(b: ByteArray) {
        buffer.appendBytes(b)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        buffer.appendBytes(b, off, len)
    }
}

fun Buffer.toOutputStream() =
    BufferOutputStream(this)
