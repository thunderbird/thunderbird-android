package com.fsck.k9.ui.messagedetails

import android.app.PendingIntent
import android.content.res.Resources
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.k9mail.core.android.common.contact.CachingRepository
import app.k9mail.core.android.common.contact.ContactPermissionResolver
import app.k9mail.core.android.common.contact.ContactRepository
import app.k9mail.legacy.mailstore.FolderRepository
import app.k9mail.legacy.message.controller.MessageReference
import app.k9mail.legacy.ui.folder.FolderNameFormatter
import com.fsck.k9.helper.ClipboardManager
import com.fsck.k9.mail.Address
import com.fsck.k9.mailstore.CryptoResultAnnotation
import com.fsck.k9.mailstore.MessageDate
import com.fsck.k9.mailstore.MessageRepository
import com.fsck.k9.ui.R
import com.fsck.k9.view.MessageCryptoDisplayStatus
import java.text.DateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.thunderbird.core.android.account.AccountManager
import net.thunderbird.core.android.account.LegacyAccount
import net.thunderbird.core.common.mail.toEmailAddressOrNull
import net.thunderbird.feature.mail.folder.api.Folder

@Suppress("TooManyFunctions")
internal class MessageDetailsViewModel(
    private val resources: Resources,
    private val messageRepository: MessageRepository,
    private val folderRepository: FolderRepository,
    private val contactSettingsProvider: ContactSettingsProvider,
    private val contactRepository: ContactRepository,
    private val contactPermissionResolver: ContactPermissionResolver,
    private val clipboardManager: ClipboardManager,
    private val accountManager: AccountManager,
    private val participantFormatter: MessageDetailsParticipantFormatter,
    private val folderNameFormatter: FolderNameFormatter,
) : ViewModel() {
    private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.MEDIUM, Locale.getDefault())

    private val internalUiState = MutableStateFlow<MessageDetailsState>(MessageDetailsState.Loading)
    val uiState: Flow<MessageDetailsState>
        get() = internalUiState

    private val eventChannel = Channel<MessageDetailEvent>()
    val uiEvents = eventChannel.receiveAsFlow()

    private var messageReference: MessageReference? = null
    var cryptoResult: CryptoResultAnnotation? = null

    fun initialize(messageReference: MessageReference) {
        this.messageReference = messageReference
        loadData(messageReference)
    }

    fun reload() {
        messageReference?.let { messageReference ->
            if (contactRepository is CachingRepository) {
                contactRepository.clearCache()
            }

            loadData(messageReference)
        }
    }

    private fun loadData(messageReference: MessageReference) {
        viewModelScope.launch(Dispatchers.IO) {
            internalUiState.value = try {
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
                    folder = folder?.toFolderInfo(),
                )

                val messageDetailsAppearance = MessageDetailsAppearance(
                    showContactPicture = contactSettingsProvider.isShowContactPicture,
                    alwaysHideAddToContactsButton = !contactPermissionResolver.hasContactPermission(),
                )

                MessageDetailsState.DataLoaded(
                    appearance = messageDetailsAppearance,
                    details = messageDetailsUi,
                )
            } catch (e: Exception) {
                MessageDetailsState.Error
            }
        }
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
            isClickable = messageCryptoDisplayStatus.hasAssociatedKey() ||
                messageCryptoDisplayStatus.isUnknownKey ||
                hasOpenPgpInsecureWarningPendingIntent(),
        )
    }

    private fun List<Address>.toParticipants(account: LegacyAccount): List<Participant> {
        return this.map { address ->
            val displayName = participantFormatter.getDisplayName(address, account)
            val emailAddress = address.address

            Participant(
                displayName = displayName,
                emailAddress = emailAddress,
                contactLookupUri = getContactLookupUri(emailAddress),
            )
        }
    }

    private fun getContactLookupUri(email: String): Uri? {
        return email.toEmailAddressOrNull()?.let { emailAddress ->
            contactRepository.getContactFor(emailAddress)?.uri
        }
    }

    private fun Folder.toFolderInfo(): FolderInfoUi {
        return FolderInfoUi(
            displayName = folderNameFormatter.displayName(this),
            type = this.type,
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
        val appearance: MessageDetailsAppearance,
        val details: MessageDetailsUi,
    ) : MessageDetailsState
}

sealed interface MessageDetailEvent {
    data class ShowCryptoKeys(val pendingIntent: PendingIntent) : MessageDetailEvent
    object SearchCryptoKeys : MessageDetailEvent
    object ShowCryptoWarning : MessageDetailEvent
}
