package app.k9mail.feature.migration.qrcode.settings

import java.util.UUID

internal class DefaultUuidGenerator : UuidGenerator {
    override fun generateUuid(): String {
        return UUID.randomUUID().toString()
    }
}
