package net.thunderbird.core.preference.imageCompression

import net.thunderbird.core.preference.PreferenceManager

enum class ImageCompressionSettingKey(val value: String) {
    JpegQuality("imageCompressionJpegQuality"),
    LongestEdge("imageCompressionLongestEdge"),
}

interface ImageCompressionSettingsPreferenceManager : PreferenceManager<ImageCompressionSettings>
