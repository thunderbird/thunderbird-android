package com.fsck.k9.activity.compose

import com.fsck.k9.Account
import com.fsck.k9.Identity
import com.fsck.k9.K9
import com.fsck.k9.MissingAccoutException
import com.fsck.k9.Preferences
import com.fsck.k9.controller.MessageReference
import com.fsck.k9.mail.Message
import com.fsck.k9.mailstore.LocalMessage
import com.fsck.k9.mailstore.MessageViewInfo
import com.fsck.k9.message.IdentityField
import com.fsck.k9.message.IdentityHeaderParser
import java.util.HashMap
import java.util.Objects

class AccountProvider {
    var account: Account
    var identity: Identity
        set(identity) {
            identityChanged = field != null && !Objects.equals(field, identity)
            field = identity
        }

    private val preferences: Preferences
    private var identityChanged = false
    var signatureChanged = false

    constructor(preferences: Preferences, accountUuid: String?) {
        var acc: Account? = null
        if (accountUuid != null) {
            acc = preferences.getAccount(accountUuid)
        }

        if (acc == null) {
            acc = preferences.defaultAccount
        }

        if (acc == null) {
            throw MissingAccoutException()
        }
        this.account = acc!!
        this.identity = account.getIdentity(0)
        this.preferences = preferences
    }

    fun onAccountChosen(account: Account, callback: OnAccountSelectedCallback) {
        if (!this.account.equals(account)) {
            val previousAccount = this.account
            this.account = account
            callback.accountChosen(previousAccount, account)
        }
    }

    fun updateAccountAndIdentity(
        messageViewInfo: MessageViewInfo,
        message: Message,
        callback: OnIdentityUpdateFinishedCallback
    ) {
        val draftProperties: Map<IdentityField?, String?> = decodeIdentityHeader(message, messageViewInfo)
        if (isChangedIdentiy(draftProperties)) {
            findIdentity(draftProperties)
        }
        updateSignature(draftProperties, message)
        updateReplyTo(message)
        callback.onFinished(getRelatedMessageReference(draftProperties), draftProperties)
    }

    private fun getRelatedMessageReference(draftProperties: Map<IdentityField?, String?>): MessageReference? {
        var relatedMessageReference: MessageReference? = null
        if (draftProperties.containsKey(IdentityField.ORIGINAL_MESSAGE)) {
            val originalMessage = draftProperties[IdentityField.ORIGINAL_MESSAGE]
            val messageReference = MessageReference.parse(originalMessage)
            if (messageReference != null) {
                // Check if this is a valid account in our database
                val account: Account? = preferences.getAccount(messageReference.accountUuid)
                if (account != null) {
                    relatedMessageReference = messageReference
                }
            }
        }
        return relatedMessageReference
    }

    fun hasIdentityChanged(): Boolean {
        return identityChanged
    }

    private fun decodeIdentityHeader(
        message: Message,
        messageViewInfo: MessageViewInfo
    ): Map<IdentityField?, String?> {
        // Decode the identity header when loading a draft.
        // See buildIdentityHeader(TextBody) for a detailed description of the composition of this blob.
        var draftProperties: Map<IdentityField?, String?> = HashMap()
        var identityHeaders = message.getHeader(K9.IDENTITY_HEADER)
        if (identityHeaders.size == 0) {
            identityHeaders = messageViewInfo.rootPart.getHeader(K9.IDENTITY_HEADER)
        }
        if (identityHeaders.size > 0 && identityHeaders[0] != null) {
            draftProperties = IdentityHeaderParser.parse(identityHeaders[0])
        }
        return draftProperties
    }

    private fun findIdentity(k9identity: Map<IdentityField?, String?>) {
        var changedIdentityExists = false
        for (id in account.identities) {
            if (id.name.equals(k9identity[IdentityField.NAME]) && id.email.equals(k9identity[IdentityField.EMAIL])) {
                identity = id
                changedIdentityExists = true
                break
            }
        }
        if (!changedIdentityExists) {
            identity = Identity(null, k9identity[IdentityField.NAME], k9identity[IdentityField.EMAIL])
        }
        identityChanged = true
    }

    private fun isChangedIdentiy(k9identity: Map<IdentityField?, String?>) =
        k9identity.containsKey(IdentityField.NAME) && k9identity.containsKey(IdentityField.EMAIL)

    private fun updateSignature(k9identity: Map<IdentityField?, String?>, message: Message) {
        if (k9identity.containsKey(IdentityField.SIGNATURE)) {
            identity = identity
                .withSignatureUse(true)
                .withSignature(k9identity[IdentityField.SIGNATURE])
            signatureChanged = true
        } else {
            if (message is LocalMessage) {
                identity = identity.withSignatureUse(message.folder.signatureUse)
            }
        }
    }

    private fun updateReplyTo(message: Message) {
        if (message.replyTo.size > 0) {
            identity = identity.withReplyTo(message.replyTo[0].toEncodedString())
        }
    }
}

interface OnAccountSelectedCallback {
    fun accountChosen(previousAccount: Account, account: Account)
}

interface OnIdentityUpdateFinishedCallback {
    fun onFinished(relatedMessageReference: MessageReference?, draftProperties: Map<IdentityField?, String?>)
}
