package com.fsck.k9.activity.compose;


import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.support.v4.app.LoaderManager;

import com.fsck.k9.Account;
import com.fsck.k9.DI;
import com.fsck.k9.K9RobolectricTest;
import com.fsck.k9.activity.compose.RecipientMvpView.CryptoSpecialModeDisplayType;
import com.fsck.k9.activity.compose.RecipientMvpView.CryptoStatusDisplayType;
import com.fsck.k9.activity.compose.RecipientPresenter.CryptoMode;
import com.fsck.k9.autocrypt.AutocryptDraftStateHeaderParser;
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
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openintents.openpgp.OpenPgpApiManager;
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpApiManagerCallback;
import org.openintents.openpgp.OpenPgpApiManager.OpenPgpProviderState;
import org.openintents.openpgp.util.OpenPgpApi;
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
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@SuppressWarnings("ConstantConditions")
@Config(shadows = { ShadowOpenPgpAsyncTask.class })
public class RecipientPresenterTest extends K9RobolectricTest {
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
    private OpenPgpApiManager openPgpApiManager;
    private OpenPgpApiManagerCallback openPgpApiManagerCallback;


    @Before
    public void setUp() throws Exception {
        Context context = ShadowApplication.getInstance().getApplicationContext();
        Robolectric.getBackgroundThreadScheduler().pause();

        recipientMvpView = mock(RecipientMvpView.class);
        openPgpApiManager = mock(OpenPgpApiManager.class);
        account = mock(Account.class);
        composePgpInlineDecider = mock(ComposePgpInlineDecider.class);
        composePgpEnableByDefaultDecider = mock(ComposePgpEnableByDefaultDecider.class);
        autocryptStatusInteractor = mock(AutocryptStatusInteractor.class);
        replyToParser = mock(ReplyToParser.class);
        LoaderManager loaderManager = mock(LoaderManager.class);
        listener = mock(RecipientPresenter.RecipientsChangedListener.class);

        when(openPgpApiManager.getOpenPgpProviderState()).thenReturn(OpenPgpProviderState.UNCONFIGURED);

        recipientPresenter = new RecipientPresenter(
                context, loaderManager, openPgpApiManager, recipientMvpView, account, composePgpInlineDecider,
                composePgpEnableByDefaultDecider, autocryptStatusInteractor, replyToParser, listener,
                DI.get(AutocryptDraftStateHeaderParser.class)
        );

        ArgumentCaptor<OpenPgpApiManagerCallback> callbackCaptor = ArgumentCaptor.forClass(OpenPgpApiManagerCallback.class);
        verify(openPgpApiManager).setOpenPgpProvider(isNull(String.class), callbackCaptor.capture());
        openPgpApiManagerCallback = callbackCaptor.getValue();

        noRecipientsAutocryptResult = new RecipientAutocryptStatus(RecipientAutocryptStatusType.NO_RECIPIENTS, null);
    }

    @Test
    @Ignore("It looks like the support version of AsyncTaskLoader handles background tasks differently")
    public void testInitFromReplyToMessage() throws Exception {
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);

        recipientPresenter.initFromReplyToMessage(message, false);
        runBackgroundTask();

