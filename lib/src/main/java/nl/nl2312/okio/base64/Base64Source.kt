package nl.nl2312.okio.base64

import okio.Buffer
import okio.ByteString
import okio.ForwardingSource
import okio.Source

/**
 * Accepts any Base64-encoded data [Source] and decodes it on the fly.
 *
 * <p>Chunked and partial reading is supported. Requests for a specific number of bytes are honored (until the source
 * is drained). Due to the nature of Base64 encoding, this means that for every 3 requested bytes, 4 bytes are read from
 * the source.
 */
class Base64Source(source: Source) : ForwardingSource(source) {

    private val sourceBuffer: Buffer = Buffer()
    private val decodeBuffer: Buffer = Buffer()

    override fun read(sink: Buffer, bytesRequested: Long): Long {
        if (bytesRequested >= MAX_REQUEST_LENGTH) throw IllegalArgumentException("bytesRequested > max allowed")

        // If we have the requested bytes already buffered, return directly
        if (decodeBuffer.size() >= bytesRequested) {
            sink.write(decodeBuffer, bytesRequested)
            return bytesRequested
        }

        var streamEnded = false
        while (decodeBuffer.size() < bytesRequested && !streamEnded) {
            val bytesRead = super.read(sourceBuffer, bytesRequested)
            if (bytesRead < 0) {
                streamEnded = true
            }

            // Decode all available blocks
            val allFullBlocks = BASE64_BLOCK * (sourceBuffer.size() / BASE64_BLOCK)
            val decoded: ByteString? = ByteString.decodeBase64(sourceBuffer.readUtf8(allFullBlocks))
            if (decoded == null) throw IllegalStateException("base64 decode failed")
            decodeBuffer.write(decoded)
        }

        // Return the requested number of bytes (or all that were available)
        val bytesToReturn = bytesRequested.coerceAtMost(decodeBuffer.size())
        sink.write(decodeBuffer, bytesToReturn)

        return if (streamEnded) -1 else bytesToReturn
    }

    companion object {

        const val MAX_REQUEST_LENGTH = 9223372036854775804L // 4 * (Long.MAX_VALUE / 4)
        const val BASE64_BLOCK = 4 // Read blocks of 4 bytes, which fix neatly 3 decoded bytes

    }

}
