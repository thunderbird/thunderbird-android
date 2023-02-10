package com.fsck.k9.ui.messagedetails

import android.app.PendingIntent
import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fsck.k9.Account
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.helper.ClipboardManager
import com.fsck.k9.helper.Contacts
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.CryptoResultAnnotation
import com.fsck.k9.mailstore.Folder
import com.fsck.k9.mailstore.FolderRepository
import com.fsck.k9.mailstore.MessageDate
import com.fsck.k9.mailstore.MessageRepository
import com.fsck.k9.preferences.AccountManager
import com.fsck.k9.ui.R
import com.fsck.k9.ui.folders.FolderNameFormatter
import com.fsck.k9.view.MessageCryptoDisplayStatus
import java.text.DateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

internal class MessageDetailsViewModel(
    private val resources: Resources,
    private val messageRepository: MessageRepository,
    private val folderRepository: FolderRepository,
    private val contactSettingsProvider: ContactSettingsProvider,
    private val contacts: Contacts,
    private val clipboardManager: ClipboardManager,
    private val accountManager: AccountManager,
    private val participantFormatter: MessageDetailsParticipantFormatter,
    private val folderNameFormatter: FolderNameFormatter
) : ViewModel() {
    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.getDefault())
    private val uiState = MutableStateFlow<MessageDetailsState>(MessageDetailsState.Loading)
    private val eventChannel = Channel<MessageDetailEvent>()

    val uiEvents = eventChannel.receiveAsFlow()
    var cryptoResult: CryptoResultAnnotation? = null

    fun loadData(messageReference: MessageReference): StateFlow<MessageDetailsState> {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.value = try {
                val account = accountManager.getAccount(messageReference.accountUuid) ?: error("Account not found")
                val messageDetails = messageRepository.getMessageDetails(messageReference)

                val folder = folderRepository.getFolder(account, folderId = messageReference.folderId)

                val senderList = messageDetails.sender?.let { listOf(it) } ?: emptyList()
                val messageDetailsUi = MessageDetailsUi(
                    date = buildDisplayDate(messageDetails.date),
                    cryptoDetails = cryptoResult?.toCryptoDetails(),
                    from = messageDetails.from.toParticipants(account),
                    sender = senderList.toParticipants(account),
                    replyTo = messageDetails.replyTo.toParticipants(account),
                    to = messageDetails.to.toParticipants(account),
                    cc = messageDetails.cc.toParticipants(account),
                    bcc = messageDetails.bcc.toParticipants(account),
                    folder = folder?.toFolderInfo()
                )

                MessageDetailsState.DataLoaded(
                    showContactPicture = contactSettingsProvider.isShowContactPicture,
                    details = messageDetailsUi
                )
            } catch (e: Exception) {
                MessageDetailsState.Error
            }
        }

        return uiState
    }

    private fun buildDisplayDate(messageDate: MessageDate): String? {
        return when (messageDate) {
            is MessageDate.InvalidDate -> messageDate.dateHeader
            MessageDate.MissingDate -> null
            is MessageDate.ValidDate -> dateFormat.format(messageDate.date)
        }
    }

    private fun CryptoResultAnnotation.toCryptoDetails(): CryptoDetails {
        val messageCryptoDisplayStatus = MessageCryptoDisplayStatus.fromResultAnnotation(this)
        return CryptoDetails(
            cryptoStatus = messageCryptoDisplayStatus,
            isClickable = messageCryptoDisplayStatus.hasAssociatedKey() || messageCryptoDisplayStatus.isUnknownKey ||
                hasOpenPgpInsecureWarningPendingIntent()
        )
    }

    private fun List<Address>.toParticipants(account: Account): List<Participant> {
        return this.map { address ->
            val displayName = participantFormatter.getDisplayName(address, account)
            val emailAddress = address.address

            Participant(
                displayName = displayName,
                emailAddress = emailAddress,
                contactLookupUri = contacts.getContactUri(emailAddress)
            )
        }
    }

    private fun Folder.toFolderInfo(): FolderInfoUi {
        return FolderInfoUi(
            displayName = folderNameFormatter.displayName(this),
            type = this.type
        )
    }

    fun onCryptoStatusClicked() {
        val cryptoResult = cryptoResult ?: return
        val cryptoStatus = MessageCryptoDisplayStatus.fromResultAnnotation(cryptoResult)

        if (cryptoStatus.hasAssociatedKey()) {
            val pendingIntent = cryptoResult.openPgpSigningKeyIntentIfAny
            if (pendingIntent != null) {
                viewModelScope.launch {
                    eventChannel.send(MessageDetailEvent.ShowCryptoKeys(pendingIntent))
                }
            }
        } else if (cryptoStatus.isUnknownKey) {
            viewModelScope.launch {
                eventChannel.send(MessageDetailEvent.SearchCryptoKeys)
            }
        } else if (cryptoResult.hasOpenPgpInsecureWarningPendingIntent()) {
            viewModelScope.launch {
                eventChannel.send(MessageDetailEvent.ShowCryptoWarning)
            }
        }
    }

    fun onCopyEmailAddressToClipboard(participant: Participant) {
        val label = resources.getString(R.string.clipboard_label_email_address)
        val emailAddress = participant.address.address
        clipboardManager.setText(label, emailAddress)
    }

    fun onCopyNameAndEmailAddressToClipboard(participant: Participant) {
        val label = resources.getString(R.string.clipboard_label_name_and_email_address)
        val nameAndEmailAddress = participant.address.toString()
        clipboardManager.setText(label, nameAndEmailAddress)
    }
}

sealed interface MessageDetailsState {
    object Loading : MessageDetailsState
    object Error : MessageDetailsState
    data class DataLoaded(
        val showContactPicture: Boolean,
        val details: MessageDetailsUi
    ) : MessageDetailsState
}

sealed interface MessageDetailEvent {
    data class ShowCryptoKeys(val pendingIntent: PendingIntent) : MessageDetailEvent
    object SearchCryptoKeys : MessageDetailEvent
    object ShowCryptoWarning : MessageDetailEvent
}
