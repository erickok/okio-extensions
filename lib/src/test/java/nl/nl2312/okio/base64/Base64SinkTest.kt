package nl.nl2312.okio.base64

import com.google.common.truth.Truth.assertThat
import okio.Buffer
import org.junit.Test

class Base64SinkTest {

    private val utf8String = "okio oh my¿¡"
    private val utf8Sink = Buffer().writeUtf8(utf8String)

    @Test
    fun write_fromFixedString() {
        val output = Buffer()
        val sink = Base64Sink(output)

        sink.write(utf8Sink, Long.MAX_VALUE)

        assertThat(output.readUtf8()).isEqualTo("b2tpbyBvaCBtecK/wqE=")
    }

    @Test
    fun write_partialWrite() {
        val output = Buffer()
        val sink = Base64Sink(output)

        // Request only to write the first 5 (decoded) characters
        sink.write(utf8Sink, 5)

        assertThat(output.readUtf8()).isEqualTo("b2tpbyA=")
    }

}