package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.view.Menu;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.compose.ComposeCryptoStatus.AttachErrorState;
import com.fsck.k9.activity.compose.ComposeCryptoStatus.ComposeCryptoStatusBuilder;
import com.fsck.k9.activity.compose.ComposeCryptoStatus.SendErrorState;
import com.fsck.k9.activity.compose.RecipientMvpView.CryptoStatusDisplayType;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MailTo;
import com.fsck.k9.helper.ReplyToParser;
import com.fsck.k9.helper.ReplyToParser.ReplyToAddresses;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.message.AutocryptStatusInteractor;
import com.fsck.k9.message.AutocryptStatusInteractor.RecipientAutocryptStatus;
import com.fsck.k9.message.ComposePgpEnableByDefaultDecider;
import com.fsck.k9.message.ComposePgpInlineDecider;
import com.fsck.k9.message.MessageBuilder;
import com.fsck.k9.message.PgpMessageBuilder;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.PermissionPingCallback;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpServiceConnection.OnBound;
import timber.log.Timber;


public class RecipientPresenter implements PermissionPingCallback {
    private static final String STATE_KEY_CC_SHOWN = "state:ccShown";
    private static final String STATE_KEY_BCC_SHOWN = "state:bccShown";
    private static final String STATE_KEY_LAST_FOCUSED_TYPE = "state:lastFocusedType";
    private static final String STATE_KEY_CURRENT_CRYPTO_MODE = "state:currentCryptoMode";
    private static final String STATE_KEY_CRYPTO_ENABLE_PGP_INLINE = "state:cryptoEnablePgpInline";

    private static final int CONTACT_PICKER_TO = 1;
    private static final int CONTACT_PICKER_CC = 2;
    private static final int CONTACT_PICKER_BCC = 3;
    private static final int OPENPGP_USER_INTERACTION = 4;
    private static final int REQUEST_CODE_AUTOCRYPT = 5;

    private static final int PGP_DIALOG_DISPLAY_THRESHOLD = 2;


    // transient state, which is either obtained during construction and initialization, or cached
    private final Context context;
    private final RecipientMvpView recipientMvpView;
    private final ComposePgpEnableByDefaultDecider composePgpEnableByDefaultDecider;
    private final ComposePgpInlineDecider composePgpInlineDecider;
    private final AutocryptStatusInteractor autocryptStatusInteractor;
    private final RecipientsChangedListener listener;
    private ReplyToParser replyToParser;
    private Account account;
    private String openPgpProvider;
    private Boolean hasContactPicker;
    private PendingIntent pendingUserInteractionIntent;
    private CryptoProviderState cryptoProviderState = CryptoProviderState.UNCONFIGURED;
    private OpenPgpServiceConnection openPgpServiceConnection;
    @Nullable
    private ComposeCryptoStatus cachedCryptoStatus;


    // persistent state, saved during onSaveInstanceState
    private RecipientType lastFocusedType = RecipientType.TO;
    private CryptoMode currentCryptoMode = CryptoMode.NO_CHOICE;
    private boolean cryptoEnablePgpInline = false;


    public RecipientPresenter(Context context, LoaderManager loaderManager,
            RecipientMvpView recipientMvpView, Account account, ComposePgpInlineDecider composePgpInlineDecider,
            ComposePgpEnableByDefaultDecider composePgpEnableByDefaultDecider,
            AutocryptStatusInteractor autocryptStatusInteractor,
            ReplyToParser replyToParser, RecipientsChangedListener recipientsChangedListener) {
        this.recipientMvpView = recipientMvpView;
        this.context = context;
        this.autocryptStatusInteractor = autocryptStatusInteractor;
        this.composePgpInlineDecider = composePgpInlineDecider;
        this.composePgpEnableByDefaultDecider = composePgpEnableByDefaultDecider;
        this.replyToParser = replyToParser;
        this.listener = recipientsChangedListener;

        recipientMvpView.setPresenter(this);
        recipientMvpView.setLoaderManager(loaderManager);
        onSwitchAccount(account);
    }

    public List<Address> getToAddresses() {
        return recipientMvpView.getToAddresses();
    }

    public List<Address> getCcAddresses() {
        return recipientMvpView.getCcAddresses();
    }

    public List<Address> getBccAddresses() {
        return recipientMvpView.getBccAddresses();
    }

