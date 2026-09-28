package net.thunderbird.core.featureflag.serialization

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsOnly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import assertk.assertions.key
import assertk.assertions.messageContains
import assertk.assertions.prop
import kotlin.test.Test
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import net.thunderbird.core.featureflag.model.AppVariantOverridesRawType
import net.thunderbird.core.featureflag.model.BaseAppVariantOverrides
import net.thunderbird.core.featureflag.model.FlagAttributeType
import net.thunderbird.core.featureflag.model.FlagRegistry

class DefaultFeatureFlagCatalogJsonParserTest {

    @Test
    fun `decodeFromString should decode the catalog version flags and overrides`() {
        // Arrange
        // language=json
        val rawJson = """
            {
              "version": "2026-07-30.1",
              "flags": [
                { "key": "first_flag", "default": true },
                { "key": "second_flag", "default": false }
              ],
              "overrides": {
                "thunderbird": { "debug": { "second_flag": true }, "release": {} },
                "k9": { "debug": {}, "release": {} }
              }
            }
        """.trimIndent()
        val testSubject = createTestSubject()

        // Act
        val catalog = testSubject.decodeFromString(rawJson)

        // Assert
        assertThat(catalog.version).isEqualTo("2026-07-30.1")
        assertThat(catalog.flags.map(FlagRegistry::key)).containsExactly("first_flag", "second_flag")
        assertThat(catalog.overrides.thunderbird).all {
            key("debug").containsOnly("second_flag" to true)
            key("release").isEmpty()
        }
        assertThat(catalog.overrides.k9).all {
            key("debug").isEmpty()
            key("release").isEmpty()
        }
    }

    @Test
    fun `decodeFromString should route each application override section to its own factory`() {
        // Arrange
        // language=json
        val rawJson = """
            {
              "version": "test-version",
              "flags": [],
              "overrides": {
                "thunderbird": { "debug": { "thunderbird_only_flag": true } },
                "k9": { "debug": { "k9_only_flag": true } }
              }
            }
        """.trimIndent()
        val testSubject = createTestSubject()

        // Act
        val catalog = testSubject.decodeFromString(rawJson)

        // Assert
        assertThat(catalog.overrides.thunderbird).isInstanceOf<FakeThunderbirdOverrides>()
        assertThat(catalog.overrides.k9).isInstanceOf<FakeK9Overrides>()
        assertThat(catalog.overrides.thunderbird).key("debug").containsOnly("thunderbird_only_flag" to true)
        assertThat(catalog.overrides.k9).key("debug").containsOnly("k9_only_flag" to true)
    }

    @Test
    fun `decodeFromString should apply defaults when optional flag fields are omitted`() {
        // Arrange
        // language=json
        val rawJson = """
            {
              "version": "test-version",
              "flags": [
                { "key": "minimal_flag", "default": true }
              ],
              "overrides": { "thunderbird": {}, "k9": {} }
            }
        """.trimIndent()
        val testSubject = createTestSubject()

        // Act
        val catalog = testSubject.decodeFromString(rawJson)

        // Assert
        assertThat(catalog.flags).hasSize(1)
        assertThat(catalog.flags.first()).all {
            prop(FlagRegistry::key).isEqualTo("minimal_flag")
            prop(FlagRegistry::default).isTrue()
            prop(FlagRegistry::description).isNull()
            prop(FlagRegistry::type).isEqualTo(FlagAttributeType.Boolean)
            prop(FlagRegistry::timeToPromote).isNull()
        }
    }

    @Test
    fun `decodeFromString should decode optional flag fields when present`() {
        // Arrange
        // language=json
        val rawJson = """
            {
              "version": "test-version",
              "flags": [
                {
                  "key": "complete_flag",
                  "default": false,
                  "description": "A fully described flag.",
                  "type": "boolean",
                  "time_to_promote": "2026-12-31"
                }
              ],
              "overrides": { "thunderbird": {}, "k9": {} }
            }
        """.trimIndent()
        val testSubject = createTestSubject()

        // Act
        val catalog = testSubject.decodeFromString(rawJson)

        // Assert
        assertThat(catalog.flags.first()).all {
            prop(FlagRegistry::key).isEqualTo("complete_flag")
            prop(FlagRegistry::default).isFalse()
            prop(FlagRegistry::description).isEqualTo("A fully described flag.")
            prop(FlagRegistry::type).isEqualTo(FlagAttributeType.Boolean)
            prop(FlagRegistry::timeToPromote).isEqualTo("2026-12-31")
        }
    }

    @Test
    fun `decodeFromString should ignore unknown keys at the catalog level`() {
        // Arrange
        // language=json
        val rawJson = $$"""
            {
              "$schema": "thunderbird_mobile_featureflag.schema.json",
              "version": "test-version",
              "flags": [
                { "key": "first_flag", "default": true }
              ],
              "overrides": { "thunderbird": {}, "k9": {} }
            }
        """.trimIndent()
        val testSubject = createTestSubject()

        // Act
        val catalog = testSubject.decodeFromString(rawJson)

        // Assert
        assertThat(catalog.flags.map(FlagRegistry::key)).containsExactly("first_flag")
    }

    @Test
    fun `decodeFromString should fail when the json is syntactically invalid`() {
        // Arrange
        // language=json
        val malformedJson = """
            {
              "version": "test-version",
              "flags": [
                { "key": "broken_flag", "default": }
              ]
        """.trimIndent()
        val testSubject = createTestSubject()

        // Act & Assert
        assertFailure { testSubject.decodeFromString(malformedJson) }
            .isInstanceOf<SerializationException>()
            .messageContains("Unexpected JSON token")
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `decodeFromString should fail when a required flag field is missing`() {
        // Arrange
        // language=json
        val rawJson = """
            {
              "version": "test-version",
              "flags": [
                { "key": "flag_without_default" }
              ],
              "overrides": { "thunderbird": {}, "k9": {} }
            }
        """.trimIndent()
        val testSubject = createTestSubject()

        // Act & Assert
        assertFailure { testSubject.decodeFromString(rawJson) }
            .isInstanceOf<MissingFieldException>()
            .messageContains("default")
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `decodeFromString should fail when an application override section is missing`() {
        // Arrange
        // language=json
        val rawJson = """
            {
              "version": "test-version",
              "flags": [],
              "overrides": { "thunderbird": {} }
            }
        """.trimIndent()
        val testSubject = createTestSubject()

        // Act & Assert
        assertFailure { testSubject.decodeFromString(rawJson) }
            .isInstanceOf<MissingFieldException>()
            .messageContains("k9")
    }
}

private fun createTestSubject(): DefaultFeatureFlagCatalogJsonParser = DefaultFeatureFlagCatalogJsonParser(
    registrySerializer = FlagRegistryOverrideSerializer(
        k9Factory = { wrapper -> FakeK9Overrides(wrapper) },
        thunderbirdFactory = { wrapper -> FakeThunderbirdOverrides(wrapper) },
    ),
)

/**
 * Distinct per-application fakes so the assertions can tell which factory the serializer handed each override
 * section to.
 */
private class FakeK9Overrides(wrapper: AppVariantOverridesRawType) : BaseAppVariantOverrides(wrapper)

private class FakeThunderbirdOverrides(wrapper: AppVariantOverridesRawType) : BaseAppVariantOverrides(wrapper)
