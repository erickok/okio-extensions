package nl.nl2312.okio.cipher

import com.google.common.truth.Truth.assertThat
import nl.nl2312.okio.base64.Base64Source
import okio.Buffer
import okio.ByteString
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


class CipherSourceTest {

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
    fun read_emptySource() {
        val cipheredSource = Buffer()//.write(byteArrayOf())

        val decoded = CipherSource(cipheredSource, decodeCipher)

        val output = Buffer().also { it.writeAll(decoded) }
        assertThat(output.readUtf8()).isEqualTo("")
    }

    @Test
    fun read_cipheredArray_oneBlock() {
        // NOTE Input is shorter than 1 cipher block of 16 bytes
        val ciphered = encodeCipher.doFinal("okio oh my¿¡".toByteArray())
        val cipheredSource = Buffer().write(ciphered)

        val decoded = CipherSource(cipheredSource, decodeCipher)

        val output = Buffer().also { it.writeAll(decoded) }
        assertThat(output.readUtf8()).isEqualTo("okio oh my¿¡")
    }

    @Test
    fun read_cipheredArray_multipleBlocks() {
        // NOTE Input is longer than 1 cipher block of 16 bytes
        val ciphered = encodeCipher.doFinal("okio oh my¿¡ okio oh my¿¡ okio oh my¿¡".toByteArray())
        val cipheredSource = Buffer().write(ciphered)

        val decoded = CipherSource(cipheredSource, decodeCipher)

        val output = Buffer().also { it.writeAll(decoded) }
        assertThat(output.readUtf8()).isEqualTo("okio oh my¿¡ okio oh my¿¡ okio oh my¿¡")
    }

    @Test
    fun read_cipheredChunked() {
        val ciphered = encodeCipher.doFinal("okio oh my¿¡ okio oh my¿¡ okio oh my¿¡".toByteArray())
        val cipheredSource = Buffer().write(ciphered)

        val decoded = CipherSource(cipheredSource, decodeCipher)

        // First request 5 bytes
        val output = Buffer()
        decoded.read(output, 5)
        assertThat(output.readUtf8()).isEqualTo("okio ")
        // Another 10
        decoded.read(output, 10)
        assertThat(output.readUtf8()).isEqualTo("oh my¿¡ ")
        // Another 100 (but there are only 2 remaining)
        decoded.read(output, 100)
        assertThat(output.readUtf8()).isEqualTo("okio oh my¿¡ okio oh my¿¡")
    }

    @Test
    fun read_base64Wrapped() {
        val ciphered = encodeCipher.doFinal("okio oh my¿¡".toByteArray())
        val base64 = ByteString.encodeUtf8(ByteString.of(ciphered, 0, ciphered.size).base64())
        val base64Source = Buffer().write(base64)

        val decoded = CipherSource(Base64Source(base64Source), decodeCipher)

        val output = Buffer().also { it.writeAll(decoded) }
        assertThat(output.readUtf8()).isEqualTo("okio oh my¿¡")
    }

    @Test(expected = IllegalArgumentException::class)
    fun read_tooMuch() {
        val ciphered = encodeCipher.doFinal("okio oh my¿¡".toByteArray())
        val cipheredSource = Buffer().write(ciphered)

        val decoded = CipherSource(cipheredSource, decodeCipher)

        // Request more bytes than CipherSource can take due to buffer length
        val output = Buffer()
        decoded.read(output, Long.MAX_VALUE)
    }

}