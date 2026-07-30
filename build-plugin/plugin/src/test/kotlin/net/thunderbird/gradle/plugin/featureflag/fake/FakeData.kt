package net.thunderbird.gradle.plugin.featureflag.fake

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
                }
              }
            }
    """.trimIndent()

    // language=json
    val VALID_CATALOG = """
            {
              "version": "2026-07-30.1",
              "flags": [
                {
                  "key": "archive_marks_as_read",
                  "default": true,
                  "time_to_promote": "2026-12-31"
                }
              ]
            }
    """.trimIndent()

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

    // language=json
    val CATALOG_WITH_INVALID_DATE_FORMAT = """
            {
              "version": "2026-07-30.1",
              "flags": [
                {
                  "key": "archive_marks_as_read",
                  "default": true,
                  "time_to_promote": "not-a-date"
                }
              ]
            }
    """.trimIndent()

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
}
