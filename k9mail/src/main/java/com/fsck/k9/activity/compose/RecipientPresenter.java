package com.fsck.k9.activity.compose;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.fsck.k9.Account;
import com.fsck.k9.Identity;
import com.fsck.k9.K9;
import com.fsck.k9.R;
import com.fsck.k9.activity.compose.ComposeCryptoStatus.ComposeCryptoStatusBuilder;
import com.fsck.k9.activity.compose.ComposeCryptoStatus.SendErrorState;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MailTo;
import com.fsck.k9.helper.ReplyToParser;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.ui.crypto.CryptoMethod;
import com.fsck.k9.view.RecipientSelectView.Recipient;

import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.OpenPgpServiceConnection.OnBound;
import org.openintents.smime.ISMimeService;
import org.openintents.smime.util.SMimeApi;
import org.openintents.smime.util.SMimeServiceConnection;


public class RecipientPresenter implements OpenPgpApi.PermissionPingCallback, SMimeApi.PermissionPingCallback {
    private static final String STATE_KEY_CC_SHOWN = "state:ccShown";
    private static final String STATE_KEY_BCC_SHOWN = "state:bccShown";
    private static final String STATE_KEY_LAST_FOCUSED_TYPE = "state:lastFocusedType";
    private static final String STATE_KEY_CURRENT_CRYPTO_MODE = "key:initialOrFormerCryptoMode";

    private static final int CONTACT_PICKER_TO = 1;
    private static final int CONTACT_PICKER_CC = 2;
    private static final int CONTACT_PICKER_BCC = 3;
    private static final int OPENPGP_USER_INTERACTION = 4;
    private static final int SMIME_USER_INTERACTION = 4;


    // transient state, which is either obtained during construction and initialization, or cached
    private final Context context;
    private final RecipientMvpView recipientMvpView;
    private Account account;
    private String openPgpProvider;
    private String smimeProvider;
    private Boolean hasContactPicker;
    private ComposeCryptoStatus cachedCryptoStatus;
    private PendingIntent pendingUserInteractionIntent;
    private CryptoProviderState openPgpProviderState = CryptoProviderState.UNCONFIGURED;
    private CryptoProviderState smimeProviderState = CryptoProviderState.UNCONFIGURED;
    //TODO: Add support for switching CryptoMethod
    private CryptoMethod cryptoMethod = CryptoMethod.OPENPGP;
    private OpenPgpServiceConnection openPgpServiceConnection;
    private SMimeServiceConnection smimeServiceConnection;


    // persistent state, saved during onSaveInstanceState
    private RecipientType lastFocusedType = RecipientType.TO;
    // TODO initialize cryptoMode to other values under some circumstances, e.g. if we reply to an encrypted e-mail
    private CryptoMode currentCryptoMode = CryptoMode.OPPORTUNISTIC;


