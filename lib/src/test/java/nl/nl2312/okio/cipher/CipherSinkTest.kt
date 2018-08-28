package nl.nl2312.okio.cipher

import com.google.common.truth.Truth.assertThat
import okio.Buffer
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class CipherSinkTest {

    private val encodeCipher: Cipher
    private val decodeCipher: Cipher

    init {
        // Generate random secret key for the scope of these tests
        val secureRandom = SecureRandom()
        val key = ByteArray(16)
        secureRandom.nextBytes(key)
        val secretKeySpec = SecretKeySpec(key, "AES")
        val ivBytes = ByteArray(16)
        secureRandom.nextBytes(ivBytes)
        val iv = IvParameterSpec(ivBytes)

        // Encode some data to test with
        encodeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        encodeCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, iv)

        // Prepare decoding decodeCipher for our tests
        decodeCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        decodeCipher.init(Cipher.DECRYPT_MODE, secretKeySpec, iv)
    }

    @Test
    fun write_fromFixedString() {
        val output = Buffer()
        val cipheredSink = CipherSink(output, encodeCipher)

        val utf8Sink = Buffer().writeUtf8("okio oh my¿¡")
        cipheredSink.write(utf8Sink, Long.MAX_VALUE)

        val cipheredBytes = output.readByteArray()
        assertThat(String(cipheredBytes)).isNotEqualTo("okio oh my¿¡")
        val decipheredOutput = decodeCipher.doFinal(cipheredBytes)
        assertThat(String(decipheredOutput)).isEqualTo("okio oh my¿¡")
    }

    @Test
    fun write_partialWrite() {
        val output = Buffer()
        val cipheredSink = CipherSink(output, encodeCipher)

        val utf8Sink = Buffer().writeUtf8("okio oh my¿¡")

        // Encrypt first 5 bytes
        cipheredSink.write(utf8Sink, 5)
        val firstOutput = decodeCipher.doFinal(output.readByteArray())
        assertThat(String(firstOutput)).isEqualTo("okio ")

        // Encrypt another 5 bytes
        cipheredSink.write(utf8Sink, 5)
        val secondOutput = decodeCipher.doFinal(output.readByteArray())
        assertThat(String(secondOutput)).isEqualTo("oh my")

        // Asking to encrypt another 5 bytes will drain the sink
        cipheredSink.write(utf8Sink, 5)
        val finalOutput = decodeCipher.doFinal(output.readByteArray())
        assertThat(String(finalOutput)).isEqualTo("¿¡")
    }

    @Test(expected = BadPaddingException::class)
    fun write_chunkedWrite() {
        val output = Buffer()
        val cipheredSink = CipherSink(output, encodeCipher)

        val utf8Sink = Buffer().writeUtf8("okio oh my¿¡")

        // Encrypt first 5 bytes and then another 5 bytes
        cipheredSink.write(utf8Sink, 5)
        cipheredSink.write(utf8Sink, 5)

        // Throws BadPaddingException as the output is not a single cipher text but two individual ones
        decodeCipher.doFinal(output.readByteArray())
    }

}