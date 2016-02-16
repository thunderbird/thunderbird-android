package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fsck.k9.activity.compose.RecipientMvpView.CryptoStatusDisplayType;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.RecipientCryptoStatus;

/** This is an immutable object which contains all relevant metadata entered
 * during e-mail composition to apply cryptographic operations before sending
 * or saving as draft.
 */
public class ComposeCryptoStatus {


    private CryptoMode cryptoMode;
    private List<String> keyReferences;
    private boolean allKeysAvailable;
    private boolean allKeysVerified;
    private boolean hasRecipients;
    private Long signingKeyId;
    private Long selfEncryptKeyId;


    @SuppressWarnings("UnusedParameters")
    public long[] getEncryptKeyIds(boolean isDraft) {
        if (selfEncryptKeyId == null) {
            return null;
        }
        return new long[] { selfEncryptKeyId };
    }

    public String[] getEncryptKeyReferences(boolean isDraft) {
        if (isDraft) {
            return null;
        }
        if (keyReferences.isEmpty()) {
            return null;
        }
        return keyReferences.toArray(new String[keyReferences.size()]);
    }

    public Long getSigningKeyId() {
        return signingKeyId;
    }

    public CryptoStatusDisplayType getCryptoStatusDisplayType() {
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
            case ERROR:
                return CryptoStatusDisplayType.ERROR;
            default:
            case UNINITIALIZED:
                return CryptoStatusDisplayType.UNINITIALIZED;
        }
    }

    public boolean isPgpErrorState() {
        return cryptoMode == CryptoMode.ERROR;
    }

    public boolean shouldUsePgpMessageBuilder() {
        return cryptoMode == CryptoMode.PRIVATE || cryptoMode == CryptoMode.OPPORTUNISTIC
                || cryptoMode == CryptoMode.SIGN_ONLY;
    }

    public boolean isEncryptionEnabled() {
        return cryptoMode == CryptoMode.PRIVATE || cryptoMode == CryptoMode.OPPORTUNISTIC;
    }

    public boolean isSigningEnabled() {
        return cryptoMode != CryptoMode.DISABLE && signingKeyId != null;
    }

    public boolean isMissingSignKey() {
        return signingKeyId == null;
    }

    public boolean isPrivateAndIncomplete() {
        return cryptoMode == CryptoMode.PRIVATE && !allKeysAvailable;
    }


    public static class ComposeCryptoStatusBuilder {

        private CryptoMode cryptoMode;
        private Long signingKeyId;
        private Long selfEncryptKeyId;
        private List<Recipient> recipients;

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

        public ComposeCryptoStatus build() {
            if (cryptoMode == null) {
                throw new AssertionError("crypto mode must be set. this is a bug!");
            }
            if (recipients == null) {
                throw new AssertionError("recipients must be set. this is a bug!");
            }

            ArrayList<String> keyReferences = new ArrayList<>();
            boolean allKeysAvailable = true;
            boolean allKeysVerified = true;
            boolean hasRecipients = !recipients.isEmpty();
            for (Recipient recipient : recipients) {
                RecipientCryptoStatus cryptoStatus = recipient.getCryptoStatus();
                if (cryptoStatus.isAvailable()) {
                    keyReferences.add(recipient.getKeyReference());
                    if (cryptoStatus == RecipientCryptoStatus.AVAILABLE_UNTRUSTED) {
                        allKeysVerified = false;
                    }
                } else {
                    allKeysAvailable = false;
                }
            }

            ComposeCryptoStatus result = new ComposeCryptoStatus();
            result.cryptoMode = cryptoMode;
            result.keyReferences = Collections.unmodifiableList(keyReferences);
            result.allKeysAvailable = allKeysAvailable;
            result.allKeysVerified = allKeysVerified;
            result.hasRecipients = hasRecipients;
            result.signingKeyId = signingKeyId;
            result.selfEncryptKeyId = selfEncryptKeyId;
            return result;
        }
    }

}
