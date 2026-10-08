package net.thunderbird.core.common.notification

import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test

class NotificationActionTokensTest {
    @Test
    fun `normalizeOrder should ignore unknown tokens`() {
        val result = NotificationActionTokens.normalizeOrder(
            persistedTokens = listOf("unknown", NotificationActionTokens.DELETE),
            supportedActions = supportedActions(),
        )

        assertThat(result).containsExactly("delete", "reply", "mark_as_read")
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

        assertThat(result).containsExactly("delete", "reply", "mark_as_read")
    }

    @Test
    fun `normalizeOrder should append missing supported actions in canonical order`() {
        val result = NotificationActionTokens.normalizeOrder(
            persistedTokens = listOf(NotificationActionTokens.MARK_AS_READ),
            supportedActions = supportedActions(),
        )

        assertThat(result).containsExactly("mark_as_read", "reply", "delete")
    }

    private fun supportedActions(): List<Pair<String, String>> {
        return listOf(
            NotificationActionTokens.REPLY to NotificationActionTokens.REPLY,
            NotificationActionTokens.MARK_AS_READ to NotificationActionTokens.MARK_AS_READ,
            NotificationActionTokens.DELETE to NotificationActionTokens.DELETE,
        )
    }
}
