package net.thunderbird.feature.mail.message.list.internal.fakes

internal class RecordingFunction<T> {
    val calls = mutableListOf<T>()

    val function: (T) -> Unit = { value ->
        calls += value
    }
}

internal class RecordingSuspendFunction<T> {
    val calls = mutableListOf<T>()

    val function: suspend (T) -> Unit = { value ->
        calls += value
    }
}
