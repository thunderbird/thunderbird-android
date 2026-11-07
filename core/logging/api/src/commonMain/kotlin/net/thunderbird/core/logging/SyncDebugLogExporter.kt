package net.thunderbird.core.logging

import com.eygraber.uri.Uri

interface SyncDebugLogExporter {
    suspend fun export(destination: Uri)
}
