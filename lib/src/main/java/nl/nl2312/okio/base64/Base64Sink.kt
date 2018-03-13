package nl.nl2312.okio.base64

import okio.Buffer
import okio.ForwardingSink
import okio.Sink

/**
 * Encodes writes to a [Sink] on the fly using Base64.
 *
 * <p>Chunked writing is unsupported; the written Base64 output is always finalised. However, partial writing (not
 * draining the full source) is supported.
 */
class Base64Sink(delegate: Sink) : ForwardingSink(delegate) {

    override fun write(source: Buffer, byteCount: Long) {
        // Read the requested number of bytes (or all available) from source
        val bytesToRead = byteCount.coerceAtMost(source.size())
        val decoded = source.readByteString(bytesToRead)

        // Base64-encode
        val encoded = decoded.base64()

        val encodedSink = Buffer()
        encodedSink.writeUtf8(encoded)
        super.write(encodedSink, encodedSink.size())
    }

}