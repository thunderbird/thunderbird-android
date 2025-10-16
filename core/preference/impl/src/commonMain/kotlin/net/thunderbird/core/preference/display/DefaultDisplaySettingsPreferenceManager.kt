package net.thunderbird.core.preference.display

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettingsPreferenceManager
import net.thunderbird.core.preference.display.inboxSettings.DisplayInboxSettingsPreferenceManager
import net.thunderbird.core.preference.display.miscSettings.DisplayMiscSettingsPreferenceManager
import net.thunderbird.core.preference.display.visualSettings.DisplayVisualSettingsPreferenceManager

private const val TAG = "DefaultDisplaySettingsPreferenceManager"

class DefaultDisplaySettingsPreferenceManager(
    private val logger: Logger,
    private val coreSettingsPreferenceManager: DisplayCoreSettingsPreferenceManager,
    private val inboxSettingsPreferenceManager: DisplayInboxSettingsPreferenceManager,
    private val visualSettingsPreferenceManager: DisplayVisualSettingsPreferenceManager,
    private val miscSettingsPreferenceManager: DisplayMiscSettingsPreferenceManager,
) : DisplaySettingsPreferenceManager {
    private val configState: MutableStateFlow<DisplaySettings> = MutableStateFlow(value = loadConfig())

    override fun getConfig(): DisplaySettings = configState.value
    override fun getConfigFlow(): Flow<DisplaySettings> = configState

    override fun save(config: DisplaySettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        coreSettingsPreferenceManager.save(config.coreSettings)
        inboxSettingsPreferenceManager.save(config.inboxSettings)
        visualSettingsPreferenceManager.save(config.visualSettings)
        miscSettingsPreferenceManager.save(config.miscSettings)
        configState.update { config }
    }

    private fun loadConfig(): DisplaySettings = DisplaySettings(
        coreSettings = coreSettingsPreferenceManager.getConfig(),
        inboxSettings = inboxSettingsPreferenceManager.getConfig(),
        visualSettings = visualSettingsPreferenceManager.getConfig(),
        miscSettings = miscSettingsPreferenceManager.getConfig(),
    )
}
