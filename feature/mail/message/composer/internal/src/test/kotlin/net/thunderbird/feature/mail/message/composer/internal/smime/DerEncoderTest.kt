package net.thunderbird.feature.mail.message.composer.internal.smime

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.math.BigInteger
import kotlin.test.Test

class DerEncoderTest {

    @Test
    fun `encodeOid should encode sha256 OID according to X690`() {
        // Act
        val encoded = DerEncoder.encodeOid("2.16.840.1.101.3.4.2.1")

        // Assert
        assertThat(encoded.toHexString()).isEqualTo("0609608648016503040201")
    }

    @Test
    fun `encodeOid should encode id-signedData OID according to X690`() {
        // Act
        val encoded = DerEncoder.encodeOid("1.2.840.113549.1.7.2")

        // Assert
        assertThat(encoded.toHexString()).isEqualTo("06092a864886f70d010702")
    }

    @Test
    fun `encodeInteger should pad high bit positive integer with leading zero`() {
        // Act
        val encoded = DerEncoder.encodeInteger(BigInteger.valueOf(128))

        // Assert
        assertThat(encoded.toHexString()).isEqualTo("02020080")
    }

    @Test
    fun `encodeOctetString should use long-form length for payloads over 127 bytes`() {
        // Arrange
        val payload = ByteArray(260) { 0x41 }

        // Act
        val encoded = DerEncoder.encodeOctetString(payload)

        // Assert
        assertThat(encoded[0]).isEqualTo(0x04.toByte())
        assertThat(encoded[1]).isEqualTo(0x82.toByte())
        assertThat(encoded[2]).isEqualTo(0x01.toByte())
        assertThat(encoded[3]).isEqualTo(0x04.toByte())
        assertThat(encoded.size).isEqualTo(4 + 260)
    }

    @Test
    fun `encodeSet should sort elements in lexicographical unsigned byte order`() {
        // Arrange
        val elementA = DerEncoder.encodeInteger(1)
        val elementB = DerEncoder.encodeOctetString(byteArrayOf(0x01))

        // Act
        val encoded = DerEncoder.encodeSet(listOf(elementB, elementA))

        // Assert (INTEGER tag 0x02 sorts before OCTET STRING tag 0x04)
        assertThat(encoded.toHexString()).isEqualTo("3106020101040101")
    }

    @Test
    fun `encodeTime should use UTCTime for dates between 1950 and 2049`() {
        // Arrange: 2026-01-02T03:04:05Z = 1767323045000L
        val epochMillis = 1_767_323_045_000L

        // Act
        val encoded = DerEncoder.encodeTime(epochMillis)

        // Assert
        assertThat(encoded[0]).isEqualTo(0x17.toByte())
        assertThat(String(encoded, 2, encoded.size - 2, Charsets.US_ASCII)).isEqualTo("260102030405Z")
    }

    @Test
    fun `encodeTime should use GeneralizedTime for dates from 2050 onwards`() {
        // Arrange: 2050-01-01T00:00:00Z = 2524608000000L
        val epochMillis = 2_524_608_000_000L

        // Act
        val encoded = DerEncoder.encodeTime(epochMillis)

        // Assert
        assertThat(encoded[0]).isEqualTo(0x18.toByte())
        assertThat(String(encoded, 2, encoded.size - 2, Charsets.US_ASCII)).isEqualTo("20500101000000Z")
    }

    private fun ByteArray.toHexString(): String = joinToString(separator = "") { "%02x".format(it) }
}
