package app.k9mail.backend.demo

import java.util.UUID

internal object DemoHelper {
    fun createNewServerId() = UUID.randomUUID().toString()
}
