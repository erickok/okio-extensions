package nl.nl2312.okio.base64

import com.google.common.truth.Truth.assertThat
import okio.Buffer
import okio.ByteString
import org.junit.Test
import java.util.*

class Base64SourceTest {

    private val base64String = "b2tpbyBvaCBtecK/wqE=" // okio oh my¿¡
    private val base64StringSource = Buffer().writeUtf8(base64String)

    @Test
    fun read_fromFixedString() {
        val decoded = Base64Source(base64StringSource)

        val output = Buffer().also { it.writeAll(decoded) }
        assertThat(output.readUtf8()).isEqualTo("okio oh my¿¡")
    }

    @Test
    fun read_fromRadondomLongString() {
        // Generate a very long random string
        val randomLongByteArray =
                Random().let { random -> (0..10_000).map { (random.nextInt()).toByte() } }.toByteArray()
        val randomLongBase64 = ByteString.of(randomLongByteArray, 0, randomLongByteArray.size)
        val randomLongSource = Buffer().also { it.writeUtf8(randomLongBase64.base64()) }

        val decoded = Base64Source(randomLongSource)

        val output = Buffer().also { it.writeAll(decoded) }
        assertThat(output.readUtf8()).isEqualTo(randomLongBase64.utf8())
    }

    @Test
    fun read_partialRead() {
        val decoded = Base64Source(base64StringSource)
        val output = Buffer()

        // Request only the first 5 characters; this tests partial reading
        decoded.read(output, 5)
        assertThat(output.readUtf8()).isEqualTo("okio ")
    }

    @Test
    fun read_stopsOnSourceEnd() {
        val decoded = Base64Source(base64StringSource)
        val output = Buffer()

        // Request 1 byte at a time; this tests buffering
        do while (decoded.read(output, 1) > 0)
        assertThat(output.readUtf8()).isEqualTo("okio oh my¿¡")

        // Trying to read more returns no more bytes
        val readMore = decoded.read(output, 1)
        assertThat(readMore).isEqualTo(-1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun read_requestTooMuch() {
        val decoded = Base64Source(base64StringSource)
        val output = Buffer()

        // Request more bytes than Base64 can take due to buffer length
        decoded.read(output, Long.MAX_VALUE)
    }

}