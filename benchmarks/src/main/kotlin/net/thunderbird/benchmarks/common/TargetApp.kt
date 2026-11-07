package net.thunderbird.benchmarks.common

import androidx.test.platform.app.InstrumentationRegistry

/**
 * The application id of the build variant under test.
 *
 * It's passed as the `targetAppId` instrumentation runner argument, see `build.gradle.kts`.
 */
val targetAppId: String
    get() = requireNotNull(InstrumentationRegistry.getArguments().getString("targetAppId")) {
        "targetAppId not passed as instrumentation runner arg"
    }

/**
 * Resource id of the message list, shown once startup with an account configured is complete.
 */
internal const val MESSAGE_LIST_RESOURCE_ID = "message_list"

internal const val MESSAGE_LIST_TIMEOUT_MS = 10_000L
