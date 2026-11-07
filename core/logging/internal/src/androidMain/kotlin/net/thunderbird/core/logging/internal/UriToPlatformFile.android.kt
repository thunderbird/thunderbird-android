package net.thunderbird.core.logging.internal

import com.eygraber.uri.Uri
import com.eygraber.uri.toAndroidUri
import io.github.vinceglb.filekit.PlatformFile

internal actual fun uriToPlatformFile(uri: Uri): PlatformFile = PlatformFile(uri.toAndroidUri())
