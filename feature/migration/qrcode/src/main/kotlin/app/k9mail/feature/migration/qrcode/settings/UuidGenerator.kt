package app.k9mail.feature.migration.qrcode.settings

internal fun interface UuidGenerator {
    fun generateUuid(): String
}
