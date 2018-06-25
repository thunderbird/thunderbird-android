package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.List;

import android.app.PendingIntent;

import com.fsck.k9.activity.compose.RecipientMvpView.CryptoSpecialModeDisplayType;
import com.fsck.k9.activity.compose.RecipientMvpView.CryptoStatusDisplayType;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.message.AutocryptStatusInteractor.RecipientAutocryptStatus;
import com.fsck.k9.message.AutocryptStatusInteractor.RecipientAutocryptStatusType;
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderState;
import com.fsck.k9.view.RecipientSelectView.Recipient;

/** This is an immutable object which contains all relevant metadata entered
 * during email composition to apply cryptographic operations before sending
 * or saving as draft.
 */
public class ComposeCryptoStatus {


    private OpenPgpProviderState openPgpProviderState;
    private Long openPgpKeyId;
    private String[] recipientAddresses;
    private boolean enablePgpInline;
    private boolean preferEncryptMutual;
    private boolean isReplyToEncrypted;
    private boolean encryptSubject;
    private CryptoMode cryptoMode;
    private RecipientAutocryptStatus recipientAutocryptStatus;


    public Long getOpenPgpKeyId() {
        return openPgpKeyId;
    }

    CryptoStatusDisplayType getCryptoStatusDisplayType() {
        switch (openPgpProviderState) {
            case UNCONFIGURED:
                return CryptoStatusDisplayType.UNCONFIGURED;
            case UNINITIALIZED:
                return CryptoStatusDisplayType.UNINITIALIZED;
            case ERROR:
            case UI_REQUIRED:
                return CryptoStatusDisplayType.ERROR;
            case OK:
                // provider status is ok -> return value is based on cryptoMode
                break;
            default:
                throw new AssertionError("all CryptoProviderStates must be handled!");
        }

        if (recipientAutocryptStatus == null) {
            throw new IllegalStateException("Display type must be obtained from provider!");
        }

        RecipientAutocryptStatusType recipientAutocryptStatusType = recipientAutocryptStatus.type;

        if (recipientAutocryptStatusType == RecipientAutocryptStatusType.ERROR) {
            return CryptoStatusDisplayType.ERROR;
        }

        if (isEncryptionEnabled()) {
            if (!recipientAutocryptStatusType.canEncrypt()) {
                return CryptoStatusDisplayType.ENABLED_ERROR;
            } else if (recipientAutocryptStatusType.isConfirmed()) {
                return CryptoStatusDisplayType.ENABLED_TRUSTED;
            } else {
                return CryptoStatusDisplayType.ENABLED;
            }
        } else if (isSigningEnabled()) {
            return CryptoStatusDisplayType.SIGN_ONLY;
        } else if (recipientAutocryptStatusType.canEncrypt()) {
            return CryptoStatusDisplayType.AVAILABLE;
        } else {
            return CryptoStatusDisplayType.UNAVAILABLE;
        }
    }

    CryptoSpecialModeDisplayType getCryptoSpecialModeDisplayType() {
        if (openPgpProviderState != OpenPgpProviderState.OK) {
            return CryptoSpecialModeDisplayType.NONE;
        }

        if (isSignOnly() && isPgpInlineModeEnabled()) {
            return CryptoSpecialModeDisplayType.SIGN_ONLY_PGP_INLINE;
        }

        if (isSignOnly()) {
            return CryptoSpecialModeDisplayType.SIGN_ONLY;
        }

        if (allRecipientsCanEncrypt() && isPgpInlineModeEnabled()) {
            return CryptoSpecialModeDisplayType.PGP_INLINE;
        }

        return CryptoSpecialModeDisplayType.NONE;
    }

    public boolean shouldUsePgpMessageBuilder() {
        // CryptoProviderState.ERROR will be handled as an actual error, see SendErrorState
        return openPgpProviderState != OpenPgpProviderState.UNCONFIGURED;
    }

    public boolean isEncryptionEnabled() {
        if (openPgpProviderState == OpenPgpProviderState.UNCONFIGURED) {
            return false;
        }

        boolean isExplicitlyEnabled = (cryptoMode == CryptoMode.CHOICE_ENABLED);
        boolean isMutualAndNotDisabled = (cryptoMode != CryptoMode.CHOICE_DISABLED && canEncryptAndIsMutualDefault());
        boolean isReplyAndNotDisabled = (cryptoMode != CryptoMode.CHOICE_DISABLED && isReplyToEncrypted());
        return isExplicitlyEnabled || isMutualAndNotDisabled || isReplyAndNotDisabled;
    }

    boolean isSignOnly() {
        return cryptoMode == CryptoMode.SIGN_ONLY;
    }

    public boolean isSigningEnabled() {
        return cryptoMode == CryptoMode.SIGN_ONLY || isEncryptionEnabled();
    }

    public boolean isPgpInlineModeEnabled() {
        return enablePgpInline;
    }

    public boolean isProviderStateOk() {
        return openPgpProviderState == OpenPgpProviderState.OK;
    }

    boolean allRecipientsCanEncrypt() {
        return recipientAutocryptStatus != null && recipientAutocryptStatus.type.canEncrypt();
    }

    public String[] getRecipientAddresses() {
        return recipientAddresses;
    }

    public boolean hasRecipients() {
        return recipientAddresses.length > 0;
    }

    public boolean isSenderPreferEncryptMutual() {
        return preferEncryptMutual;
    }

    private boolean isRecipientsPreferEncryptMutual() {
        return recipientAutocryptStatus.type.isMutual();
    }

    public boolean isReplyToEncrypted() {
        return isReplyToEncrypted;
    }

