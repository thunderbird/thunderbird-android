package net.thunderbird.core.preference.imageCompression

const val IMAGE_COMPRESSION_SETTINGS_DEFAULT_JPEG_QUALITY = 85
const val IMAGE_COMPRESSION_SETTINGS_DEFAULT_LONGEST_EDGE = 1920

data class ImageCompressionSettings(
    val jpegQuality: Int = IMAGE_COMPRESSION_SETTINGS_DEFAULT_JPEG_QUALITY,
    val longestEdge: Int = IMAGE_COMPRESSION_SETTINGS_DEFAULT_LONGEST_EDGE,
)
