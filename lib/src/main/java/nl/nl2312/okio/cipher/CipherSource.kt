package nl.nl2312.okio.cipher

import okio.Buffer
import okio.ForwardingSource
import okio.Source
import javax.crypto.Cipher

/**
 * Accepts an encrypted [Source] and deciphers it on the fly.
 *
 * <p>Chunked and partial reading is supported. However, the deciphered output text is not complete until the [Source]
 * is drained. It is an error to attempt to (re-)use the supplied [cipher] during or after the streaming, without
 * explicitly calling its [Cipher.init] again.
 */
class CipherSource(
        source: Source,
        private val cipher: Cipher) : ForwardingSource(source) {

    private val sourceBuffer: Buffer = Buffer()
    private val decipheredBuffer: Buffer = Buffer()

    override fun read(sink: Buffer, bytesRequested: Long): Long {
        // Ask for full decodable blocks, with an extra block to check for the end
        val bytesToRead = cipher.blockSize * (1 + (bytesRequested / cipher.blockSize) +
                if (bytesRequested % cipher.blockSize > 0) 1 else 0)
        // Guard against Long overflow when requesting too many bytes
        if (bytesToRead < 0) throw IllegalArgumentException("bytesRequested > max allowed")

        var streamEnd = false
        while (sourceBuffer.size < bytesToRead && !streamEnd) {
            val bytesRead = super.read(sourceBuffer, bytesToRead - sourceBuffer.size)
            if (bytesRead < 0) {
                streamEnd = true
            }
        }

        // Decrypt all bytes we already buffered from the source
        val bytes = sourceBuffer.readByteArray()
        if (bytes.isNotEmpty()) {
            val decrypted = cipher.update(bytes)
            decipheredBuffer.write(decrypted)
        }
        if (streamEnd) {
            // Finalize (with padding) if we are at the end
            val remainder = cipher.doFinal()
            if (remainder != null) {
                decipheredBuffer.write(remainder)
            }
        }

        // Sink the requested number of bytes (or all that were still in the source)
        val bytesToReturn = bytesRequested.coerceAtMost(decipheredBuffer.size)
        sink.write(decipheredBuffer, bytesToReturn)

        // Return number of written deciphered bytes, or -1 if there is nothing more to decipher
        return if (bytesToReturn > 0) bytesToReturn else -1
    }

}
