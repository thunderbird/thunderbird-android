package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.List;

import com.fsck.k9.activity.compose.RecipientMvpView.CryptoStatusDisplayType;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoProviderState;
import com.fsck.k9.ui.crypto.CryptoMethod;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.RecipientCryptoStatus;

/** This is an immutable object which contains all relevant metadata entered
 * during e-mail composition to apply cryptographic operations before sending
 * or saving as draft.
 */
public class ComposeCryptoStatus {


    private CryptoProviderState cryptoProviderState;
    private CryptoMethod cryptoMethod;
    private CryptoMode cryptoMode;

    private boolean hasRecipients;
    private String[] recipientAddresses;

    private boolean allKeysAvailable;
    private boolean allKeysVerified;
    private Long signingKeyId;
    private Long selfEncryptKeyId;


    private boolean allCertificatesAvailable;
    private boolean allCertificatesVerified;
    private Long signingCertificateId;
    private Long selfEncryptCertificateId;


    public long[] getEncryptKeyIds() {
        if (selfEncryptKeyId == null) {
            return null;
        }
        return new long[] { selfEncryptKeyId };
    }

    public long[] getEncryptCertificateIds() {
        if (selfEncryptCertificateId == null) {
            return null;
        }
        return new long[] { selfEncryptCertificateId };
    }

    public String[] getRecipientAddresses() {
        return recipientAddresses;
    }

    public Long getSigningKeyId() {
        return signingKeyId;
    }

    public CryptoStatusDisplayType getCryptoStatusDisplayType() {
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
                throw new AssertionError("all CryptoProviderStates must be handled, this is a bug!");
        }

