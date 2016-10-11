package com.fsck.k9.activity.compose;


import java.util.Arrays;
import java.util.List;

import android.app.LoaderManager;
import android.content.Context;

import com.fsck.k9.Account;
import com.fsck.k9.helper.ReplyToParser;
import com.fsck.k9.helper.ReplyToParser.ReplyToAddresses;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Message;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.message.ComposePgpInlineDecider;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class RecipientPresenterTest {
    public static final ReplyToAddresses TO_ADDRESSES = new ReplyToAddresses(Address.parse("to@example.org"));
    public static final List<Address> ALL_TO_ADDRESSES = Arrays.asList(Address.parse("allTo@example.org"));
    public static final List<Address> ALL_CC_ADDRESSES = Arrays.asList(Address.parse("allCc@example.org"));


    RecipientPresenter recipientPresenter;
    private ReplyToParser replyToParser;
    private ComposePgpInlineDecider composePgpInlineDecider;
    private Account account;
    private RecipientMvpView recipientMvpView;
    private LoaderManager loaderManager;


    @Before
    public void setUp() throws Exception {
        Context context = ShadowApplication.getInstance().getApplicationContext();

        recipientMvpView = mock(RecipientMvpView.class);
        account = mock(Account.class);
        composePgpInlineDecider = mock(ComposePgpInlineDecider.class);
        replyToParser = mock(ReplyToParser.class);
        loaderManager = mock(LoaderManager.class);

        recipientPresenter = new RecipientPresenter(
                context, loaderManager, recipientMvpView, account, composePgpInlineDecider, replyToParser);
        recipientPresenter.updateCryptoStatus();
    }

    @Test
    public void testInitFromReplyToMessage() throws Exception {
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);

        recipientPresenter.initFromReplyToMessage(message, false);

        verify(recipientMvpView).addRecipients(eq(RecipientType.TO), any(Recipient[].class));
    }

    @Test
    public void testInitFromReplyToAllMessage() throws Exception {
        Message message = mock(Message.class);
        when(replyToParser.getRecipientsToReplyTo(message, account)).thenReturn(TO_ADDRESSES);
        ReplyToAddresses replyToAddresses = new ReplyToAddresses(ALL_TO_ADDRESSES, ALL_CC_ADDRESSES);
        when(replyToParser.getRecipientsToReplyAllTo(message, account)).thenReturn(replyToAddresses);

        recipientPresenter.initFromReplyToMessage(message, true);

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
}
