package nl.nl2312.okio.cipher

import okio.Buffer
import okio.ForwardingSink
import okio.Sink
import javax.crypto.Cipher

/**
 * Encrypts writes to a [Sink] on the fly given a pre-set [Cipher].
 *
 * <p>Chunked writing is unsupported; the written ciphered output is always finalised. However, partial writing (not
 * draining the full source) is supported.
 */
class CipherSink(
        delegate: Sink,
        private val cipher: Cipher) : ForwardingSink(delegate) {

    override fun write(source: Buffer, byteCount: Long) {
        // Read the requested number of bytes (or all available) from source
        val bytesToRead = byteCount.coerceAtMost(source.size())
        val decrypted = source.readByteArray(bytesToRead)

        // Encrypt
        val encrypted = cipher.doFinal(decrypted)

        val encryptedSink = Buffer()
        encryptedSink.write(encrypted)
        super.write(encryptedSink, encryptedSink.size())
    }

}