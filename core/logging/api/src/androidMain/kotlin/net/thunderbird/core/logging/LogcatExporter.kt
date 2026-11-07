package net.thunderbird.core.logging

import android.net.Uri

interface LogcatExporter {
    suspend fun export(destination: Uri)
}
