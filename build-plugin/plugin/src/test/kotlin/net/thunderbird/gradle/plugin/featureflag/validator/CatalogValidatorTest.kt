package net.thunderbird.gradle.plugin.featureflag.validator

import assertk.Assert
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import net.thunderbird.gradle.plugin.featureflag.FeatureFlagCatalog
import net.thunderbird.gradle.plugin.featureflag.FlagOverrides
import net.thunderbird.gradle.plugin.featureflag.FlagRegistry
import net.thunderbird.gradle.plugin.featureflag.FlagRegistryOverrides
import net.thunderbird.gradle.plugin.featureflag.K9Overrides
import net.thunderbird.gradle.plugin.featureflag.ThunderbirdOverrides
import org.junit.Test

internal class CatalogValidatorTest {

    @Test
    fun `validate should return Success when catalog declares no override`() {
        // Arrange
        val catalog = catalog(flags = flags("archive_marks_as_read", "email_notifications"))
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isEqualTo(CatalogValidator.Result.Success)
    }

    @Test
    fun `validate should return Success when catalog declares neither flag nor override`() {
        // Arrange
        val catalog = catalog()
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isEqualTo(CatalogValidator.Result.Success)
    }

    @Test
    fun `validate should return Success when every override key is defined on flags and flips its default`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read", "email_notifications"),
            thunderbird = thunderbirdOverrides(
                debug = mapOf("archive_marks_as_read" to true),
                daily = mapOf("email_notifications" to true),
                beta = mapOf("archive_marks_as_read" to true, "email_notifications" to true),
                release = mapOf("email_notifications" to true),
            ),
            k9 = k9Overrides(
                debug = mapOf("archive_marks_as_read" to true),
                release = mapOf("email_notifications" to true),
            ),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isEqualTo(CatalogValidator.Result.Success)
    }

    @Test
    fun `validate should return Success when override value differs from the flag default`() {
        // Arrange
        val catalog = catalog(
            flags = listOf(FlagRegistry(key = "archive_marks_as_read", default = true)),
            thunderbird = thunderbirdOverrides(debug = mapOf("archive_marks_as_read" to false)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isEqualTo(CatalogValidator.Result.Success)
    }

    @Test
    fun `validate should return ValidationError when an override repeats an enabled flag default`() {
        // Arrange
        val catalog = catalog(
            flags = listOf(FlagRegistry(key = "archive_marks_as_read", default = true)),
            thunderbird = thunderbirdOverrides(daily = mapOf("archive_marks_as_read" to true)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.thunderbird.daily:
                  - Key 'archive_marks_as_read' has the same value as the default defined at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should return ValidationError when an override repeats a disabled flag default`() {
        // Arrange
        val catalog = catalog(
            flags = flags("email_notifications"),
            k9 = k9Overrides(release = mapOf("email_notifications" to false)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.k9.release:
                  - Key 'email_notifications' has the same value as the default defined at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should report missing keys before redundant keys of the same override`() {
        // Arrange
        val catalog = catalog(
            flags = listOf(FlagRegistry(key = "archive_marks_as_read", default = true)),
            k9 = k9Overrides(
                debug = mapOf(
                    "archive_marks_as_read" to true,
                    "unknown_flag" to false,
                ),
            ),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.k9.debug:
                  - Key 'unknown_flag' missing definition at '$.flags'
                  - Key 'archive_marks_as_read' has the same value as the default defined at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should return ValidationError when thunderbird debug override key is missing from flags`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read"),
            thunderbird = thunderbirdOverrides(debug = mapOf("unknown_flag" to true)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.thunderbird.debug:
                  - Key 'unknown_flag' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should return ValidationError when thunderbird daily override key is missing from flags`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read"),
            thunderbird = thunderbirdOverrides(daily = mapOf("unknown_flag" to true)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.thunderbird.daily:
                  - Key 'unknown_flag' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should return ValidationError when thunderbird beta override key is missing from flags`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read"),
            thunderbird = thunderbirdOverrides(beta = mapOf("unknown_flag" to true)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.thunderbird.beta:
                  - Key 'unknown_flag' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should return ValidationError when thunderbird release override key is missing from flags`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read"),
            thunderbird = thunderbirdOverrides(release = mapOf("unknown_flag" to true)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.thunderbird.release:
                  - Key 'unknown_flag' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should return ValidationError when k9 debug override key is missing from flags`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read"),
            k9 = k9Overrides(debug = mapOf("unknown_flag" to true)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.k9.debug:
                  - Key 'unknown_flag' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should return ValidationError when k9 release override key is missing from flags`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read"),
            k9 = k9Overrides(release = mapOf("unknown_flag" to true)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.k9.release:
                  - Key 'unknown_flag' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should report every missing key of an override in declaration order`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read"),
            k9 = k9Overrides(
                debug = mapOf(
                    "unknown_flag" to true,
                    "another_unknown_flag" to false,
                ),
            ),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.k9.debug:
                  - Key 'unknown_flag' missing definition at '$.flags'
                  - Key 'another_unknown_flag' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should report only the missing keys when an override mixes defined and missing keys`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read", "email_notifications"),
            thunderbird = thunderbirdOverrides(
                beta = mapOf(
                    "archive_marks_as_read" to true,
                    "unknown_flag" to false,
                    "email_notifications" to true,
                ),
            ),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.thunderbird.beta:
                  - Key 'unknown_flag' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should report every thunderbird override with missing keys separated by a blank line`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read"),
            thunderbird = thunderbirdOverrides(
                debug = mapOf("debug_only_flag" to true),
                daily = mapOf("daily_only_flag" to true),
                beta = mapOf("beta_only_flag" to true),
                release = mapOf("release_only_flag" to true),
            ),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.thunderbird.debug:
                  - Key 'debug_only_flag' missing definition at '$.flags'

                overrides.thunderbird.daily:
                  - Key 'daily_only_flag' missing definition at '$.flags'

                overrides.thunderbird.beta:
                  - Key 'beta_only_flag' missing definition at '$.flags'

                overrides.thunderbird.release:
                  - Key 'release_only_flag' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should report k9 errors before thunderbird errors when both apps have missing keys`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read"),
            thunderbird = thunderbirdOverrides(beta = mapOf("thunderbird_only_flag" to true)),
            k9 = k9Overrides(release = mapOf("k9_only_flag" to false)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.k9.release:
                  - Key 'k9_only_flag' missing definition at '$.flags'

                overrides.thunderbird.beta:
                  - Key 'thunderbird_only_flag' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should report the same missing key once per override that declares it`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read"),
            thunderbird = thunderbirdOverrides(
                debug = mapOf("unknown_flag" to true),
                daily = mapOf("unknown_flag" to false),
            ),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.thunderbird.debug:
                  - Key 'unknown_flag' missing definition at '$.flags'

                overrides.thunderbird.daily:
                  - Key 'unknown_flag' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should report every override key as missing when catalog declares no flag`() {
        // Arrange
        val catalog = catalog(
            thunderbird = thunderbirdOverrides(release = mapOf("archive_marks_as_read" to true)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.thunderbird.release:
                  - Key 'archive_marks_as_read' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should match override keys against flag keys case sensitively`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read"),
            k9 = k9Overrides(debug = mapOf("Archive_Marks_As_Read" to true)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.k9.debug:
                  - Key 'Archive_Marks_As_Read' missing definition at '$.flags'
            """.trimIndent(),
        )
    }

    @Test
    fun `validate should report a blank override key as missing`() {
        // Arrange
        val catalog = catalog(
            flags = flags("archive_marks_as_read"),
            thunderbird = thunderbirdOverrides(debug = mapOf("" to true)),
        )
        val testSubject = CatalogValidator()

        // Act
        val result = testSubject.validate(catalog = catalog)

        // Assert
        assertThat(result).isValidationError(
            message = """
                overrides.thunderbird.debug:
                  - Key '' missing definition at '$.flags'
            """.trimIndent(),
        )
    }
}

private fun Assert<CatalogValidator.Result>.isValidationError(message: String) =
    isInstanceOf<CatalogValidator.Result.Error.ValidationError>()
        .prop(CatalogValidator.Result.Error.ValidationError::message)
        .isEqualTo(message)

private fun flags(vararg keys: String): List<FlagRegistry> =
    keys.map { key -> FlagRegistry(key = key, default = false) }

private fun catalog(
    flags: List<FlagRegistry> = emptyList(),
    thunderbird: ThunderbirdOverrides = thunderbirdOverrides(),
    k9: K9Overrides = k9Overrides(),
): FeatureFlagCatalog = FeatureFlagCatalog(
    version = "2026-07-30.1",
    flags = flags,
    overrides = FlagRegistryOverrides(thunderbird = thunderbird, k9 = k9),
)

private fun thunderbirdOverrides(
    debug: FlagOverrides = emptyMap(),
    daily: FlagOverrides = emptyMap(),
    beta: FlagOverrides = emptyMap(),
    release: FlagOverrides = emptyMap(),
): ThunderbirdOverrides = ThunderbirdOverrides(debug = debug, daily = daily, beta = beta, release = release)

private fun k9Overrides(
    debug: FlagOverrides = emptyMap(),
    release: FlagOverrides = emptyMap(),
): K9Overrides = K9Overrides(debug = debug, release = release)