        Recipient toRecipient = new Recipient(TO_ADDRESSES.to[0]);
        verify(recipientMvpView).addRecipients(eq(RecipientType.TO), eq(toRecipient));
    }

    @Test
    @Ignore("It looks like the support version of AsyncTaskLoader handles background tasks differently")
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
        when(openPgpApiManager.getOpenPgpProviderState()).thenReturn(OpenPgpProviderState.UNCONFIGURED);
        recipientPresenter.asyncUpdateCryptoStatus();

        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.UNCONFIGURED, status.getDisplayType());
        assertEquals(CryptoSpecialModeDisplayType.NONE, status.getSpecialModeDisplayType());
        assertNull(status.getAttachErrorStateOrNull());
        assertFalse(status.isProviderStateOk());
        assertFalse(status.isOpenPgpConfigured());
    }

    @Test
    public void getCurrentCryptoStatus_withCryptoProvider() throws Exception {
        setupCryptoProvider(noRecipientsAutocryptResult);

        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.UNAVAILABLE, status.getDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.isOpenPgpConfigured());
    }

    @Test
    public void getCurrentCryptoStatus_withOpportunistic() throws Exception {
        RecipientAutocryptStatus recipientAutocryptStatus = new RecipientAutocryptStatus(
                RecipientAutocryptStatusType.AVAILABLE_UNCONFIRMED, null);
        setupCryptoProvider(recipientAutocryptStatus);

        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.AVAILABLE, status.getDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.isOpenPgpConfigured());
    }

    @Test
    public void getCurrentCryptoStatus_withOpportunistic__confirmed() throws Exception {
        RecipientAutocryptStatus recipientAutocryptStatus = new RecipientAutocryptStatus(
                RecipientAutocryptStatusType.AVAILABLE_CONFIRMED, null);
        setupCryptoProvider(recipientAutocryptStatus);

        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.AVAILABLE, status.getDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.isOpenPgpConfigured());
    }

    @Test
    public void getCurrentCryptoStatus_withOpportunistic__missingKeys() throws Exception {
        RecipientAutocryptStatus recipientAutocryptStatus = new RecipientAutocryptStatus(
                RecipientAutocryptStatusType.UNAVAILABLE, null);
        setupCryptoProvider(recipientAutocryptStatus);

        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.UNAVAILABLE, status.getDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.isOpenPgpConfigured());
    }

    @Test
    public void getCurrentCryptoStatus_withOpportunistic__privateMissingKeys() throws Exception {
        RecipientAutocryptStatus recipientAutocryptStatus = new RecipientAutocryptStatus(
                RecipientAutocryptStatusType.UNAVAILABLE, null);
        setupCryptoProvider(recipientAutocryptStatus);

        recipientPresenter.onCryptoModeChanged(CryptoMode.CHOICE_ENABLED);
        runBackgroundTask();
        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.ENABLED_ERROR, status.getDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.isOpenPgpConfigured());
    }

    @Test
    public void getCurrentCryptoStatus_withModeDisabled() throws Exception {
        RecipientAutocryptStatus recipientAutocryptStatus = new RecipientAutocryptStatus(
                RecipientAutocryptStatusType.AVAILABLE_UNCONFIRMED, null);
        setupCryptoProvider(recipientAutocryptStatus);

        recipientPresenter.onCryptoModeChanged(CryptoMode.CHOICE_DISABLED);
        runBackgroundTask();
        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.AVAILABLE, status.getDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.isOpenPgpConfigured());
    }

    @Test
    public void getCurrentCryptoStatus_withModePrivate() throws Exception {
        RecipientAutocryptStatus recipientAutocryptStatus = new RecipientAutocryptStatus(
                RecipientAutocryptStatusType.AVAILABLE_UNCONFIRMED, null);
        setupCryptoProvider(recipientAutocryptStatus);

        recipientPresenter.onCryptoModeChanged(CryptoMode.CHOICE_ENABLED);
        runBackgroundTask();
        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.ENABLED, status.getDisplayType());
        assertTrue(status.isProviderStateOk());
        assertTrue(status.isOpenPgpConfigured());
    }

    @Test
    public void getCurrentCryptoStatus_withModeSignOnly() throws Exception {
        setupCryptoProvider(noRecipientsAutocryptResult);

        recipientPresenter.onMenuSetSignOnly(true);
        runBackgroundTask();
        ComposeCryptoStatus status = recipientPresenter.getCurrentCachedCryptoStatus();

        assertEquals(CryptoStatusDisplayType.SIGN_ONLY, status.getDisplayType());
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

        assertEquals(CryptoStatusDisplayType.UNAVAILABLE, status.getDisplayType());
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

    private void setupCryptoProvider(RecipientAutocryptStatus autocryptStatusResult) throws Exception {
        Account account = mock(Account.class);
        OpenPgpApi openPgpApi = mock(OpenPgpApi.class);

        when(account.getOpenPgpProvider()).thenReturn(CRYPTO_PROVIDER);
        when(account.isOpenPgpProviderConfigured()).thenReturn(true);
        when(account.getOpenPgpKey()).thenReturn(CRYPTO_KEY_ID);
        recipientPresenter.onSwitchAccount(account);

        when(openPgpApiManager.getOpenPgpProviderState()).thenReturn(OpenPgpProviderState.OK);
        when(openPgpApiManager.getOpenPgpApi()).thenReturn(openPgpApi);
        when(autocryptStatusInteractor.retrieveCryptoProviderRecipientStatus(
                any(OpenPgpApi.class), any(String[].class))).thenReturn(autocryptStatusResult);

        openPgpApiManagerCallback.onOpenPgpProviderStatusChanged();
        runBackgroundTask();
    }
}
