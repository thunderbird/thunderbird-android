package net.thunderbird.core.configstore.backend

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import net.thunderbird.core.configstore.ConfigId
import net.thunderbird.core.configstore.backend.DefaultDataStoreConfigBackend
import okio.Path.Companion.toPath

internal const val DATA_STORE_FILE_EXTENSION = "preferences_pb"

class DataStoreConfigBackendFactory(
    private val fileManager: ConfigBackendFileManager,
) : ConfigBackendFactory {
    override fun create(id: ConfigId): ConfigBackend {
        val dataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                fileManager.getFilePath(generateFileName(id)).toPath()
            },
        )

        return DefaultDataStoreConfigBackend(dataStore)
    }

    private fun generateFileName(id: ConfigId): String {
        return "${id.backend}.$DATA_STORE_FILE_EXTENSION"
    }
}
