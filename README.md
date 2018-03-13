# okio-extensions
A set of Okio extensions to work with various data formats.

```groovy
    allprojects {
        repositories {
            ...
            maven { url 'https://jitpack.io' }
        }
    }
    
    dependencies {
            compile 'com.github.erickok:okio-extensions:v1.0'
    }

```

Written in Kotlin, works on plain Java (JVM) and Android.

## `Base64Source` and `Base64Sink`

Stream [Base64](http://www.ietf.org/rfc/rfc2045.txt)-encoded bytes using Okio's own [Base64 implementation](https://github.com/square/okio/blob/master/okio/src/main/java/okio/Base64.java).

```kotlin
    val base64Source = Buffer().writeUtf8("b2tpbyBvaCBtecK/wqE=")

    val decodedSource = Base64Source(base64Source)

    val decodedString = Buffer().also { it.writeAll(decodedSource) }.readUtf8()
    assertThat(decodedString).isEqualTo("okio oh my¿¡")
```

```kotlin
    val utf8Sink = Buffer().writeUtf8("okio oh my¿¡")

    val base64Buffer = Buffer()
    val base64Sink = Base64Sink(base64Buffer)

    base64Sink.write(utf8Sink, Long.MAX_VALUE)

    assertThat(base64Buffer.readUtf8()).isEqualTo("b2tpbyBvaCBtecK/wqE=")
```

`Base64Source` supports partial and chunked reading, such as for decoding http chunked transfer mode responses in memory-efficient manner.

Think Apache Commons' [Base64InputStream and Base64OutputStream](https://commons.apache.org/proper/commons-codec/apidocs/org/apache/commons/codec/binary/package-summary.html) but for Okio `Source`s and `Sink`s.

## `CipherSource` and `CipherSink`

Encrypt and decrypt data in a streaming fashion using the Java platform's [Cipher](https://docs.oracle.com/javase/7/docs/api/javax/crypto/Cipher.html) API.

```kotlin
    val cipheredSource = ...

    val decodedSource = CipherSource(cipheredSource, decryptCipher)

    val output = Buffer().also { it.writeAll(decodedSource) }
    assertThat(output.readUtf8()).isEqualTo("okio oh my¿¡")
```

```kotlin
    val cipheredBuffer = Buffer()
    val cipheredSink = CipherSink(cipheredBuffer, encryptCipher)

    val utf8Sink = Buffer().writeUtf8("okio oh my¿¡")
    cipheredSink.write(utf8Sink, Long.MAX_VALUE)

    val deciphered = decodeCipher.doFinal(cipheredBuffer.readByteArray())
    assertThat(String(deciphered)).isEqualTo("okio oh my¿¡")
```

`CipherSource` supports chunked deciphering, such as for decrypting http chunked transfer mode responses on the fly.

Think javax.crypto's standard [CipherInputStream and CipherOutputStream](https://docs.oracle.com/javase/7/docs/api/javax/crypto/package-summary.html) but for Okio `Source`s and `Sink`s.

`CipherSource` and `CipherSink` are tested and used in production with AES ciphers, but should work (without warrenty) with any Java-supported Cipher algorithm.

## License and credits
Designed and developed by [Eric Kok](mailto:eric@2312.nl) of [2312 development](http://2312.nl).

    Copyright 2018 Eric Kok
    
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at
    
       http://www.apache.org/licenses/LICENSE-2.0
    
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
