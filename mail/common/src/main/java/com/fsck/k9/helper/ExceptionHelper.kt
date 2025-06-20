package com.fsck.k9.helper

import net.thunderbird.core.common.exception.rootCauseMassage

object ExceptionHelper {
    @Deprecated(
        message = "Use net.thunderbird.core.common.exception.rootCauseMassage extension property instead.",
        replaceWith = ReplaceWith(
            "throwable.rootCauseMassage",
            "net.thunderbird.core.common.exception.rootCauseMassage",
        ),
    )
    @JvmStatic
    fun getRootCauseMessage(throwable: Throwable): String {
        return throwable.rootCauseMassage.orEmpty()
    }
}
