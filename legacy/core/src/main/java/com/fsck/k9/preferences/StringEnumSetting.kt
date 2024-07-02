package com.fsck.k9.preferences

import com.fsck.k9.preferences.Settings.StringSetting

internal class StringEnumSetting(
    defaultValue: String,
    private val values: Set<String>,
) : StringSetting(defaultValue) {
    override fun fromString(value: String?): String {
        return value?.takeIf { it in values } ?: defaultValue
    }
}
