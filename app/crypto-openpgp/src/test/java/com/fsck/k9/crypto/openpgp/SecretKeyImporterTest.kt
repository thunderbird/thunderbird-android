package com.fsck.k9.crypto.openpgp

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.InputStream
import java.security.Security

class SecretKeyImporterTest {
    private val secretKeyImporter = SecretKeyImporter()

    @Before
    fun setUp() {
        Security.addProvider(BouncyCastleProvider())
    }

    @Test
    fun findSecretKeyRings() {
        val inputStream = readFromResourceFile("secret_keyrings.sec.asc")

        val secretKeyInfoList = secretKeyImporter.findSecretKeyRings(inputStream)

        assertEquals(2, secretKeyInfoList.size)
        with(secretKeyInfoList[0]) {
            assertEquals("test1@mugenguild.com", secretKeyRing.primaryUserIdWithFallback)
            assertFalse(needsPassword)
        }
        with(secretKeyInfoList[1]) {
            assertEquals("test2@mugenguild.com", secretKeyRing.primaryUserIdWithFallback)
            assertTrue(needsPassword)
        }
    }

    private fun readFromResourceFile(name: String): InputStream {
        return javaClass.getResourceAsStream("/$name") ?: error("Resource not found: $name")
    }
}