    private List<Recipient> getAllRecipients() {
        ArrayList<Recipient> result = new ArrayList<>();

        result.addAll(recipientMvpView.getToRecipients());
        result.addAll(recipientMvpView.getCcRecipients());
        result.addAll(recipientMvpView.getBccRecipients());

        return result;
    }

    public boolean checkRecipientsOkForSending() {
        recipientMvpView.recipientToTryPerformCompletion();
        recipientMvpView.recipientCcTryPerformCompletion();
        recipientMvpView.recipientBccTryPerformCompletion();

        if (recipientMvpView.recipientToHasUncompletedText()) {
            recipientMvpView.showToUncompletedError();
            return true;
        }

        if (recipientMvpView.recipientCcHasUncompletedText()) {
            recipientMvpView.showCcUncompletedError();
            return true;
        }

        if (recipientMvpView.recipientBccHasUncompletedText()) {
            recipientMvpView.showBccUncompletedError();
            return true;
        }

        if (getToAddresses().isEmpty() && getCcAddresses().isEmpty() && getBccAddresses().isEmpty()) {
            recipientMvpView.showNoRecipientsError();
            return true;
        }

        return false;
    }

    public void initFromReplyToMessage(Message message, boolean isReplyAll) {
        ReplyToAddresses replyToAddresses = isReplyAll ?
                replyToParser.getRecipientsToReplyAllTo(message, account) :
                replyToParser.getRecipientsToReplyTo(message, account);

        addToAddresses(replyToAddresses.to);
        addCcAddresses(replyToAddresses.cc);

        boolean shouldSendAsPgpInline = composePgpInlineDecider.shouldReplyInline(message);
        if (shouldSendAsPgpInline) {
            cryptoEnablePgpInline = true;
        }

        boolean shouldEnablePgpByDefault = composePgpEnableByDefaultDecider.shouldEncryptByDefault(message);
        currentCryptoMode = shouldEnablePgpByDefault ? CryptoMode.CHOICE_ENABLED : CryptoMode.NO_CHOICE;
    }

    public void initFromTrustIdAction(String trustId) {
        addToAddresses(Address.parse(trustId));
        currentCryptoMode = CryptoMode.CHOICE_ENABLED;
    }

    public void initFromMailto(MailTo mailTo) {
        addToAddresses(mailTo.getTo());
        addCcAddresses(mailTo.getCc());
        addBccAddresses(mailTo.getBcc());
    }

