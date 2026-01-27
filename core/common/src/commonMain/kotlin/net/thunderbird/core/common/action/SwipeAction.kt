package net.thunderbird.core.common.action

enum class SwipeAction(val removesItem: Boolean) {
    None(removesItem = false),
    ToggleSelection(removesItem = false),
    ToggleRead(removesItem = false),
    ToggleStar(removesItem = false),
    Archive(removesItem = true),
    ArchiveDisabled(removesItem = false),
    ArchiveSetupArchiveFolder(removesItem = false),
    Delete(removesItem = true),
    Spam(removesItem = true),
    Move(removesItem = true),
}

data class SwipeActions(
    val leftAction: SwipeAction,
    val rightAction: SwipeAction,
)
