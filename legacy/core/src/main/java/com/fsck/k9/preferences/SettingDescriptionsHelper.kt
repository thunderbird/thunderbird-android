package com.fsck.k9.preferences

import com.fsck.k9.preferences.Settings.SettingsDescription
import java.util.TreeMap

internal typealias SettingDescriptionVersions = TreeMap<Int, SettingsDescription<*>?>
internal typealias SettingsDescriptions = Map<String, SettingDescriptionVersions>

internal fun versions(vararg pairs: Pair<Int, SettingsDescription<*>?>): SettingDescriptionVersions {
    return pairs.toMap(TreeMap<Int, SettingsDescription<*>?>())
}
