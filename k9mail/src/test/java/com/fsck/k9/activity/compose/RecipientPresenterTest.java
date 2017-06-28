package com.fsck.k9.activity.compose;


import java.util.Arrays;
import java.util.List;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.os.ParcelFileDescriptor;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.K9RobolectricTestRunner;
import com.fsck.k9.activity.compose.RecipientMvpView.CryptoSpecialModeDisplayType;
import com.fsck.k9.activity.compose.RecipientMvpView.CryptoStatusDisplayType;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.helper.ReplyToParser;
import com.fsck.k9.helper.ReplyToParser.ReplyToAddresses;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.message.AutocryptStatusInteractor;
import com.fsck.k9.message.AutocryptStatusInteractor.RecipientAutocryptStatus;
import com.fsck.k9.message.AutocryptStatusInteractor.RecipientAutocryptStatusType;
import com.fsck.k9.message.ComposePgpEnableByDefaultDecider;
import com.fsck.k9.message.ComposePgpInlineDecider;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.IOpenPgpService2;
import org.openintents.openpgp.util.OpenPgpApi;
import org.openintents.openpgp.util.OpenPgpServiceConnection;
import org.openintents.openpgp.util.ShadowOpenPgpAsyncTask;
import org.robolectric.Robolectric;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("ConstantConditions")
@RunWith(K9RobolectricTestRunner.class)
@Config(shadows = {ShadowOpenPgpAsyncTask.class})
public class RecipientPresenterTest {
    private static final ReplyToAddresses TO_ADDRESSES = new ReplyToAddresses(Address.parse("to@example.org"));
    private static final List<Address> ALL_TO_ADDRESSES = Arrays.asList(Address.parse("allTo@example.org"));
    private static final List<Address> ALL_CC_ADDRESSES = Arrays.asList(Address.parse("allCc@example.org"));
    private static final String CRYPTO_PROVIDER = "crypto_provider";
    private static final long CRYPTO_KEY_ID = 123L;


    private RecipientPresenter recipientPresenter;
    private ReplyToParser replyToParser;
    private ComposePgpInlineDecider composePgpInlineDecider;
    private ComposePgpEnableByDefaultDecider composePgpEnableByDefaultDecider;
    private Account account;
    private RecipientMvpView recipientMvpView;
    private RecipientPresenter.RecipientsChangedListener listener;
    private AutocryptStatusInteractor autocryptStatusInteractor;
    private RecipientAutocryptStatus noRecipientsAutocryptResult;


    @Before
    public void setUp() throws Exception {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        Robolectric.getBackgroundThreadScheduler().pause();

        recipientMvpView = mock(RecipientMvpView.class);
        account = mock(Account.class);
        composePgpInlineDecider = mock(ComposePgpInlineDecider.class);
        composePgpEnableByDefaultDecider = mock(ComposePgpEnableByDefaultDecider.class);
        autocryptStatusInteractor = mock(AutocryptStatusInteractor.class);
        replyToParser = mock(ReplyToParser.class);
        LoaderManager loaderManager = mock(LoaderManager.class);
        listener = mock(RecipientPresenter.RecipientsChangedListener.class);

        recipientPresenter = new RecipientPresenter(
                context, loaderManager, recipientMvpView, account, composePgpInlineDecider,
                composePgpEnableByDefaultDecider, autocryptStatusInteractor, replyToParser, listener);
        runBackgroundTask();

        noRecipientsAutocryptResult = new RecipientAutocryptStatus(RecipientAutocryptStatusType.NO_RECIPIENTS, null);
    }

    @Test
    public void testInitFromReplyToMessage() throws Exception {
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);

        recipientPresenter.initFromReplyToMessage(message, false);
        runBackgroundTask();

