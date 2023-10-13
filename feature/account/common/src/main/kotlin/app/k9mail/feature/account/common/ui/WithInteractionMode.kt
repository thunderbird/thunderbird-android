package app.k9mail.feature.account.common.ui

import app.k9mail.feature.account.common.domain.entity.InteractionMode

/**
 * Interface for screens that can be used in different interaction modes.
 */
interface WithInteractionMode {
    val mode: InteractionMode
}