    public void initFromSendOrViewIntent(Intent intent) {
        String[] extraEmail = intent.getStringArrayExtra(Intent.EXTRA_EMAIL);
        String[] extraCc = intent.getStringArrayExtra(Intent.EXTRA_CC);
        String[] extraBcc = intent.getStringArrayExtra(Intent.EXTRA_BCC);

        if (extraEmail != null) {
            addToAddresses(addressFromStringArray(extraEmail));
        }

        if (extraCc != null) {
            addCcAddresses(addressFromStringArray(extraCc));
        }

        if (extraBcc != null) {
            addBccAddresses(addressFromStringArray(extraBcc));
        }
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        recipientMvpView.setCcVisibility(savedInstanceState.getBoolean(STATE_KEY_CC_SHOWN));
        recipientMvpView.setBccVisibility(savedInstanceState.getBoolean(STATE_KEY_BCC_SHOWN));
        lastFocusedType = RecipientType.valueOf(savedInstanceState.getString(STATE_KEY_LAST_FOCUSED_TYPE));
        currentCryptoMode = CryptoMode.valueOf(savedInstanceState.getString(STATE_KEY_CURRENT_CRYPTO_MODE));
        cryptoEnablePgpInline = savedInstanceState.getBoolean(STATE_KEY_CRYPTO_ENABLE_PGP_INLINE);
        updateRecipientExpanderVisibility();
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_KEY_CC_SHOWN, recipientMvpView.isCcVisible());
        outState.putBoolean(STATE_KEY_BCC_SHOWN, recipientMvpView.isBccVisible());
        outState.putString(STATE_KEY_LAST_FOCUSED_TYPE, lastFocusedType.toString());
        outState.putString(STATE_KEY_CURRENT_CRYPTO_MODE, currentCryptoMode.toString());
        outState.putBoolean(STATE_KEY_CRYPTO_ENABLE_PGP_INLINE, cryptoEnablePgpInline);
    }

    public void initFromDraftMessage(Message message) {
        initRecipientsFromDraftMessage(message);
        initPgpInlineFromDraftMessage(message);
    }

    private void initRecipientsFromDraftMessage(Message message) {
        addToAddresses(message.getRecipients(RecipientType.TO));

        Address[] ccRecipients = message.getRecipients(RecipientType.CC);
        addCcAddresses(ccRecipients);

        Address[] bccRecipients = message.getRecipients(RecipientType.BCC);
        addBccAddresses(bccRecipients);
    }

    private void initPgpInlineFromDraftMessage(Message message) {
        cryptoEnablePgpInline = message.isSet(Flag.X_DRAFT_OPENPGP_INLINE);
    }

    private void addToAddresses(Address... toAddresses) {
        addRecipientsFromAddresses(RecipientType.TO, toAddresses);
    }

    private void addCcAddresses(Address... ccAddresses) {
        if (ccAddresses.length > 0) {
            addRecipientsFromAddresses(RecipientType.CC, ccAddresses);
            recipientMvpView.setCcVisibility(true);
            updateRecipientExpanderVisibility();
        }
    }

    public void addBccAddresses(Address... bccRecipients) {
        if (bccRecipients.length > 0) {
            addRecipientsFromAddresses(RecipientType.BCC, bccRecipients);
            String bccAddress = account.getAlwaysBcc();

            // If the auto-bcc is the only entry in the BCC list, don't show the Bcc fields.
            boolean alreadyVisible = recipientMvpView.isBccVisible();
            boolean singleBccRecipientFromAccount =
                    bccRecipients.length == 1 && bccRecipients[0].toString().equals(bccAddress);
            recipientMvpView.setBccVisibility(alreadyVisible || !singleBccRecipientFromAccount);
            updateRecipientExpanderVisibility();
        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
        ComposeCryptoStatus currentCryptoStatus = getCurrentCachedCryptoStatus();
        boolean isCryptoConfigured = currentCryptoStatus != null && currentCryptoStatus.isProviderStateOk();
        if (isCryptoConfigured) {
            boolean pgpInlineModeEnabled = currentCryptoStatus.isPgpInlineModeEnabled();
            menu.findItem(R.id.openpgp_inline_enable).setVisible(!pgpInlineModeEnabled);
            menu.findItem(R.id.openpgp_inline_disable).setVisible(pgpInlineModeEnabled);

            boolean isEncrypting = currentCryptoStatus.isEncryptionEnabled();
            menu.findItem(R.id.openpgp_encrypt_enable).setVisible(!isEncrypting);
            menu.findItem(R.id.openpgp_encrypt_disable).setVisible(isEncrypting);

            boolean showSignOnly = K9.getOpenPgpSupportSignOnly();
            boolean isSignOnly = currentCryptoStatus.isSignOnly();
            menu.findItem(R.id.openpgp_sign_only).setVisible(showSignOnly && !isSignOnly);
            menu.findItem(R.id.openpgp_sign_only_disable).setVisible(showSignOnly && isSignOnly);
        } else {
            menu.findItem(R.id.openpgp_inline_enable).setVisible(false);
            menu.findItem(R.id.openpgp_inline_disable).setVisible(false);
            menu.findItem(R.id.openpgp_encrypt_enable).setVisible(false);
            menu.findItem(R.id.openpgp_encrypt_disable).setVisible(false);
            menu.findItem(R.id.openpgp_sign_only).setVisible(false);
            menu.findItem(R.id.openpgp_sign_only_disable).setVisible(false);
        }

        boolean noContactPickerAvailable = !hasContactPicker();
        if (noContactPickerAvailable) {
            menu.findItem(R.id.add_from_contacts).setVisible(false);
        }
    }

    public void onSwitchAccount(Account account) {
        this.account = account;

        if (account.isAlwaysShowCcBcc()) {
            recipientMvpView.setCcVisibility(true);
            recipientMvpView.setBccVisibility(true);
            updateRecipientExpanderVisibility();
        }

        // This does not strictly depend on the account, but this is as good a point to set this as any
        setupCryptoProvider();
    }

    @SuppressWarnings("UnusedParameters")
    public void onSwitchIdentity(Identity identity) {

        // TODO decide what actually to do on identity switch?
        /*
        if (mIdentityChanged) {
            mBccWrapper.setVisibility(View.VISIBLE);
        }
        mBccView.setText("");
        mBccView.addAddress(new Address(mAccount.getAlwaysBcc(), ""));
        */

    }

    private static Address[] addressFromStringArray(String[] addresses) {
        return addressFromStringArray(Arrays.asList(addresses));
    }

    private static Address[] addressFromStringArray(List<String> addresses) {
        ArrayList<Address> result = new ArrayList<>(addresses.size());

        for (String addressStr : addresses) {
            Collections.addAll(result, Address.parseUnencoded(addressStr));
        }

        return result.toArray(new Address[result.size()]);
    }

    void onClickToLabel() {
        recipientMvpView.requestFocusOnToField();
    }

    void onClickCcLabel() {
        recipientMvpView.requestFocusOnCcField();
    }

    void onClickBccLabel() {
        recipientMvpView.requestFocusOnBccField();
    }

    void onClickRecipientExpander() {
        recipientMvpView.setCcVisibility(true);
        recipientMvpView.setBccVisibility(true);
        updateRecipientExpanderVisibility();
    }

    private void hideEmptyExtendedRecipientFields() {
        if (recipientMvpView.getCcAddresses().isEmpty()) {
            recipientMvpView.setCcVisibility(false);
            if (lastFocusedType == RecipientType.CC) {
                lastFocusedType = RecipientType.TO;
            }
        }
        if (recipientMvpView.getBccAddresses().isEmpty()) {
            recipientMvpView.setBccVisibility(false);
            if (lastFocusedType == RecipientType.BCC) {
                lastFocusedType = RecipientType.TO;
            }
        }
        updateRecipientExpanderVisibility();
    }

    private void updateRecipientExpanderVisibility() {
        boolean notBothAreVisible = !(recipientMvpView.isCcVisible() && recipientMvpView.isBccVisible());
        recipientMvpView.setRecipientExpanderVisibility(notBothAreVisible);
    }

    public void asyncUpdateCryptoStatus() {
        cachedCryptoStatus = null;

        boolean isOkStateButLostConnection = cryptoProviderState == CryptoProviderState.OK &&
                (openPgpServiceConnection == null || !openPgpServiceConnection.isBound());
        if (isOkStateButLostConnection) {
            setCryptoProviderState(CryptoProviderState.LOST_CONNECTION);
            pendingUserInteractionIntent = null;
        }

        Long accountCryptoKey = account.getCryptoKey();
        if (accountCryptoKey == Account.NO_OPENPGP_KEY) {
            accountCryptoKey = null;
        }

        final ComposeCryptoStatus composeCryptoStatus = new ComposeCryptoStatusBuilder()
                .setCryptoProviderState(cryptoProviderState)
                .setCryptoMode(currentCryptoMode)
                .setEnablePgpInline(cryptoEnablePgpInline)
                .setRecipients(getAllRecipients())
                .setOpenPgpKeyId(accountCryptoKey)
                .build();

        final String[] recipientAddresses = composeCryptoStatus.getRecipientAddresses();

        new AsyncTask<Void,Void,RecipientAutocryptStatus>() {
            @Override
            protected RecipientAutocryptStatus doInBackground(Void... voids) {
                if (cryptoProviderState != CryptoProviderState.OK) {
                    return null;
                }

                return autocryptStatusInteractor.retrieveCryptoProviderRecipientStatus(
                                getOpenPgpApi(), recipientAddresses);
            }

            @Override
            protected void onPostExecute(RecipientAutocryptStatus recipientAutocryptStatus) {
                if (recipientAutocryptStatus != null) {
                    cachedCryptoStatus = composeCryptoStatus.withRecipientAutocryptStatus(recipientAutocryptStatus);
                } else {
                    cachedCryptoStatus = composeCryptoStatus;
                }

                redrawCachedCryptoStatusIcon();
            }
        }.execute();
    }

    private void redrawCachedCryptoStatusIcon() {
        if (cachedCryptoStatus == null) {
            throw new IllegalStateException("must have cached crypto status to redraw it!");
        }

        recipientMvpView.setRecipientTokensShowCryptoEnabled(cachedCryptoStatus.isEncryptionEnabled());

        CryptoStatusDisplayType cryptoStatusDisplayType = cachedCryptoStatus.getCryptoStatusDisplayType();
        recipientMvpView.showCryptoStatus(cryptoStatusDisplayType);
        recipientMvpView.showCryptoSpecialMode(cachedCryptoStatus.getCryptoSpecialModeDisplayType());
    }

    @Nullable
    public ComposeCryptoStatus getCurrentCachedCryptoStatus() {
        return cachedCryptoStatus;
    }

    public boolean isForceTextMessageFormat() {
        return cryptoEnablePgpInline;
    }

    void onToTokenAdded() {
        asyncUpdateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onToTokenRemoved() {
        asyncUpdateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onToTokenChanged() {
        asyncUpdateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onCcTokenAdded() {
        asyncUpdateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onCcTokenRemoved() {
        asyncUpdateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onCcTokenChanged() {
        asyncUpdateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onBccTokenAdded() {
        asyncUpdateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onBccTokenRemoved() {
        asyncUpdateCryptoStatus();
        listener.onRecipientsChanged();
    }

    void onBccTokenChanged() {
        asyncUpdateCryptoStatus();
        listener.onRecipientsChanged();
    }

    public void onCryptoModeChanged(CryptoMode cryptoMode) {
        currentCryptoMode = cryptoMode;
        asyncUpdateCryptoStatus();
    }

    public void onCryptoPgpInlineChanged(boolean enablePgpInline) {
        cryptoEnablePgpInline = enablePgpInline;
        asyncUpdateCryptoStatus();
    }

    private void addRecipientsFromAddresses(final RecipientType recipientType, final Address... addresses) {
        new RecipientLoader(context, openPgpProvider, addresses) {
            @Override
            public void deliverResult(List<Recipient> result) {
                Recipient[] recipientArray = result.toArray(new Recipient[result.size()]);
                recipientMvpView.addRecipients(recipientType, recipientArray);

                stopLoading();
                abandon();
            }
        }.startLoading();
    }

    private void addRecipientFromContactUri(final RecipientType recipientType, final Uri uri) {
        new RecipientLoader(context, openPgpProvider, uri, false) {
            @Override
            public void deliverResult(List<Recipient> result) {
                // TODO handle multiple available mail addresses for a contact?
                if (result.isEmpty()) {
                    recipientMvpView.showErrorContactNoAddress();
                    return;
                }

                Recipient recipient = result.get(0);
                recipientMvpView.addRecipients(recipientType, recipient);

                stopLoading();
                abandon();
            }
        }.startLoading();
    }

    void onToFocused() {
        lastFocusedType = RecipientType.TO;
    }

    void onCcFocused() {
        lastFocusedType = RecipientType.CC;
    }

    void onBccFocused() {
        lastFocusedType = RecipientType.BCC;
    }

    public void onMenuAddFromContacts() {
        int requestCode = recipientTypeToRequestCode(lastFocusedType);
        recipientMvpView.showContactPicker(requestCode);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CONTACT_PICKER_TO:
            case CONTACT_PICKER_CC:
            case CONTACT_PICKER_BCC:
                if (resultCode != Activity.RESULT_OK || data == null) {
                    return;
                }
                RecipientType recipientType = recipientTypeFromRequestCode(requestCode);
                addRecipientFromContactUri(recipientType, data.getData());
                break;
            case OPENPGP_USER_INTERACTION:
                cryptoProviderBindOrCheckPermission();
                break;
            case REQUEST_CODE_AUTOCRYPT:
                asyncUpdateCryptoStatus();
                break;
        }
    }

    private static int recipientTypeToRequestCode(RecipientType type) {
        switch (type) {
            case TO: {
                return CONTACT_PICKER_TO;
            }
            case CC: {
                return CONTACT_PICKER_CC;
            }
            case BCC: {
                return CONTACT_PICKER_BCC;
            }
        }

        throw new AssertionError("Unhandled case: " + type);
    }

    private static RecipientType recipientTypeFromRequestCode(int type) {
        switch (type) {
            case CONTACT_PICKER_TO: {
                return RecipientType.TO;
            }
            case CONTACT_PICKER_CC: {
                return RecipientType.CC;
            }
            case CONTACT_PICKER_BCC: {
                return RecipientType.BCC;
            }
        }

        throw new AssertionError("Unhandled case: " + type);
    }

    public void onNonRecipientFieldFocused() {
        if (!account.isAlwaysShowCcBcc()) {
            hideEmptyExtendedRecipientFields();
        }
    }

    void onClickCryptoStatus() {
        switch (cryptoProviderState) {
            case UNCONFIGURED:
                Timber.e("click on crypto status while unconfigured - this should not really happen?!");
                return;
            case OK:
                ComposeCryptoStatus currentCryptoStatus = getCurrentCachedCryptoStatus();
                if (currentCryptoStatus == null) {
                    Timber.e("click on crypto status while crypto status not available - should not really happen?!");
                    return;
                }

                if (currentCryptoStatus.isEncryptionEnabledError()) {
                    recipientMvpView.showOpenPgpEnabledErrorDialog(false);
                    return;
                }

                if (currentCryptoMode == CryptoMode.SIGN_ONLY) {
                    recipientMvpView.showErrorIsSignOnly();
                    return;
                }

                if (currentCryptoMode == CryptoMode.NO_CHOICE) {
                    if (currentCryptoStatus.hasAutocryptPendingIntent()) {
                        recipientMvpView.launchUserInteractionPendingIntent(
                                currentCryptoStatus.getAutocryptPendingIntent(), REQUEST_CODE_AUTOCRYPT);
                    } else if (currentCryptoStatus.canEncryptAndIsMutual()) {
                        onCryptoModeChanged(CryptoMode.CHOICE_DISABLED);
                    } else {
                        onCryptoModeChanged(CryptoMode.CHOICE_ENABLED);
                    }
                    return;
                }

                if (currentCryptoMode == CryptoMode.CHOICE_DISABLED && !currentCryptoStatus.canEncryptAndIsMutual()) {
                    onCryptoModeChanged(CryptoMode.CHOICE_ENABLED);
                    return;
                }

                onCryptoModeChanged(CryptoMode.NO_CHOICE);
                return;

            case LOST_CONNECTION:
            case UNINITIALIZED:
            case ERROR:
                cryptoProviderBindOrCheckPermission();
        }
    }

    /**
     * Does the device actually have a Contacts application suitable for
     * picking a contact. As hard as it is to believe, some vendors ship
     * without it.
     *
     * @return True, if the device supports picking contacts. False, otherwise.
     */
    private boolean hasContactPicker() {
        if (hasContactPicker == null) {
            Contacts contacts = Contacts.getInstance(context);

            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(contacts.contactPickerIntent(), 0);
            hasContactPicker = !resolveInfoList.isEmpty();
        }

        return hasContactPicker;
    }

    public void showPgpSendError(SendErrorState sendErrorState) {
        switch (sendErrorState) {
            case ENABLED_ERROR:
                recipientMvpView.showOpenPgpEnabledErrorDialog(false);
                break;
            case PROVIDER_ERROR:
                recipientMvpView.showErrorOpenPgpConnection();
                break;
            default:
                throw new AssertionError("not all error states handled, this is a bug!");
        }
    }

    void showPgpAttachError(AttachErrorState attachErrorState) {
        switch (attachErrorState) {
            case IS_INLINE:
                recipientMvpView.showErrorInlineAttach();
                break;
            default:
                throw new AssertionError("not all error states handled, this is a bug!");
        }
    }

    private void setupCryptoProvider() {
        String openPgpProvider = K9.getOpenPgpProvider();
        if (TextUtils.isEmpty(openPgpProvider)) {
            openPgpProvider = null;
        }

        boolean providerIsBound = openPgpServiceConnection != null && openPgpServiceConnection.isBound();
        boolean isSameProvider = openPgpProvider != null && openPgpProvider.equals(this.openPgpProvider);
        if (isSameProvider && providerIsBound) {
            cryptoProviderBindOrCheckPermission();
            return;
        }

        if (providerIsBound) {
            openPgpServiceConnection.unbindFromService();
            openPgpServiceConnection = null;
        }

        this.openPgpProvider = openPgpProvider;

        if (openPgpProvider == null) {
            setCryptoProviderState(CryptoProviderState.UNCONFIGURED);
            return;
        }

        setCryptoProviderState(CryptoProviderState.UNINITIALIZED);
        openPgpServiceConnection = new OpenPgpServiceConnection(context, openPgpProvider, new OnBound() {
            @Override
            public void onBound(IOpenPgpService2 service) {
                cryptoProviderBindOrCheckPermission();
            }

            @Override
            public void onError(Exception e) {
                onCryptoProviderError(e);
            }
        });
        cryptoProviderBindOrCheckPermission();

        recipientMvpView.setCryptoProvider(openPgpProvider);
    }

    private void cryptoProviderBindOrCheckPermission() {
        if (openPgpServiceConnection == null) {
            setCryptoProviderState(CryptoProviderState.UNCONFIGURED);
            return;
        }

        if (!openPgpServiceConnection.isBound()) {
            pendingUserInteractionIntent = null;
            openPgpServiceConnection.bindToService();
            return;
        }

        if (pendingUserInteractionIntent != null) {
            recipientMvpView
                    .launchUserInteractionPendingIntent(pendingUserInteractionIntent, OPENPGP_USER_INTERACTION);
            pendingUserInteractionIntent = null;
            return;
        }

        getOpenPgpApi().checkPermissionPing(this);
    }

    private void onCryptoProviderError(Exception e) {
        // TODO handle error case better
        recipientMvpView.showErrorOpenPgpConnection();
        setCryptoProviderState(CryptoProviderState.ERROR);
        Timber.e(e, "error connecting to crypto provider!");
        asyncUpdateCryptoStatus();
    }

    @Override
    public void onPgpPermissionCheckResult(Intent result) {
        int resultCode = result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
        switch (resultCode) {
            case OpenPgpApi.RESULT_CODE_SUCCESS:
                setCryptoProviderState(CryptoProviderState.OK);
                break;

            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                recipientMvpView.showErrorOpenPgpUserInteractionRequired();
                pendingUserInteractionIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                setCryptoProviderState(CryptoProviderState.ERROR);
                break;

            case OpenPgpApi.RESULT_CODE_ERROR:
            default:
                if (result.hasExtra(OpenPgpApi.RESULT_ERROR)) {
                    OpenPgpError error = result.getParcelableExtra(OpenPgpApi.RESULT_ERROR);
                    if (error != null) {
                        handleOpenPgpError(error);
                    } else {
                        recipientMvpView.showErrorOpenPgpConnection();
                        setCryptoProviderState(CryptoProviderState.ERROR);
                    }
                } else {
                    recipientMvpView.showErrorOpenPgpConnection();
                    setCryptoProviderState(CryptoProviderState.ERROR);
                }
                break;
        }
    }

    private void handleOpenPgpError(OpenPgpError error) {
        Timber.e("OpenPGP Api error: %s", error);

        if (error.getErrorId() == OpenPgpError.INCOMPATIBLE_API_VERSIONS) {
            // nothing we can do here, this is irrecoverable!
            openPgpProvider = null;
            K9.setOpenPgpProvider(null);
            recipientMvpView.showErrorOpenPgpIncompatible();
            setCryptoProviderState(CryptoProviderState.UNCONFIGURED);
        } else {
            recipientMvpView.showErrorOpenPgpConnection();
        }
    }

    private void setCryptoProviderState(CryptoProviderState state) {
        cryptoProviderState = state;

        if (state == CryptoProviderState.OK) {
            recipientMvpView.setCryptoProvider(openPgpProvider);
        } else {
            recipientMvpView.setCryptoProvider(null);
        }

        asyncUpdateCryptoStatus();
    }

    public void onActivityDestroy() {
        if (openPgpServiceConnection != null && openPgpServiceConnection.isBound()) {
            openPgpServiceConnection.unbindFromService();
        }
        openPgpServiceConnection = null;
    }

    private OpenPgpApi getOpenPgpApi() {
        if (openPgpServiceConnection == null || !openPgpServiceConnection.isBound()) {
            Timber.e("obtained openpgpapi object, but service is not bound! inconsistent state?");
        }
        return new OpenPgpApi(context, openPgpServiceConnection.getService());
    }

    public void builderSetProperties(MessageBuilder messageBuilder) {
        if (messageBuilder instanceof PgpMessageBuilder) {
            throw new IllegalArgumentException("PpgMessageBuilder must be called with ComposeCryptoStatus argument!");
        }

        messageBuilder.setTo(getToAddresses());
        messageBuilder.setCc(getCcAddresses());
        messageBuilder.setBcc(getBccAddresses());
    }

    public void builderSetProperties(PgpMessageBuilder pgpMessageBuilder, ComposeCryptoStatus cryptoStatus) {
        pgpMessageBuilder.setTo(getToAddresses());
        pgpMessageBuilder.setCc(getCcAddresses());
        pgpMessageBuilder.setBcc(getBccAddresses());

        pgpMessageBuilder.setOpenPgpApi(getOpenPgpApi());
        pgpMessageBuilder.setCryptoStatus(cryptoStatus);
    }

    public void onMenuSetPgpInline(boolean enablePgpInline) {
        onCryptoPgpInlineChanged(enablePgpInline);
        if (enablePgpInline) {
            boolean shouldShowPgpInlineDialog = checkAndIncrementPgpInlineDialogCounter();
            if (shouldShowPgpInlineDialog) {
                recipientMvpView.showOpenPgpInlineDialog(true);
            }
        }
    }

    public void onMenuSetSignOnly(boolean enableSignOnly) {
        if (enableSignOnly) {
            onCryptoModeChanged(CryptoMode.SIGN_ONLY);
            boolean shouldShowPgpSignOnlyDialog = checkAndIncrementPgpSignOnlyDialogCounter();
            if (shouldShowPgpSignOnlyDialog) {
                recipientMvpView.showOpenPgpSignOnlyDialog(true);
            }
        } else {
            onCryptoModeChanged(CryptoMode.NO_CHOICE);
        }
    }

    public void onMenuSetEnableEncryption(boolean enableEncryption) {
        if (cachedCryptoStatus == null) {
            Timber.e("Received crypto button press while status wasn't initialized?");
            return;
        }
        if (enableEncryption) {
            if (!cachedCryptoStatus.canEncrypt()) {
                onCryptoModeChanged(CryptoMode.CHOICE_ENABLED);
                recipientMvpView.showOpenPgpEnabledErrorDialog(true);
            } else if (cachedCryptoStatus.canEncryptAndIsMutual()) {
                onCryptoModeChanged(CryptoMode.NO_CHOICE);
            } else {
                recipientMvpView.showOpenPgpEncryptExplanationDialog();
                onCryptoModeChanged(CryptoMode.CHOICE_ENABLED);
            }
        } else {
            if (cachedCryptoStatus.canEncryptAndIsMutual()) {
                onCryptoModeChanged(CryptoMode.CHOICE_DISABLED);
            } else {
                onCryptoModeChanged(CryptoMode.NO_CHOICE);
            }
        }
    }

    public void onCryptoPgpClickDisable() {
        onCryptoModeChanged(CryptoMode.NO_CHOICE);
    }

    public void onCryptoPgpSignOnlyDisabled() {
        onCryptoPgpInlineChanged(false);
        onCryptoModeChanged(CryptoMode.NO_CHOICE);
    }

    private boolean checkAndIncrementPgpInlineDialogCounter() {
        int pgpInlineDialogCounter = K9.getPgpInlineDialogCounter();
        if (pgpInlineDialogCounter < PGP_DIALOG_DISPLAY_THRESHOLD) {
            K9.setPgpInlineDialogCounter(pgpInlineDialogCounter + 1);
            K9.saveSettingsAsync();
            return true;
        }
        return false;
    }

    private boolean checkAndIncrementPgpSignOnlyDialogCounter() {
        int pgpSignOnlyDialogCounter = K9.getPgpSignOnlyDialogCounter();
        if (pgpSignOnlyDialogCounter < PGP_DIALOG_DISPLAY_THRESHOLD) {
            K9.setPgpSignOnlyDialogCounter(pgpSignOnlyDialogCounter + 1);
            K9.saveSettingsAsync();
            return true;
        }
        return false;
    }

    void onClickCryptoSpecialModeIndicator() {
        if (currentCryptoMode == CryptoMode.SIGN_ONLY) {
            recipientMvpView.showOpenPgpSignOnlyDialog(false);
        } else if (cryptoEnablePgpInline) {
            recipientMvpView.showOpenPgpInlineDialog(false);
        } else {
            throw new IllegalStateException("This icon should not be clickable while no special mode is active!");
        }
    }

    @VisibleForTesting
    void setOpenPgpServiceConnection(OpenPgpServiceConnection openPgpServiceConnection, String cryptoProvider) {
        this.openPgpServiceConnection = openPgpServiceConnection;
        this.openPgpProvider = cryptoProvider;
    }

    public boolean shouldSaveRemotely() {
        // TODO more appropriate logic?
        return cachedCryptoStatus == null || !cachedCryptoStatus.isEncryptionEnabled();
    }

    public enum CryptoProviderState {
        UNCONFIGURED,
        UNINITIALIZED,
        LOST_CONNECTION,
        ERROR,
        OK
    }

    public enum CryptoMode {
        SIGN_ONLY,
        NO_CHOICE,
        CHOICE_DISABLED,
        CHOICE_ENABLED,
    }

    public interface RecipientsChangedListener {
        void onRecipientsChanged();
    }
}
