package com.fsck.k9.preferences.upgrader

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ServerSettingsUpgraderTo94Test {
    private val upgrader = ServerSettingsUpgraderTo94()

    @Test
    fun `AUTOMATIC with STARTTLS_REQUIRED should be rewritten to PLAIN`() {
        val mutableSettings = mutableMapOf<String, Any?>(
            "authenticationType" to "AUTOMATIC",
            "connectionSecurity" to "STARTTLS_REQUIRED",
        )

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(
            mapOf<String, Any?>(
                "authenticationType" to "PLAIN",
                "connectionSecurity" to "STARTTLS_REQUIRED",
            ),
        )
    }

    @Test
    fun `AUTOMATIC with SSL_TLS_REQUIRED should be rewritten to PLAIN`() {
        val mutableSettings = mutableMapOf<String, Any?>(
            "authenticationType" to "AUTOMATIC",
            "connectionSecurity" to "SSL_TLS_REQUIRED",
        )

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(
            mapOf<String, Any?>(
                "authenticationType" to "PLAIN",
                "connectionSecurity" to "SSL_TLS_REQUIRED",
            ),
        )
    }

    @Test
    fun `AUTOMATIC with NONE should be rewritten to CRAM_MD5`() {
        val mutableSettings = mutableMapOf<String, Any?>(
            "authenticationType" to "AUTOMATIC",
            "connectionSecurity" to "NONE",
        )

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(
            mapOf<String, Any?>(
                "authenticationType" to "CRAM_MD5",
                "connectionSecurity" to "NONE",
            ),
        )
    }

    @Test
    fun `LOGIN should be rewritten to PLAIN`() {
        val mutableSettings = mutableMapOf<String, Any?>(
            "authenticationType" to "LOGIN",
            "connectionSecurity" to "SSL_TLS_REQUIRED",
        )

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(
            mapOf<String, Any?>(
                "authenticationType" to "PLAIN",
                "connectionSecurity" to "SSL_TLS_REQUIRED",
            ),
        )
    }

    @Test
    fun `PLAIN should not be rewritten`() {
        val settings = mapOf<String, Any?>(
            "authenticationType" to "PLAIN",
            "connectionSecurity" to "SSL_TLS_REQUIRED",
        )
        val mutableSettings = settings.toMutableMap()

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(settings)
    }

    @Test
    fun `EXTERNAL should not be rewritten`() {
        val settings = mapOf<String, Any?>(
            "authenticationType" to "EXTERNAL",
            "connectionSecurity" to "NONE",
        )
        val mutableSettings = settings.toMutableMap()

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(settings)
    }

    @Test
    fun `XOAUTH2 should not be rewritten`() {
        val settings = mapOf<String, Any?>(
            "authenticationType" to "XOAUTH2",
            "connectionSecurity" to "STARTTLS_REQUIRED",
        )
        val mutableSettings = settings.toMutableMap()

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(settings)
    }
}
