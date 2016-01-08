package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.activity.compose.RecipientMvpView.CryptoStatusDisplayType;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.RecipientCryptoStatus;

/** This is an immutable object, which contains all relevant metadata entered
 * during e-mail composition to apply cryptographic operations before sending
 * or saving as draft.
 */
public class ComposeCryptoStatus {


    private final CryptoMode cryptoMode;
    private final ArrayList<String> keyReferences;
    private final boolean allKeysAvailable;
    private final boolean allKeysVerified;
    private final Long signingKeyId;
    private final Long selfEncryptKeyId;


    private ComposeCryptoStatus(CryptoMode cryptoMode, boolean allKeysAvailable, boolean allKeysVerified,
            ArrayList<String> keyReferences, Long signingKeyId, Long selfEncryptKeyId) {
        this.cryptoMode = cryptoMode;
        this.keyReferences = keyReferences;
        this.allKeysAvailable = allKeysAvailable;
        this.allKeysVerified = allKeysVerified;
        this.signingKeyId = signingKeyId;
        this.selfEncryptKeyId = selfEncryptKeyId;
    }

    public static ComposeCryptoStatus createFromRecipients(CryptoMode cryptoMode, List<Recipient> recipients,
            Long accountCryptoKey) {
        ArrayList<String> keyReferences = new ArrayList<>();

        boolean allKeysAvailable = true;
        boolean allKeysVerified = true;
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

        // noinspection UnnecessaryLocalVariable
        Long signingKeyId = accountCryptoKey;
        // noinspection UnnecessaryLocalVariable // TODO introduce separate key setting here?
        Long selfEncryptKeyId = accountCryptoKey;

        return new ComposeCryptoStatus(cryptoMode, allKeysAvailable, allKeysVerified, keyReferences,
                signingKeyId, selfEncryptKeyId);
    }

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
                if (allKeysAvailable && allKeysVerified) {
                    return CryptoStatusDisplayType.PRIVATE_TRUSTED;
                } else if (allKeysAvailable) {
                    return CryptoStatusDisplayType.PRIVATE_UNTRUSTED;
                }
                return CryptoStatusDisplayType.PRIVATE_NOKEY;
            case OPPORTUNISTIC:
                if (allKeysAvailable && allKeysVerified) {
                    return CryptoStatusDisplayType.OPPORTUNISTIC_TRUSTED;
                } else if (allKeysAvailable) {
                    return CryptoStatusDisplayType.OPPORTUNISTIC_UNTRUSTED;
                }
                return CryptoStatusDisplayType.OPPORTUNISTIC_NOKEY;
            case SIGN_ONLY:
                return CryptoStatusDisplayType.SIGN_ONLY;
            default:
            case DISABLE:
                return CryptoStatusDisplayType.DISABLED;
        }
    }

    public boolean shouldUsePgpMessageBuilder() {
        return cryptoMode != CryptoMode.DISABLE;
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
}
