package net.thunderbird.core.logging.internal

import com.eygraber.uri.Uri
import io.github.vinceglb.filekit.PlatformFile

internal expect fun uriToPlatformFile(uri: Uri): PlatformFile
