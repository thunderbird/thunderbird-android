package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.activity.compose.RecipientMvpView.CryptoSpecialModeDisplayType;
import com.fsck.k9.activity.compose.RecipientMvpView.CryptoStatusDisplayType;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoProviderState;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.RecipientCryptoStatus;

/** This is an immutable object which contains all relevant metadata entered
 * during e-mail composition to apply cryptographic operations before sending
 * or saving as draft.
 */
public class ComposeCryptoStatus {


    private CryptoProviderState cryptoProviderState;
    private CryptoMode cryptoMode;
    private boolean allKeysAvailable;
    private boolean allKeysVerified;
    private boolean hasRecipients;
    private Long signingKeyId;
    private Long selfEncryptKeyId;
    private String[] recipientAddresses;
    private boolean enablePgpInline;


    public long[] getEncryptKeyIds() {
        if (selfEncryptKeyId == null) {
            return null;
        }
        return new long[] { selfEncryptKeyId };
    }

    public String[] getRecipientAddresses() {
        return recipientAddresses;
    }

    public Long getSigningKeyId() {
        return signingKeyId;
    }

    CryptoStatusDisplayType getCryptoStatusDisplayType() {
        switch (cryptoProviderState) {
            case UNCONFIGURED:
                return CryptoStatusDisplayType.UNCONFIGURED;
            case UNINITIALIZED:
                return CryptoStatusDisplayType.UNINITIALIZED;
            case LOST_CONNECTION:
            case ERROR:
                return CryptoStatusDisplayType.ERROR;
            case OK:
                // provider status is ok -> return value is based on cryptoMode
                break;
            default:
                throw new AssertionError("all CryptoProviderStates must be handled!");
        }

        switch (cryptoMode) {
            case PRIVATE:
                if (!hasRecipients) {
                    return CryptoStatusDisplayType.PRIVATE_EMPTY;
                } else if (allKeysAvailable && allKeysVerified) {
                    return CryptoStatusDisplayType.PRIVATE_TRUSTED;
                } else if (allKeysAvailable) {
                    return CryptoStatusDisplayType.PRIVATE_UNTRUSTED;
                }
                return CryptoStatusDisplayType.PRIVATE_NOKEY;
            case OPPORTUNISTIC:
                if (!hasRecipients) {
                    return CryptoStatusDisplayType.OPPORTUNISTIC_EMPTY;
                } else if (allKeysAvailable && allKeysVerified) {
                    return CryptoStatusDisplayType.OPPORTUNISTIC_TRUSTED;
                } else if (allKeysAvailable) {
                    return CryptoStatusDisplayType.OPPORTUNISTIC_UNTRUSTED;
                }
                return CryptoStatusDisplayType.OPPORTUNISTIC_NOKEY;
            case SIGN_ONLY:
                return CryptoStatusDisplayType.SIGN_ONLY;
            case DISABLE:
                return CryptoStatusDisplayType.DISABLED;
            default:
                throw new AssertionError("all CryptoModes must be handled!");
        }
    }

    CryptoSpecialModeDisplayType getCryptoSpecialModeDisplayType() {
        if (cryptoProviderState != CryptoProviderState.OK) {
            return CryptoSpecialModeDisplayType.NONE;
        }

        if (isSignOnly() && isPgpInlineModeEnabled()) {
            return CryptoSpecialModeDisplayType.SIGN_ONLY_PGP_INLINE;
        }

        if (isSignOnly()) {
            return CryptoSpecialModeDisplayType.SIGN_ONLY;
        }

        if (isPgpInlineModeEnabled()) {
            return CryptoSpecialModeDisplayType.PGP_INLINE;
        }

        return CryptoSpecialModeDisplayType.NONE;
    }

    public boolean shouldUsePgpMessageBuilder() {
        return cryptoProviderState != CryptoProviderState.UNCONFIGURED && cryptoMode != CryptoMode.DISABLE;
    }

    public boolean isEncryptionEnabled() {
        return cryptoMode == CryptoMode.PRIVATE || cryptoMode == CryptoMode.OPPORTUNISTIC;
    }