    boolean canEncryptAndIsMutualDefault() {
        return allRecipientsCanEncrypt() && isSenderPreferEncryptMutual() && isRecipientsPreferEncryptMutual();
    }

    boolean hasAutocryptPendingIntent() {
        return recipientAutocryptStatus.hasPendingIntent();
    }

    PendingIntent getAutocryptPendingIntent() {
        return recipientAutocryptStatus.intent;
    }

    public boolean isEncryptSubject() {
        return encryptSubject;
    }

    public static class ComposeCryptoStatusBuilder {

        private OpenPgpProviderState openPgpProviderState;
        private CryptoMode cryptoMode;
        private Long openPgpKeyId;
        private List<Recipient> recipients;
        private Boolean enablePgpInline;
        private Boolean preferEncryptMutual;
        private Boolean isReplyToEncrypted;
        private Boolean encryptSubject;

        public ComposeCryptoStatusBuilder setOpenPgpProviderState(OpenPgpProviderState openPgpProviderState) {
            this.openPgpProviderState = openPgpProviderState;
            return this;
        }

        public ComposeCryptoStatusBuilder setCryptoMode(CryptoMode cryptoMode) {
            this.cryptoMode = cryptoMode;
            return this;
        }

        public ComposeCryptoStatusBuilder setOpenPgpKeyId(Long openPgpKeyId) {
            this.openPgpKeyId = openPgpKeyId;
            return this;
        }

        public ComposeCryptoStatusBuilder setRecipients(List<Recipient> recipients) {
            this.recipients = recipients;
            return this;
        }

        public ComposeCryptoStatusBuilder setEnablePgpInline(boolean cryptoEnableCompat) {
            this.enablePgpInline = cryptoEnableCompat;
            return this;
        }

        public ComposeCryptoStatusBuilder setPreferEncryptMutual(boolean preferEncryptMutual) {
            this.preferEncryptMutual = preferEncryptMutual;
            return this;
        }

        public ComposeCryptoStatusBuilder setIsReplyToEncrypted(boolean isReplyToEncrypted) {
            this.isReplyToEncrypted = isReplyToEncrypted;
            return this;
        }

        public ComposeCryptoStatusBuilder setEncryptSubject(boolean encryptSubject) {
            this.encryptSubject = encryptSubject;
            return this;
        }

        public ComposeCryptoStatus build() {
            if (openPgpProviderState == null) {
                throw new AssertionError("cryptoProviderState must be set!");
            }
            if (cryptoMode == null) {
                throw new AssertionError("crypto mode must be set!");
            }
            if (recipients == null) {
                throw new AssertionError("recipients must be set!");
            }
            if (enablePgpInline == null) {
                throw new AssertionError("enablePgpInline must be set!");
            }
            if (preferEncryptMutual == null) {
                throw new AssertionError("preferEncryptMutual must be set!");
            }
            if (isReplyToEncrypted == null) {
                throw new AssertionError("isReplyToEncrypted must be set!");
            }
            if (encryptSubject == null) {
                throw new AssertionError("encryptSubject must be set!");
            }

            ArrayList<String> recipientAddresses = new ArrayList<>();
            for (Recipient recipient : recipients) {
                recipientAddresses.add(recipient.address.getAddress());
            }

            ComposeCryptoStatus result = new ComposeCryptoStatus();
            result.openPgpProviderState = openPgpProviderState;
            result.cryptoMode = cryptoMode;
            result.recipientAddresses = recipientAddresses.toArray(new String[0]);
            result.openPgpKeyId = openPgpKeyId;
            result.isReplyToEncrypted = isReplyToEncrypted;
            result.enablePgpInline = enablePgpInline;
            result.preferEncryptMutual = preferEncryptMutual;
            result.encryptSubject = encryptSubject;
            return result;
        }
    }

    ComposeCryptoStatus withRecipientAutocryptStatus(RecipientAutocryptStatus recipientAutocryptStatusType) {
        ComposeCryptoStatus result = new ComposeCryptoStatus();
        result.openPgpProviderState = openPgpProviderState;
        result.cryptoMode = cryptoMode;
        result.recipientAddresses = recipientAddresses;
        result.isReplyToEncrypted = isReplyToEncrypted;
        result.openPgpKeyId = openPgpKeyId;
        result.enablePgpInline = enablePgpInline;
        result.preferEncryptMutual = preferEncryptMutual;
        result.encryptSubject = encryptSubject;
        result.recipientAutocryptStatus = recipientAutocryptStatusType;
        return result;
    }

    public enum SendErrorState {
        PROVIDER_ERROR,
        KEY_CONFIG_ERROR,
        ENABLED_ERROR
    }

    public SendErrorState getSendErrorStateOrNull() {
        if (openPgpProviderState != OpenPgpProviderState.OK) {
            // TODO: be more specific about this error
            return SendErrorState.PROVIDER_ERROR;
        }

        if (openPgpKeyId == null && (isEncryptionEnabled() || isSignOnly())) {
            return SendErrorState.KEY_CONFIG_ERROR;
        }

        if (isEncryptionEnabled() && !allRecipientsCanEncrypt()) {
            return SendErrorState.ENABLED_ERROR;
        }

        return null;
    }

    enum AttachErrorState {
        IS_INLINE
    }

    AttachErrorState getAttachErrorStateOrNull() {
        if (openPgpProviderState == OpenPgpProviderState.UNCONFIGURED) {
            return null;
        }

        if (enablePgpInline) {
            return AttachErrorState.IS_INLINE;
        }

        return null;
    }

}
