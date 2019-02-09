package com.fsck.k9.ui.endtoend

import androidx.lifecycle.ViewModel

internal class AutocryptKeyTransferViewModel(
        val autocryptSetupMessageLiveEvent: AutocryptSetupMessageLiveEvent,
        val autocryptSetupTransferLiveEvent: AutocryptSetupTransferLiveEvent) : ViewModel()