    public boolean isEncryptionOpportunistic() {
        return cryptoMode == CryptoMode.OPPORTUNISTIC;
    }

    public boolean isSignOnly() {
        return cryptoMode == CryptoMode.SIGN_ONLY;
    }

    public boolean isSigningEnabled() {
        return cryptoMode != CryptoMode.DISABLE && signingKeyId != null;
    }

    public boolean isPgpInlineModeEnabled() {
        return enablePgpInline;
    }

    public boolean isCryptoDisabled() {
        return cryptoMode == CryptoMode.DISABLE;
    }

    public boolean isProviderStateOk() {
        return cryptoProviderState == CryptoProviderState.OK;
    }

    public static class ComposeCryptoStatusBuilder {

        private CryptoProviderState cryptoProviderState;
        private CryptoMode cryptoMode;
        private Long signingKeyId;
        private Long selfEncryptKeyId;
        private List<Recipient> recipients;
        private Boolean enablePgpInline;

        public ComposeCryptoStatusBuilder setCryptoProviderState(CryptoProviderState cryptoProviderState) {
            this.cryptoProviderState = cryptoProviderState;
            return this;
        }

        public ComposeCryptoStatusBuilder setCryptoMode(CryptoMode cryptoMode) {
            this.cryptoMode = cryptoMode;
            return this;
        }

        public ComposeCryptoStatusBuilder setSigningKeyId(long signingKeyId) {
            this.signingKeyId = signingKeyId;
            return this;
        }

        public ComposeCryptoStatusBuilder setSelfEncryptId(long selfEncryptKeyId) {
            this.selfEncryptKeyId = selfEncryptKeyId;
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

        public ComposeCryptoStatus build() {
            if (cryptoProviderState == null) {
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

            ArrayList<String> recipientAddresses = new ArrayList<>();
            boolean allKeysAvailable = true;
            boolean allKeysVerified = true;
            boolean hasRecipients = !recipients.isEmpty();
            for (Recipient recipient : recipients) {
                RecipientCryptoStatus cryptoStatus = recipient.getCryptoStatus();
                recipientAddresses.add(recipient.address.getAddress());
                if (cryptoStatus.isAvailable()) {
                    if (cryptoStatus == RecipientCryptoStatus.AVAILABLE_UNTRUSTED) {
                        allKeysVerified = false;
                    }
                } else {
                    allKeysAvailable = false;
                }
            }

            ComposeCryptoStatus result = new ComposeCryptoStatus();
            result.cryptoProviderState = cryptoProviderState;
            result.cryptoMode = cryptoMode;
            result.recipientAddresses = recipientAddresses.toArray(new String[0]);
            result.allKeysAvailable = allKeysAvailable;
            result.allKeysVerified = allKeysVerified;
            result.hasRecipients = hasRecipients;
            result.signingKeyId = signingKeyId;
            result.selfEncryptKeyId = selfEncryptKeyId;
            result.enablePgpInline = enablePgpInline;
            return result;
        }
    }

    public enum SendErrorState {
        PROVIDER_ERROR, SIGN_KEY_NOT_CONFIGURED, PRIVATE_BUT_MISSING_KEYS
    }

    public SendErrorState getSendErrorStateOrNull() {
        if (cryptoProviderState != CryptoProviderState.OK) {
            // TODO: be more specific about this error
            return SendErrorState.PROVIDER_ERROR;
        }
        boolean isSignKeyMissing = signingKeyId == null;
        if (isSignKeyMissing) {
            return SendErrorState.SIGN_KEY_NOT_CONFIGURED;
        }
        boolean isPrivateModeAndNotAllKeysAvailable = cryptoMode == CryptoMode.PRIVATE && !allKeysAvailable;
        if (isPrivateModeAndNotAllKeysAvailable) {
            return SendErrorState.PRIVATE_BUT_MISSING_KEYS;
        }

        return null;
    }

    enum AttachErrorState {
        IS_INLINE
    }

    AttachErrorState getAttachErrorStateOrNull() {
        if (cryptoProviderState == CryptoProviderState.UNCONFIGURED) {
            return null;
        }

        if (enablePgpInline) {
            return AttachErrorState.IS_INLINE;
        }

        return null;
    }

}
