package com.fsck.k9

enum class SwipeAction(val removesItem: Boolean) {
    None(removesItem = false),
    ToggleSelection(removesItem = false),
    ToggleRead(removesItem = false),
    ToggleStar(removesItem = false),
    Archive(removesItem = true),
    Delete(removesItem = true),
    Spam(removesItem = true),
    Move(removesItem = true),
}