    public RecipientPresenter(Context context, RecipientMvpView recipientMvpView, Account account) {
        this.recipientMvpView = recipientMvpView;
        this.context = context;

        recipientMvpView.setPresenter(this);
        onSwitchAccount(account);
        updateCryptoStatus();
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

    public List<Recipient> getAllRecipients() {
        ArrayList<Recipient> result = new ArrayList<>();

        result.addAll(recipientMvpView.getToRecipients());
        result.addAll(recipientMvpView.getCcRecipients());
        result.addAll(recipientMvpView.getBccRecipients());

        return result;
    }

    public boolean checkRecipientsOkForSending() {
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

    public void initFromReplyToMessage(Message message) {
        Address[] replyToAddresses = ReplyToParser.getRecipientsToReplyTo(message);
        
        try {
            // if we're replying to a message we sent, we probably meant
            // to reply to the recipient of that message
            if (account.isAnIdentity(replyToAddresses)) {
                replyToAddresses = message.getRecipients(RecipientType.TO);
            }

            addRecipientsFromAddresses(RecipientType.TO, replyToAddresses);

            if (message.getReplyTo().length > 0) {
                for (Address address : message.getFrom()) {
                    if (!account.isAnIdentity(address) && !Utility.arrayContains(replyToAddresses, address)) {
                        addRecipientsFromAddresses(RecipientType.TO, address);
                    }
                }
            }

            for (Address address : message.getRecipients(RecipientType.TO)) {
                if (!account.isAnIdentity(address) && !Utility.arrayContains(replyToAddresses, address)) {
                    addToAddresses(address);
                }

            }

            if (message.getRecipients(RecipientType.CC).length > 0) {
                for (Address address : message.getRecipients(RecipientType.CC)) {
                    if (!account.isAnIdentity(address) && !Utility.arrayContains(replyToAddresses, address)) {
                        addCcAddresses(address);
                    }

                }
            }
        } catch (MessagingException e) {
            // can't happen, we know the recipient types exist
            throw new AssertionError(e);
        }
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
        updateRecipientExpanderVisibility();
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_KEY_CC_SHOWN, recipientMvpView.isCcVisible());
        outState.putBoolean(STATE_KEY_BCC_SHOWN, recipientMvpView.isBccVisible());
        outState.putString(STATE_KEY_LAST_FOCUSED_TYPE, lastFocusedType.toString());
        outState.putString(STATE_KEY_CURRENT_CRYPTO_MODE, currentCryptoMode.toString());
    }

    public void initFromDraftMessage(LocalMessage message) {
        try {
            addToAddresses(message.getRecipients(RecipientType.TO));

            Address[] ccRecipients = message.getRecipients(RecipientType.CC);
            addCcAddresses(ccRecipients);

            Address[] bccRecipients = message.getRecipients(RecipientType.BCC);
            addBccAddresses(bccRecipients);
        } catch (MessagingException e) {
            // can't happen, we know the recipient types exist
            throw new AssertionError(e);
        }
    }

    void addToAddresses(Address... toAddresses) {
        addRecipientsFromAddresses(RecipientType.TO, toAddresses);
    }

    void addCcAddresses(Address... ccAddresses) {
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
            recipientMvpView.setBccVisibility(alreadyVisible || singleBccRecipientFromAccount);
            updateRecipientExpanderVisibility();
        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
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

        String openPgpProvider = account.getOpenPgpProvider();
        setOpenPgpProvider(openPgpProvider);

        String smimeProvider = account.getSmimeProvider();
        setSmimeProvider(smimeProvider);
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

    public void onClickToLabel() {
        recipientMvpView.requestFocusOnToField();
    }

    public void onClickCcLabel() {
        recipientMvpView.requestFocusOnCcField();
    }

    public void onClickBccLabel() {
        recipientMvpView.requestFocusOnBccField();
    }

    public void onClickRecipientExpander() {
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

    public void updateCryptoStatus() {
        cachedCryptoStatus = null;

        boolean openPgpIsOkStateButLostConnection =
                openPgpProviderState == CryptoProviderState.OK &&
                    (openPgpServiceConnection == null || !openPgpServiceConnection.isBound());
        if (openPgpIsOkStateButLostConnection) {
            openPgpProviderState = CryptoProviderState.LOST_CONNECTION;
            pendingUserInteractionIntent = null;
        }

        boolean smimeIsOkStateButLostConnection =
                smimeProviderState == CryptoProviderState.OK &&
                        (smimeServiceConnection == null || !smimeServiceConnection.isBound());
        if (smimeIsOkStateButLostConnection) {
            smimeProviderState = CryptoProviderState.LOST_CONNECTION;
            pendingUserInteractionIntent = null;
        }

        recipientMvpView.showCryptoStatus(getCurrentCryptoStatus().getCryptoStatusDisplayType());
    }

    public ComposeCryptoStatus getCurrentCryptoStatus() {
        if (cachedCryptoStatus == null) {
            ComposeCryptoStatusBuilder builder = new ComposeCryptoStatusBuilder()
                    .setCryptoMethod(cryptoMethod)
                    .setCryptoProviderState(cryptoMethod == CryptoMethod.OPENPGP ? openPgpProviderState :
                            cryptoMethod == CryptoMethod.SMIME ? smimeProviderState : null)
                    .setCryptoMode(currentCryptoMode)
                    .setRecipients(getAllRecipients());

            if(cryptoMethod == CryptoMethod.OPENPGP) {
                long accountCryptoKey = account.getOpenPgpKey();
                if (accountCryptoKey != Account.NO_OPENPGP_KEY) {
                    // TODO split these into individual settings? maybe after key is bound to identity
                    builder.setSigningKeyId(accountCryptoKey);
                    builder.setSelfEncryptKeyId(accountCryptoKey);
                }
            } else if (cryptoMethod == CryptoMethod.SMIME) {
                long accountCryptoCertificate = account.getSmimeCertificate();
                if (accountCryptoCertificate != Account.NO_SMIME_CERTIFICATE) {
                    // TODO split these into individual settings? maybe after key is bound to identity
                    builder.setSigningCertificateId(accountCryptoCertificate);
                    builder.setSelfEncryptCertificateId(accountCryptoCertificate);
                }
            } else if (cryptoMethod == CryptoMethod.NONE) {
                builder.setCryptoProviderState(CryptoProviderState.OK);
            } else {
                //TODO: BOTH?
                throw new AssertionError("All crypto methods should be handled");
            }


            cachedCryptoStatus = builder.build();
        }

        return cachedCryptoStatus;
    }

    public boolean isForceTextMessageFormat() {
        ComposeCryptoStatus cryptoStatus = getCurrentCryptoStatus();
        return cryptoStatus.isEncryptionEnabled() || cryptoStatus.isSigningEnabled();
    }

    public boolean isAllowSavingDraftRemotely() {
        ComposeCryptoStatus cryptoStatus = getCurrentCryptoStatus();
        return cryptoStatus.isEncryptionEnabled() || cryptoStatus.isSigningEnabled();
    }

    @SuppressWarnings("UnusedParameters")
    public void onToTokenAdded(Recipient recipient) {
        updateCryptoStatus();
    }

    @SuppressWarnings("UnusedParameters")
    public void onToTokenRemoved(Recipient recipient) {
        updateCryptoStatus();
    }

    @SuppressWarnings("UnusedParameters")
    public void onToTokenChanged(Recipient recipient) {
        updateCryptoStatus();
    }

    @SuppressWarnings("UnusedParameters")
    public void onCcTokenAdded(Recipient recipient) {
        updateCryptoStatus();
    }

    @SuppressWarnings("UnusedParameters")
    public void onCcTokenRemoved(Recipient recipient) {
        updateCryptoStatus();
    }

    @SuppressWarnings("UnusedParameters")
    public void onCcTokenChanged(Recipient recipient) {
        updateCryptoStatus();
    }

    @SuppressWarnings("UnusedParameters")
    public void onBccTokenAdded(Recipient recipient) {
        updateCryptoStatus();
    }

    @SuppressWarnings("UnusedParameters")
    public void onBccTokenRemoved(Recipient recipient) {
        updateCryptoStatus();
    }

    @SuppressWarnings("UnusedParameters")
    public void onBccTokenChanged(Recipient recipient) {
        updateCryptoStatus();
    }

    public void onCryptoModeChanged(CryptoMode cryptoMode) {
        currentCryptoMode = cryptoMode;
        updateCryptoStatus();
    }

    private void addRecipientsFromAddresses(final RecipientType recipientType, final Address... addresses) {
        new RecipientLoader(context, openPgpProvider, smimeProvider, addresses) {
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
        new RecipientLoader(context, openPgpProvider, smimeProvider, uri, false) {
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

    public void onToFocused() {
        lastFocusedType = RecipientType.TO;
    }

    public void onCcFocused() {
        lastFocusedType = RecipientType.CC;
    }

    public void onBccFocused() {
        lastFocusedType = RecipientType.BCC;
    }

    public void onMenuAddFromContacts() {
        int requestCode = recipientTypeToRequestCode(lastFocusedType);
        recipientMvpView.showContactPicker(requestCode);
    }

    public void onActivityResult(int resultCode, int requestCode, Intent data) {
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
                openPgpProviderBindOrCheckPermission();
                break;
        }
    }

    private static int recipientTypeToRequestCode(RecipientType type) {
        switch (type) {
            case TO:
            default: {
                return CONTACT_PICKER_TO;
            }
            case CC: {
                return CONTACT_PICKER_CC;
            }
            case BCC: {
                return CONTACT_PICKER_BCC;
            }
        }
    }

    private static RecipientType recipientTypeFromRequestCode(int type) {
        switch (type) {
            case CONTACT_PICKER_TO:
            default: {
                return RecipientType.TO;
            }
            case CONTACT_PICKER_CC: {
                return RecipientType.CC;
            }
            case CONTACT_PICKER_BCC: {
                return RecipientType.BCC;
            }
        }
    }

    public void onNonRecipientFieldFocused() {
        hideEmptyExtendedRecipientFields();
    }

    public void onClickCryptoStatus() {
        switch(cryptoMethod) {
            case OPENPGP:
                switch (openPgpProviderState) {
                    case UNCONFIGURED:
                        Log.e(K9.LOG_TAG, "click on crypto status while unconfigured - this should not really happen?!");
                        return;
                    case OK:
                        recipientMvpView.showCryptoDialog(currentCryptoMode);
                        return;

                    case LOST_CONNECTION:
                    case UNINITIALIZED:
                    case ERROR:
                        openPgpProviderBindOrCheckPermission();
                        break;
                }
                break;
            case SMIME:
                switch (smimeProviderState) {
                    case UNCONFIGURED:
                        Log.e(K9.LOG_TAG, "click on crypto status while unconfigured - this should not really happen?!");
                        return;
                    case OK:
                        recipientMvpView.showCryptoDialog(currentCryptoMode);
                        return;

                    case LOST_CONNECTION:
                    case UNINITIALIZED:
                    case ERROR:
                        smimeProviderBindOrCheckPermission();
                        break;
                }
                break;
        }
    }

    /**
     * Does the device actually have a Contacts application suitable for
     * picking a contact. As hard as it is to believe, some vendors ship
     * without it.
     *
     * @return True, if the device supports picking contacts. False, otherwise.
     */
    public boolean hasContactPicker() {
        if (hasContactPicker == null) {
            Contacts contacts = Contacts.getInstance(context);

            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> resolveInfoList = packageManager.queryIntentActivities(contacts.contactPickerIntent(), 0);
            hasContactPicker = !resolveInfoList.isEmpty();
        }

        return hasContactPicker;
    }

    public void showCryptoSendError(SendErrorState sendErrorState) {
        switch (sendErrorState) {
            case PROVIDER_ERROR:
                recipientMvpView.showErrorOpenPgpConnection();
                break;
            case SIGN_KEY_NOT_CONFIGURED:
                recipientMvpView.showErrorMissingSignKey();
                break;
            case PRIVATE_BUT_MISSING_KEYS:
                recipientMvpView.showErrorPrivateButMissingKeys();
                break;
            default:
                throw new AssertionError("not all error states handled, this is a bug!");
        }
    }

    private void setOpenPgpProvider(String openPgpProvider) {

        boolean providerIsBound = openPgpServiceConnection != null && openPgpServiceConnection.isBound();
        boolean isSameProvider = openPgpProvider != null && openPgpProvider.equals(this.openPgpProvider);
        if (isSameProvider && providerIsBound) {
            openPgpProviderBindOrCheckPermission();
            return;
        }

        if (providerIsBound) {
            openPgpServiceConnection.unbindFromService();
            openPgpServiceConnection = null;
        }

        this.openPgpProvider = openPgpProvider;

        if (openPgpProvider == null) {
            openPgpProviderState = CryptoProviderState.UNCONFIGURED;
            return;
        }

        openPgpProviderState = CryptoProviderState.UNINITIALIZED;
        openPgpServiceConnection = new OpenPgpServiceConnection(context, openPgpProvider, new OnBound() {
            @Override
            public void onBound(IOpenPgpService2 service) {openPgpProviderBindOrCheckPermission();
            }

            @Override
            public void onError(Exception e) {
                onOpenPgpProviderError(e);
            }
        });
        openPgpProviderBindOrCheckPermission();

        recipientMvpView.setOpenPgpProvider(openPgpProvider);
    }

    private void openPgpProviderBindOrCheckPermission() {
        if (openPgpServiceConnection == null) {
            openPgpProviderState = CryptoProviderState.UNCONFIGURED;
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

    private void setSmimeProvider(String smimeProvider) {

        boolean providerIsBound = smimeServiceConnection != null && smimeServiceConnection.isBound();
        boolean isSameProvider = smimeProvider != null && smimeProvider.equals(this.smimeProvider);
        if (isSameProvider && providerIsBound) {
            smimeProviderBindOrCheckPermission();
            return;
        }

        if (providerIsBound) {
            smimeServiceConnection.unbindFromService();
            smimeServiceConnection = null;
        }

        this.smimeProvider = smimeProvider;

        if (smimeProvider == null) {
            smimeProviderState = CryptoProviderState.UNCONFIGURED;
            return;
        }

        smimeProviderState = CryptoProviderState.UNINITIALIZED;
        smimeServiceConnection = new SMimeServiceConnection(context, smimeProvider,
                new org.openintents.smime.util.SMimeServiceConnection.OnBound() {
            @Override
            public void onBound(ISMimeService service) {
                smimeProviderBindOrCheckPermission();
            }

            @Override
            public void onError(Exception e) {
                onSmimeProviderError(e);
            }
        });
        smimeProviderBindOrCheckPermission();

        recipientMvpView.setSmimeProvider(smimeProvider);
    }

    private void smimeProviderBindOrCheckPermission() {
        if (smimeServiceConnection == null) {
            smimeProviderState = CryptoProviderState.UNCONFIGURED;
            return;
        }

        if (!smimeServiceConnection.isBound()) {
            pendingUserInteractionIntent = null;
            smimeServiceConnection.bindToService();
            return;
        }

        if (pendingUserInteractionIntent != null) {
            recipientMvpView
                    .launchUserInteractionPendingIntent(pendingUserInteractionIntent, SMIME_USER_INTERACTION);
            pendingUserInteractionIntent = null;
            return;
        }

        getSMimeApi().checkPermissionPing(this);
    }

    private void onOpenPgpProviderError(Exception e) {
        // TODO handle error case better
        recipientMvpView.showErrorOpenPgpConnection();
        openPgpProviderState = CryptoProviderState.ERROR;
        Log.e(K9.LOG_TAG, "error connecting to OpenPGP provider!", e);
        updateCryptoStatus();
    }

    @Override
    public void onPgpPermissionCheckResult(Intent result) {
        int resultCode = result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
        switch (resultCode) {
            case OpenPgpApi.RESULT_CODE_SUCCESS:
                openPgpProviderState = CryptoProviderState.OK;
                break;

            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                recipientMvpView.showErrorOpenPgpUserInteractionRequired();
                pendingUserInteractionIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                openPgpProviderState = CryptoProviderState.ERROR;
                break;

            case OpenPgpApi.RESULT_CODE_ERROR:
            default:
                recipientMvpView.showErrorOpenPgpConnection();
                openPgpProviderState = CryptoProviderState.ERROR;
                break;
        }
        updateCryptoStatus();
    }

    private void onSmimeProviderError(Exception e) {
        // TODO handle error case better
        recipientMvpView.showErrorSmimeConnection();
        smimeProviderState = CryptoProviderState.ERROR;
        Log.e(K9.LOG_TAG, "error connecting to S/MIME provider!", e);
        updateCryptoStatus();
    }

    @Override
    public void onSmimePermissionCheckResult(Intent result) {
        int resultCode = result.getIntExtra(SMimeApi.RESULT_CODE, SMimeApi.RESULT_CODE_ERROR);
        switch (resultCode) {
            case SMimeApi.RESULT_CODE_SUCCESS:
                smimeProviderState = CryptoProviderState.OK;
                break;

            case SMimeApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                recipientMvpView.showErrorSmimeUserInteractionRequired();
                pendingUserInteractionIntent = result.getParcelableExtra(SMimeApi.RESULT_INTENT);
                smimeProviderState = CryptoProviderState.ERROR;
                break;

            case SMimeApi.RESULT_CODE_ERROR:
            default:
                recipientMvpView.showErrorSmimeConnection();
                smimeProviderState = CryptoProviderState.ERROR;
                break;
        }
        updateCryptoStatus();
    }

    public void onActivityDestroy() {
        if (openPgpServiceConnection != null && openPgpServiceConnection.isBound()) {
            openPgpServiceConnection.unbindFromService();
        }
        openPgpServiceConnection = null;
        if (smimeServiceConnection != null && smimeServiceConnection.isBound()) {
            smimeServiceConnection.unbindFromService();
        }
        smimeServiceConnection = null;
    }

    public OpenPgpApi getOpenPgpApi() {
        if (openPgpServiceConnection == null || !openPgpServiceConnection.isBound()) {
            Log.e(K9.LOG_TAG, "obtained OpenPGP API object, but service is not bound! inconsistent state?");
        }
        return new OpenPgpApi(context, openPgpServiceConnection.getService());
    }

    public SMimeApi getSMimeApi() {
        if (smimeServiceConnection == null || !smimeServiceConnection.isBound()) {
            Log.e(K9.LOG_TAG, "obtained S/MIME API object, but service is not bound! inconsistent state?");
        }
        return new SMimeApi(context, smimeServiceConnection.getService());
    }

    public enum CryptoProviderState {
        UNCONFIGURED,
        UNINITIALIZED,
        LOST_CONNECTION,
        ERROR,
        OK
    }

    public enum CryptoMode {
        DISABLE,
        SIGN_ONLY,
        OPPORTUNISTIC,
        PRIVATE,
    }
}
