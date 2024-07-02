package com.fsck.k9.preferences

import com.fsck.k9.preferences.Settings.InvalidSettingValueException
import com.fsck.k9.preferences.Settings.StringSetting

internal class NoDefaultStringEnumSetting(
    private val values: Set<String>,
) : StringSetting(null) {
    override fun fromString(value: String?): String {
        return value?.takeIf { it in values } ?: throw InvalidSettingValueException("Unsupported value: $value")
    }
}
