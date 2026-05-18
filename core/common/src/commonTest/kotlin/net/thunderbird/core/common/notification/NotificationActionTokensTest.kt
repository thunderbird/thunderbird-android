package net.thunderbird.core.common.notification

import kotlin.test.Test
import kotlin.test.assertEquals

class NotificationActionTokensTest {
    @Test
    fun `normalizeOrder should ignore unknown tokens`() {
        val result = NotificationActionTokens.normalizeOrder(
            persistedTokens = listOf("unknown", NotificationActionTokens.DELETE),
            supportedActions = supportedActions(),
        )

        assertEquals(
            listOf("delete", "reply", "mark_as_read"),
            result,
        )
    }

    @Test
    fun `normalizeOrder should keep first occurrence only`() {
        val result = NotificationActionTokens.normalizeOrder(
            persistedTokens = listOf(
                NotificationActionTokens.DELETE,
                NotificationActionTokens.REPLY,
                NotificationActionTokens.DELETE,
            ),
            supportedActions = supportedActions(),
        )

        assertEquals(
            listOf("delete", "reply", "mark_as_read"),
            result,
        )
    }

    @Test
    fun `normalizeOrder should append missing supported actions in canonical order`() {
        val result = NotificationActionTokens.normalizeOrder(
            persistedTokens = listOf(NotificationActionTokens.MARK_AS_READ),
            supportedActions = supportedActions(),
        )

        assertEquals(
            listOf("mark_as_read", "reply", "delete"),
            result,
        )
    }

    private fun supportedActions(): List<Pair<String, String>> {
        return listOf(
            NotificationActionTokens.REPLY to NotificationActionTokens.REPLY,
            NotificationActionTokens.MARK_AS_READ to NotificationActionTokens.MARK_AS_READ,
            NotificationActionTokens.DELETE to NotificationActionTokens.DELETE,
        )
    }
}
