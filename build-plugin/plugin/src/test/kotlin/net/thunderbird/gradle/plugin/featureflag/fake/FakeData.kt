package net.thunderbird.gradle.plugin.featureflag.fake

import net.thunderbird.gradle.plugin.featureflag.fake.FakeData.SCHEMA

object FakeData {
    // language=json
    val SCHEMA = $$"""
            {
              "$schema": "http://json-schema.org/draft-07/schema#",
              "$id": "https://example.test/featureflag.schema.json",
              "type": "object",
              "additionalProperties": false,
              "required": ["version", "flags"],
              "properties": {
                "version": { "type": "string" },
                "flags": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "required": ["key", "default"],
                    "properties": {
                      "key": { "type": "string", "pattern": "^[a-z0-9_]+$" },
                      "default": { "type": "boolean" },
                      "time_to_promote": { "type": "string", "format": "date" }
                    }
                  }
                },
                "overrides": { "type": "object" }
              }
            }
    """.trimIndent()

    /**
     * A catalog that satisfies both [SCHEMA] and the catalog rules checked after deserialization.
     */
    val VALID_CATALOG = catalog()

    // language=json
    val INVALID_CATALOG = """
            {
              "flags": [
                {
                  "key": "Invalid Key"
                }
              ]
            }
    """.trimIndent()

    /**
     * Only rejected by [SCHEMA] when format assertions are enabled.
     */
    val CATALOG_WITH_INVALID_DATE_FORMAT = catalog(timeToPromote = "not-a-date")

    /**
     * Accepted by [SCHEMA], but `overrides.thunderbird.debug` overrides a key that `flags` does not define.
     */
    val CATALOG_WITH_UNKNOWN_OVERRIDE_KEY = catalog(
        // language=json
        thunderbirdDebugOverrides = """{ "unknown_flag": true }""",
    )

    /**
     * Builds a catalog whose single override is keyed by [overrideKey].
     *
     * Overrides are keyed by flag keys, so [overrideKey] is expected to follow the flag key format.
     */
    // language=json
    fun catalogWithOverrideKey(overrideKey: String): String = """
            {
              "version": "2026-07-30.1",
              "flags": [
                {
                  "key": "archive_marks_as_read",
                  "default": true
                }
              ],
              "overrides": {
                "thunderbird": {
                  "debug": {
                    "$overrideKey": false
                  }
                }
              }
            }
    """.trimIndent()

    /**
     * Builds a full catalog, including every override of both apps.
     *
     * @param timeToPromote The `time_to_promote` of the single declared flag.
     * @param thunderbirdDebugOverrides The `overrides.thunderbird.debug` object, as JSON.
     */
    private fun catalog(
        timeToPromote: String = "2026-12-31",
        // language=json
        thunderbirdDebugOverrides: String = """{ "archive_marks_as_read": false }""",
    ): String =
        // language=json
        """
            {
              "version": "2026-07-30.1",
              "flags": [
                {
                  "key": "archive_marks_as_read",
                  "default": true,
                  "time_to_promote": "$timeToPromote"
                }
              ],
              "overrides": {
                "thunderbird": {
                  "debug": $thunderbirdDebugOverrides,
                  "daily": {},
                  "beta": {},
                  "release": {}
                },
                "k9": {
                  "debug": {},
                  "release": {}
                }
              }
            }
        """.trimIndent()
}
