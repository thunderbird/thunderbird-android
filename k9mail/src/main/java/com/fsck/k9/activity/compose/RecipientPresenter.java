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
import com.fsck.k9.activity.MessageCompose;
import com.fsck.k9.activity.compose.ComposeCryptoStatus.ComposeCryptoStatusBuilder;
import com.fsck.k9.helper.Contacts;
import com.fsck.k9.helper.MailTo;
import com.fsck.k9.helper.Utility;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mailstore.LocalMessage;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpApi.PermissionPingCallback;


public class RecipientPresenter implements PermissionPingCallback {
    private static final String STATE_KEY_CC_SHOWN = "state:ccShown";
    private static final String STATE_KEY_BCC_SHOWN = "state:bccShown";

    private static final int CONTACT_PICKER_TO = 1;
    private static final int CONTACT_PICKER_CC = 2;
    private static final int CONTACT_PICKER_BCC = 3;
    private static final int OPENPGP_USER_INTERACTION = 4;


    private final Context context;
    private final RecipientMvpView recipientMvpView;
    private Account account;
    private String cryptoProvider;
    private RecipientType lastFocusedType = RecipientType.TO;
    private Boolean hasContactPicker;
    private CryptoMode currentCryptoMode = CryptoMode.UNINITIALIZED;
    private ComposeCryptoStatus currentCryptoStatus;
    private PendingIntent pendingUserInteractionIntent;


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
        Address[] replyToAddresses;
        if (message.getReplyTo().length > 0) {
            replyToAddresses = message.getReplyTo();
        } else {
            replyToAddresses = message.getFrom();
        }

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
        updateRecipientExpanderVisibility();
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(STATE_KEY_CC_SHOWN, recipientMvpView.isCcVisible());
        outState.putBoolean(STATE_KEY_BCC_SHOWN, recipientMvpView.isBccVisible());
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

        cryptoProvider = account.getOpenPgpProvider();
        recipientMvpView.setCryptoProvider(cryptoProvider);
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
        ComposeCryptoStatusBuilder builder =  new ComposeCryptoStatusBuilder()
            .setCryptoMode(currentCryptoMode)
            .setRecipients(getAllRecipients());

        long accountCryptoKey = account.getCryptoKey();
        if (accountCryptoKey != Account.NO_OPENPGP_KEY) {
            // TODO split these into individual settings? maybe after key is bound to identity
            builder.setSigningKeyId(accountCryptoKey);
            builder.setSelfEncryptId(accountCryptoKey);
        }

        currentCryptoStatus = builder.build();
        recipientMvpView.showCryptoStatus(currentCryptoStatus.getCryptoStatusDisplayType());
    }

    public ComposeCryptoStatus getCurrentCryptoStatus() {
        return currentCryptoStatus;
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
        new RecipientLoader(context, cryptoProvider, addresses) {
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
        new RecipientLoader(context, cryptoProvider, uri, false) {
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
                recipientMvpView.checkOpenPgpServicePermission(this);
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
        if (currentCryptoMode == CryptoMode.ERROR) {
            if (pendingUserInteractionIntent != null) {
                recipientMvpView
                        .launchUserInteractionPendingIntent(pendingUserInteractionIntent, OPENPGP_USER_INTERACTION);
                pendingUserInteractionIntent = null;
            } else {
                recipientMvpView.checkOpenPgpServicePermission(this);
            }
            return;
        }
        recipientMvpView.showCryptoDialog(currentCryptoMode);
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

    public boolean canSendOrError(boolean isDraft) {
        if (isDraft) {
            return true;
        }

        ComposeCryptoStatus cryptoStatus = getCurrentCryptoStatus();
        if (cryptoStatus.isPgpErrorState()) {
            // TODO: be more specific about this error
            recipientMvpView.showErrorOpenPgpConnection();
            return false;
        }
        if (cryptoStatus.isMissingSignKey()) {
            recipientMvpView.showErrorMissingSignKey();
            return false;
        }
        if (cryptoStatus.isPrivateAndIncomplete()) {
            recipientMvpView.showErrorPrivateButMissingKeys();
            return false;
        }

        return true;
    }

    public void onCryptoProviderBound() {
        recipientMvpView.checkOpenPgpServicePermission(this);
    }

    public void onCryptoProviderError(Exception e) {
        // TODO handle error case better
        recipientMvpView.showErrorOpenPgpConnection();
        currentCryptoMode = CryptoMode.ERROR;
        Log.e(K9.LOG_TAG, "error connecting to crypto provider!", e);
        updateCryptoStatus();
    }

    @Override
    public void onPgpPermissionCheckResult(Intent result) {
        int resultCode = result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
        switch (resultCode) {
            case OpenPgpApi.RESULT_CODE_SUCCESS:
                if (currentCryptoMode == CryptoMode.UNINITIALIZED || currentCryptoMode == CryptoMode.ERROR) {
                    // TODO initialize to other values under some circumstances, e.g. if we reply to an encrypted e-mail
                    currentCryptoMode = CryptoMode.OPPORTUNISTIC;
                }
                break;

            case OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED:
                recipientMvpView.showErrorOpenPgpUserInteractionRequired();
                pendingUserInteractionIntent = result.getParcelableExtra(OpenPgpApi.RESULT_INTENT);
                currentCryptoMode = CryptoMode.ERROR;
                break;

            case OpenPgpApi.RESULT_CODE_ERROR:
            default:
                recipientMvpView.showErrorOpenPgpConnection();
                currentCryptoMode = CryptoMode.ERROR;
                break;
        }
        updateCryptoStatus();
    }

    public enum CryptoMode {
        UNINITIALIZED,
        DISABLE,
        SIGN_ONLY,
        OPPORTUNISTIC,
        PRIVATE,
        ERROR,
    }
}
