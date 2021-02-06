package com.fsck.k9.notification.rules

import com.fsck.k9.mail.internet.MimeMessage

class NotificationRule(val description: String, val enabled: Boolean = true, val action: Action, val actionExtra: String?, val clauses: List<NotificationRuleClause>) {
    public fun isMuted(message: MimeMessage): Boolean {
        if (enabled && action == Action.MUTE) {
            for (clause in clauses) {
                if (!clause.matches(message)) {
                    return false
                }
            }
            return true
        }
        return false
    }

    enum class Action {
        MUTE
    }
}
