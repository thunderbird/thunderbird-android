package net.thunderbird.core.logging.internal

import com.eygraber.uri.Uri
import com.eygraber.uri.toURI
import io.github.vinceglb.filekit.PlatformFile
import java.io.File

internal actual fun uriToPlatformFile(uri: Uri): PlatformFile = PlatformFile(File(uri.toURI()))
