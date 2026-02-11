package net.thunderbird.core.configstore.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.configstore.Config
import net.thunderbird.core.configstore.ConfigKey

class TestConfigBackendTest {

    private val booleanKey = ConfigKey.BooleanKey("boolean")
    private val intKey = ConfigKey.IntKey("int")
    private val stringKey = ConfigKey.StringKey("string")

    @Test
    fun `read should return initial config`() = runTest {
        val initialConfig = Config().apply {
            set(booleanKey, true)
        }
        val backend = TestConfigBackend(initialConfig)

        val config = backend.read(listOf(booleanKey)).first()

        assertThat(config[booleanKey]).isEqualTo(true)
    }

    @Test
    fun `update should modify config`() = runTest {
        val backend = TestConfigBackend()

        backend.update(listOf(intKey)) { config ->
            val newConfig = config.copy()
            newConfig[intKey] = 42
            newConfig
        }

        val config = backend.read(listOf(intKey)).first()
        assertThat(config[intKey]).isEqualTo(42)
    }

    @Test
    fun `clear should reset config`() = runTest {
        val initialConfig = Config().apply {
            set(stringKey, "value")
        }
        val backend = TestConfigBackend(initialConfig)

        backend.clear()

        val config = backend.read(listOf(stringKey)).first()
        assertThat(config[stringKey]).isNull()
    }

    @Test
    fun `readVersion should return 0 by default`() = runTest {
        val backend = TestConfigBackend()

        val version = backend.readVersion("version_key")

        assertThat(version).isEqualTo(0)
    }

    @Test
    fun `writeVersion should update version`() = runTest {
        val backend = TestConfigBackend()

        backend.writeVersion("version_key", 5)

        val version = backend.readVersion("version_key")
        assertThat(version).isEqualTo(5)
    }

    @Test
    fun `removeKeys should remove specified keys`() = runTest {
        val initialConfig = Config().apply {
            set(booleanKey, true)
            set(intKey, 42)
        }
        val backend = TestConfigBackend(initialConfig)

        backend.removeKeys(setOf(booleanKey))

        val config = backend.read(listOf(booleanKey, intKey)).first()
        assertThat(config[booleanKey]).isNull()
        assertThat(config[intKey]).isEqualTo(42)
    }
}
