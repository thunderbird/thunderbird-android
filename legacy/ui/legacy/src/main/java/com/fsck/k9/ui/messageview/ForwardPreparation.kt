package com.fsck.k9.ui.messageview

/** Waits for the existing complete-message download before opening the forward composer. */
internal class ForwardPreparation(
    private val confirmDownload: () -> Unit,
    private val downloadMessage: () -> Unit,
    private val forward: () -> Unit,
    private val showError: () -> Unit,
) {
    private enum class State { IDLE, CONFIRMING, DOWNLOADING }

    private var state = State.IDLE

    fun start(isMessageIncomplete: Boolean) {
        if (state != State.IDLE) return

        if (isMessageIncomplete) {
            state = State.CONFIRMING
            confirmDownload()
        } else {
            forward()
        }
    }

    fun download() {
        if (state != State.CONFIRMING) return
        state = State.DOWNLOADING
        downloadMessage()
    }

    fun onMessageLoaded(isMessageIncomplete: Boolean) {
        if (state != State.DOWNLOADING) return
        cancel()
        if (isMessageIncomplete) {
            showError()
        } else {
            forward()
        }
    }

    fun forwardWithoutDownloading() {
        if (state != State.CONFIRMING) return
        cancel()
        forward()
    }

    fun cancel() {
        state = State.IDLE
    }
}
