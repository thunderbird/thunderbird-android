package com.fsck.k9.net.ssl

import android.os.Build.VERSION_CODES
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class DefaultTrustedCertificateProviderTest {

    @Test
    @Config(sdk = [VERSION_CODES.N, VERSION_CODES.M, VERSION_CODES.LOLLIPOP_MR1, VERSION_CODES.LOLLIPOP])
    fun `should return certificates on pre-N devices`() {
        // Given
        val provider = DefaultTrustedCertificateProvider()

        // When
        val result = provider.getCertificates()

        // Then
        assertThat(result.size).isEqualTo(2)
    }

    @Test
    @Config(sdk = [VERSION_CODES.N_MR1, VERSION_CODES.O])
    fun `should return empty list on N_MR1 and later devices`() {
        // Given
        val provider = DefaultTrustedCertificateProvider()

        // When
        val result = provider.getCertificates()

        // Then
        assertThat(result).isEqualTo(emptyList())
    }
}
