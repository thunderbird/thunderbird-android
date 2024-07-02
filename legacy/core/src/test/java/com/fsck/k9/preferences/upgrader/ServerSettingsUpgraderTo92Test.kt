package com.fsck.k9.preferences.upgrader

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ServerSettingsUpgraderTo92Test {
    private val upgrader = ServerSettingsUpgraderTo92()

    @Test
    fun `STARTTLS_OPTIONAL should be rewritten to STARTTLS_REQUIRED`() {
        val mutableSettings = mutableMapOf<String, Any?>("connectionSecurity" to "STARTTLS_OPTIONAL")

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(mapOf<String, Any?>("connectionSecurity" to "STARTTLS_REQUIRED"))
    }

    @Test
    fun `SSL_TLS_OPTIONAL should be rewritten to SSL_TLS_REQUIRED`() {
        val mutableSettings = mutableMapOf<String, Any?>("connectionSecurity" to "SSL_TLS_OPTIONAL")

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(mapOf<String, Any?>("connectionSecurity" to "SSL_TLS_REQUIRED"))
    }

    @Test
    fun `NONE should be left unchanged`() {
        val settings = mapOf<String, Any?>("connectionSecurity" to "NONE")
        val mutableSettings = settings.toMutableMap()

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(settings)
    }

    @Test
    fun `STARTTLS_REQUIRED should be left unchanged`() {
        val settings = mapOf<String, Any?>("connectionSecurity" to "STARTTLS_REQUIRED")
        val mutableSettings = settings.toMutableMap()

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(settings)
    }

    @Test
    fun `SSL_TLS_REQUIRED should be left unchanged`() {
        val settings = mapOf<String, Any?>("connectionSecurity" to "SSL_TLS_REQUIRED")
        val mutableSettings = settings.toMutableMap()

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(settings)
    }

    @Test
    fun `Unsupported values should be changed to SSL_TLS_REQUIRED`() {
        val mutableSettings = mutableMapOf<String, Any?>("connectionSecurity" to "unsupported")

        val result = upgrader.upgrade(mutableSettings)

        assertThat(result).isEmpty()
        assertThat(mutableSettings).isEqualTo(mapOf<String, Any?>("connectionSecurity" to "SSL_TLS_REQUIRED"))
    }
}