        verify(recipientMvpView).addRecipients(eq(RecipientType.TO), any(Recipient[].class));
    }

    @Test
    public void testInitFromReplyToAllMessage() throws Exception {
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);
        ReplyToAddresses replyToAddresses = new ReplyToAddresses(ALL_TO_ADDRESSES, ALL_CC_ADDRESSES);
        when(replyToParser.getRecipientsToReplyAllTo(message, account)).thenReturn(replyToAddresses);

        recipientPresenter.initFromReplyToMessage(message, true);
        // one for To, one for Cc
        runBackgroundTask();
        runBackgroundTask();

        verify(recipientMvpView).addRecipients(eq(RecipientType.TO), any(Recipient.class));
        verify(recipientMvpView).addRecipients(eq(RecipientType.CC), any(Recipient.class));
    }

    @Test
    public void initFromReplyToMessage_shouldCallComposePgpInlineDecider() throws Exception {
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);

        recipientPresenter.initFromReplyToMessage(message, false);

        verify(composePgpInlineDecider).shouldReplyInline(message);
    }

    @Test
    public void getCurrentCryptoStatus_withoutCryptoProvider() throws Exception {
        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.UNCONFIGURED, status.getCryptoStatusDisplayType());
        assertEquals(CryptoSpecialModeDisplayType.NONE, status.getCryptoSpecialModeDisplayType());
        assertNull(status.getAttachErrorStateOrNull());
        assertFalse(status.isProviderStateOk());
        assertFalse(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withCryptoProvider() throws Exception {
        setupCryptoProvider(noRecipientsAutocryptResult);

        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.NO_CHOICE_EMPTY, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withOpportunistic() throws Exception {
        RecipientAutocryptStatus recipientAutocryptStatus = new RecipientAutocryptStatus(
                RecipientAutocryptStatusType.AVAILABLE_UNCONFIRMED, null);
        setupCryptoProvider(recipientAutocryptStatus);

        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.NO_CHOICE_AVAILABLE, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withOpportunistic__confirmed() throws Exception {
        RecipientAutocryptStatus recipientAutocryptStatus = new RecipientAutocryptStatus(
                RecipientAutocryptStatusType.AVAILABLE_CONFIRMED, null);
        setupCryptoProvider(recipientAutocryptStatus);

        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.NO_CHOICE_AVAILABLE_TRUSTED, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withOpportunistic__missingKeys() throws Exception {
        RecipientAutocryptStatus recipientAutocryptStatus = new RecipientAutocryptStatus(
                RecipientAutocryptStatusType.UNAVAILABLE, null);
        setupCryptoProvider(recipientAutocryptStatus);

        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.NO_CHOICE_UNAVAILABLE, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withOpportunistic__privateMissingKeys() throws Exception {
        RecipientAutocryptStatus recipientAutocryptStatus = new RecipientAutocryptStatus(
                RecipientAutocryptStatusType.UNAVAILABLE, null);
        setupCryptoProvider(recipientAutocryptStatus);

        recipientPresenter.onCryptoModeChanged(CryptoMode.CHOICE_ENABLED);
        runBackgroundTask();
        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.CHOICE_ENABLED_ERROR, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withModeDisabled() throws Exception {
        RecipientAutocryptStatus recipientAutocryptStatus = new RecipientAutocryptStatus(
                RecipientAutocryptStatusType.AVAILABLE_UNCONFIRMED, null);
        setupCryptoProvider(recipientAutocryptStatus);

        recipientPresenter.onCryptoModeChanged(CryptoMode.CHOICE_DISABLED);
        runBackgroundTask();
        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.CHOICE_DISABLED_UNTRUSTED, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withModePrivate() throws Exception {
        RecipientAutocryptStatus recipientAutocryptStatus = new RecipientAutocryptStatus(
                RecipientAutocryptStatusType.AVAILABLE_UNCONFIRMED, null);
        setupCryptoProvider(recipientAutocryptStatus);

        recipientPresenter.onCryptoModeChanged(CryptoMode.CHOICE_ENABLED);
        runBackgroundTask();
        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.CHOICE_ENABLED_UNTRUSTED, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.shouldUsePgpMessageBuilder());
    }

    @Test
    public void getCurrentCryptoStatus_withModeSignOnly() throws Exception {
        setupCryptoProvider(noRecipientsAutocryptResult);

        recipientPresenter.onMenuSetSignOnly(true);
        runBackgroundTask();
        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.SIGN_ONLY, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.isSigningEnabled());
        assertTrue(status.isSignOnly());
    }

    @Test
    public void getCurrentCryptoStatus_withModeInline() throws Exception {
        setupCryptoProvider(noRecipientsAutocryptResult);

        recipientPresenter.onMenuSetPgpInline(true);
        runBackgroundTask();
        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.NO_CHOICE_EMPTY, status.getCryptoStatusDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.isPgpInlineModeEnabled());
    }

    @Test
    public void onToTokenAdded_notifiesListenerOfRecipientChange() {
        recipientPresenter.onToTokenAdded();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onToTokenChanged_notifiesListenerOfRecipientChange() {
        recipientPresenter.onToTokenChanged();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onToTokenRemoved_notifiesListenerOfRecipientChange() {
        recipientPresenter.onToTokenRemoved();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onCcTokenAdded_notifiesListenerOfRecipientChange() {
        recipientPresenter.onCcTokenAdded();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onCcTokenChanged_notifiesListenerOfRecipientChange() {
        recipientPresenter.onCcTokenChanged();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onCcTokenRemoved_notifiesListenerOfRecipientChange() {
        recipientPresenter.onCcTokenRemoved();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onBccTokenAdded_notifiesListenerOfRecipientChange() {
        recipientPresenter.onBccTokenAdded();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onBccTokenChanged_notifiesListenerOfRecipientChange() {
        recipientPresenter.onBccTokenChanged();
        verify(listener).onRecipientsChanged();
    }

    @Test
    public void onBccTokenRemoved_notifiesListenerOfRecipientChange() {
        recipientPresenter.onBccTokenRemoved();
        verify(listener).onRecipientsChanged();
    }

    private void runBackgroundTask() {
        boolean taskRun = Robolectric.getBackgroundThreadScheduler().runOneTask();
        assertTrue(taskRun);
    }

    private void setupCryptoProvider(RecipientAutocryptStatus autocryptStatusResult) throws android.os.RemoteException {
        Account account = mock(Account.class);
        OpenPgpServiceConnection openPgpServiceConnection = mock(OpenPgpServiceConnection.class);
        IOpenPgpService2 openPgpService2 = mock(IOpenPgpService2.class);
        Intent permissionPingIntent = new Intent();

        when(autocryptStatusInteractor.retrieveCryptoProviderRecipientStatus(
                any(OpenPgpApi.class), any(String[].class))).thenReturn(autocryptStatusResult);

        K9.setOpenPgpProvider(CRYPTO_PROVIDER);
        permissionPingIntent.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_SUCCESS);
        when(account.getCryptoKey()).thenReturn(CRYPTO_KEY_ID);
        when(openPgpServiceConnection.isBound()).thenReturn(true);
        when(openPgpServiceConnection.getService()).thenReturn(openPgpService2);
        when(openPgpService2.execute(any(Intent.class), any(ParcelFileDescriptor.class), any(Integer.class)))
                .thenReturn(permissionPingIntent);

        recipientPresenter.setOpenPgpServiceConnection(openPgpServiceConnection, CRYPTO_PROVIDER);
        recipientPresenter.onSwitchAccount(account);
        // one for the permission ping, one for the async status update
        runBackgroundTask();
        runBackgroundTask();
    }
}