        switch (cryptoMode) {
            case PRIVATE:
                if (!hasRecipients) {
                    return CryptoStatusDisplayType.PRIVATE_EMPTY;
                } else if (cryptoMethod == CryptoMethod.OPENPGP && allKeysAvailable
                        && allKeysVerified) {
                    return CryptoStatusDisplayType.PRIVATE_TRUSTED;
                } else if (cryptoMethod == CryptoMethod.SMIME && allCertificatesAvailable
                        && allCertificatesVerified) {
                    return CryptoStatusDisplayType.PRIVATE_TRUSTED;
                } else if (cryptoMethod == CryptoMethod.OPENPGP && allKeysAvailable) {
                    return CryptoStatusDisplayType.PRIVATE_UNTRUSTED;
                } else if (cryptoMethod == CryptoMethod.SMIME && allCertificatesAvailable) {
                    return CryptoStatusDisplayType.PRIVATE_UNTRUSTED;
                }
                return CryptoStatusDisplayType.PRIVATE_NOKEY;
            case OPPORTUNISTIC:
                if (!hasRecipients) {
                    return CryptoStatusDisplayType.OPPORTUNISTIC_EMPTY;
                } else if (cryptoMethod == CryptoMethod.OPENPGP && allKeysAvailable
                        && allKeysVerified) {
                    return CryptoStatusDisplayType.OPPORTUNISTIC_TRUSTED;
                } else if (cryptoMethod == CryptoMethod.SMIME && allCertificatesAvailable
                        && allCertificatesVerified) {
                    return CryptoStatusDisplayType.OPPORTUNISTIC_TRUSTED;
                } else if (cryptoMethod == CryptoMethod.OPENPGP && allKeysAvailable) {
                    return CryptoStatusDisplayType.OPPORTUNISTIC_UNTRUSTED;
                } else if (cryptoMethod == CryptoMethod.SMIME && allCertificatesAvailable) {
                    return CryptoStatusDisplayType.OPPORTUNISTIC_UNTRUSTED;
                }
                return CryptoStatusDisplayType.OPPORTUNISTIC_NOKEY;
            case SIGN_ONLY:
                return CryptoStatusDisplayType.SIGN_ONLY;
            case DISABLE:
                return CryptoStatusDisplayType.DISABLED;
            default:
                throw new AssertionError("all CryptoModes must be handled, this is a bug!");
        }
    }

    public boolean shouldUsePgpMessageBuilder() {
        return cryptoProviderState != CryptoProviderState.UNCONFIGURED
                && cryptoMethod == CryptoMethod.OPENPGP
                && cryptoMode != CryptoMode.DISABLE;
    }

    public boolean shouldUseSmimeMessageBuilder() {
        return cryptoProviderState != CryptoProviderState.UNCONFIGURED
                && cryptoMethod == CryptoMethod.SMIME
                && cryptoMode != CryptoMode.DISABLE;
    }

    public boolean isEncryptionEnabled() {
        return cryptoMode == CryptoMode.PRIVATE || cryptoMode == CryptoMode.OPPORTUNISTIC;
    }

    public boolean isEncryptionOpportunistic() {
        return cryptoMode == CryptoMode.OPPORTUNISTIC;
    }

    public boolean isSigningEnabled() {
        return cryptoMode != CryptoMode.DISABLE && signingKeyId != null;
    }


    public static class ComposeCryptoStatusBuilder {

        private CryptoMethod cryptoMethod;
        private CryptoProviderState cryptoProviderState;
        private CryptoMode cryptoMode;
        private Long signingKeyId;
        private Long selfEncryptKeyId;
        private Long signingCertificateId;
        private Long selfEncryptCertificateId;
        private List<Recipient> recipients;

        public ComposeCryptoStatusBuilder setCryptoMethod(CryptoMethod cryptoMethod) {
            this.cryptoMethod = cryptoMethod;
            return this;
        }

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

        public ComposeCryptoStatusBuilder setSelfEncryptKeyId(long selfEncryptKeyId) {
            this.selfEncryptKeyId = selfEncryptKeyId;
            return this;
        }

        public ComposeCryptoStatusBuilder setSigningCertificateId(long signingCertificateId) {
            this.signingCertificateId = signingCertificateId;
            return this;
        }

        public ComposeCryptoStatusBuilder setSelfEncryptCertificateId(long selfEncryptCertificateId) {
            this.selfEncryptCertificateId = selfEncryptCertificateId;
            return this;
        }

        public ComposeCryptoStatusBuilder setRecipients(List<Recipient> recipients) {
            this.recipients = recipients;
            return this;
        }

        public ComposeCryptoStatus build() {
            if (cryptoProviderState == null) {
                throw new AssertionError("cryptoProviderState must be set. this is a bug!");
            }
            if (cryptoMode == null) {
                throw new AssertionError("crypto mode must be set. this is a bug!");
            }
            if (recipients == null) {
                throw new AssertionError("recipients must be set. this is a bug!");
            }

            ArrayList<String> recipientAddresses = new ArrayList<>();
            boolean allKeysAvailable = true, allCertificatesAvailable = true;
            boolean allKeysVerified = true, allCertificatesVerified = true;
            boolean hasRecipients = !recipients.isEmpty();
            for (Recipient recipient : recipients) {
                recipientAddresses.add(recipient.address.getAddress());
                RecipientCryptoStatus openPgpCryptoStatus = recipient.getCryptoStatus(CryptoMethod.OPENPGP);
                if (openPgpCryptoStatus.isAvailable()) {
                    if (openPgpCryptoStatus == RecipientCryptoStatus.AVAILABLE_UNTRUSTED) {
                        allKeysVerified = false;
                    }
                } else {
                    allKeysAvailable = false;
                }
                RecipientCryptoStatus smimeCryptoStatus = recipient.getCryptoStatus(CryptoMethod.SMIME);
                if (smimeCryptoStatus.isAvailable()) {
                    if (openPgpCryptoStatus == RecipientCryptoStatus.AVAILABLE_UNTRUSTED) {
                        allCertificatesVerified = false;
                    }
                } else {
                    allCertificatesAvailable = false;
                }
            }

            ComposeCryptoStatus result = new ComposeCryptoStatus();
            result.cryptoProviderState = cryptoProviderState;
            result.cryptoMode = cryptoMode;
            result.cryptoMethod = cryptoMethod;
            result.recipientAddresses = recipientAddresses.toArray(new String[0]);
            result.hasRecipients = hasRecipients;

            result.allKeysAvailable = allKeysAvailable;
            result.allKeysVerified = allKeysVerified;
            result.signingKeyId = signingKeyId;
            result.selfEncryptKeyId = selfEncryptKeyId;

            result.allCertificatesAvailable = allCertificatesAvailable;
            result.allCertificatesVerified = allCertificatesVerified;
            result.signingCertificateId = signingCertificateId;
            result.selfEncryptCertificateId = selfEncryptCertificateId;
            return result;
        }
    }

    public enum SendErrorState {
        PROVIDER_ERROR,
        SIGN_KEY_NOT_CONFIGURED, PRIVATE_BUT_MISSING_KEYS,
        SIGN_CERTIFICATE_NOT_CONFIGURED, PRIVATE_BUT_MISSING_CERTIFICATES
    }

    public SendErrorState getSendErrorStateOrNull() {
        if (cryptoProviderState != CryptoProviderState.OK) {
            // TODO: be more specific about this error
            return SendErrorState.PROVIDER_ERROR;
        }
        if (cryptoMethod == CryptoMethod.OPENPGP) {
            boolean isSignKeyMissing = signingKeyId == null;
            if (isSignKeyMissing) {
                return SendErrorState.SIGN_KEY_NOT_CONFIGURED;
            }
            boolean isPrivateModeAndNotAllKeysAvailable = cryptoMode == CryptoMode.PRIVATE
                    && !allKeysAvailable;
            if (isPrivateModeAndNotAllKeysAvailable) {
                return SendErrorState.PRIVATE_BUT_MISSING_KEYS;
            }
        } else if (cryptoMethod == CryptoMethod.SMIME) {
            boolean isSignCertificateMissing = signingCertificateId == null;
            if (isSignCertificateMissing) {
                return SendErrorState.SIGN_CERTIFICATE_NOT_CONFIGURED;
            }
            boolean isPrivateModeAndNotAllCertificatesAvailable = cryptoMode == CryptoMode.PRIVATE
                    && !allCertificatesAvailable;
            if (isPrivateModeAndNotAllCertificatesAvailable) {
                return SendErrorState.PRIVATE_BUT_MISSING_CERTIFICATES;
            }

        }

        return null;
    }

}
