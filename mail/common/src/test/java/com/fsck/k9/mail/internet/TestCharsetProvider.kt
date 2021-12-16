package com.fsck.k9.mail.internet

import com.ibm.icu.charset.CharsetProviderICU
import java.nio.charset.Charset
import java.nio.charset.spi.CharsetProvider

/**
 * CharsetProvider that adds the "ISO-2022-JP-TEST" charset.
 *
 * The "ISO-2022-JP" decoder on the JVM is more lenient than the ICU4J decoder that is used on Android. For tests we
 * use the ICU4J implementation that is also used on Android.
 */
class TestCharsetProvider : CharsetProvider() {
    private val icuCharsetProvider = CharsetProviderICU()
    private val charset = icuCharsetProvider.charsetForName("ISO-2022-JP")

    override fun charsets(): Iterator<Charset> {
        return listOf(charset).iterator()
    }

    override fun charsetForName(charsetName: String?): Charset? {
        return if (charsetName?.equals("ISO-2022-JP-TEST", ignoreCase = true) == true) {
            charset
        } else {
            null
        }
    }
}
