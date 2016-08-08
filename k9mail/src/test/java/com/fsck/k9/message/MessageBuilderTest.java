package com.fsck.k9.message;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import android.app.Application;

import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.Identity;
import com.fsck.k9.activity.misc.Attachment;
import com.fsck.k9.mail.Address;
import com.fsck.k9.mail.Body;
import com.fsck.k9.mail.Message.RecipientType;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.UUIDGenerator;
import com.fsck.k9.mail.internet.MimeMessage;
import com.fsck.k9.message.MessageBuilder.Callback;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = 21)
public class MessageBuilderTest {
    public static final String TEST_MESSAGE_TEXT = "message text";
    public static final String TEST_SUBJECT = "test_subject";
    public static final Address TEST_IDENTITY_ADDRESS = new Address("test@example.org", "tester");
    public static final Address[] TEST_TO = new Address[] {
            new Address("to1@example.org", "recip 1"), new Address("to2@example.org", "recip 2")
    };
    public static final Address[] TEST_CC = new Address[] { new Address("cc@example.org", "cc recip") };
    public static final Address[] TEST_BCC = new Address[] { new Address("bcc@example.org", "bcc recip") };

    public static final UUID TEST_UUID = new UUID(123L, 234L);


    private Application context;
    private UUIDGenerator uuidGenerator;


    @Before
    public void setUp() throws Exception {
        uuidGenerator = mock(UUIDGenerator.class);
        when(uuidGenerator.generateUUID()).thenReturn(TEST_UUID);

        context =  RuntimeEnvironment.application;
    }

    @Test
    public void build__shouldSucceed() throws Exception {
        MessageBuilder messageBuilder = createSimpleMessageBuilder();


        Callback mockCallback = mock(Callback.class);
        messageBuilder.buildAsync(mockCallback);


        ArgumentCaptor<MimeMessage> mimeMessageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mockCallback).onMessageBuildSuccess(mimeMessageCaptor.capture(), eq(false));
        verifyNoMoreInteractions(mockCallback);

        MimeMessage mimeMessage = mimeMessageCaptor.getValue();
        assertContentOfBodyEquals("message content must match", mimeMessage.getBody(), TEST_MESSAGE_TEXT);
        assertEquals("text/plain", mimeMessage.getMimeType());
        assertEquals(TEST_SUBJECT, mimeMessage.getSubject());
        assertEquals(TEST_IDENTITY_ADDRESS, mimeMessage.getFrom()[0]);
        assertArrayEquals(TEST_TO, mimeMessage.getRecipients(RecipientType.TO));
        assertArrayEquals(TEST_CC, mimeMessage.getRecipients(RecipientType.CC));
        assertArrayEquals(TEST_BCC, mimeMessage.getRecipients(RecipientType.BCC));
    }

    @Test
    public void build__detachAndReattach__shouldSucceed() throws MessagingException {
        MessageBuilder messageBuilder = createSimpleMessageBuilder();


        Callback mockCallback = mock(Callback.class);
        Robolectric.getBackgroundThreadScheduler().pause();
        messageBuilder.buildAsync(mockCallback);
        messageBuilder.detachCallback();
        Robolectric.getBackgroundThreadScheduler().unPause();

        verifyNoMoreInteractions(mockCallback);


        mockCallback = mock(Callback.class);
        messageBuilder.reattachCallback(mockCallback);

        verify(mockCallback).onMessageBuildSuccess(any(MimeMessage.class), eq(false));
        verifyNoMoreInteractions(mockCallback);
    }

    @Test
    public void buildWithException__shouldThrow() throws MessagingException {
        MessageBuilder messageBuilder = new SimpleMessageBuilder(context, uuidGenerator) {
            @Override
            protected void buildMessageInternal() {
                queueMessageBuildException(new MessagingException("expected error"));
            }
        };

        Callback mockCallback = mock(Callback.class);
        messageBuilder.buildAsync(mockCallback);

        verify(mockCallback).onMessageBuildException(any(MessagingException.class));
        verifyNoMoreInteractions(mockCallback);
    }

    @Test
    public void buildWithException__detachAndReattach__shouldThrow() throws MessagingException {
        MessageBuilder messageBuilder = new SimpleMessageBuilder(context, uuidGenerator) {
            @Override
            protected void buildMessageInternal() {
                queueMessageBuildException(new MessagingException("expected error"));
            }
        };


        Callback mockCallback = mock(Callback.class);
        Robolectric.getBackgroundThreadScheduler().pause();
        messageBuilder.buildAsync(mockCallback);
        messageBuilder.detachCallback();
        Robolectric.getBackgroundThreadScheduler().unPause();

        verifyNoMoreInteractions(mockCallback);


        mockCallback = mock(Callback.class);
        messageBuilder.reattachCallback(mockCallback);

        verify(mockCallback).onMessageBuildException(any(MessagingException.class));
        verifyNoMoreInteractions(mockCallback);
    }

    private MessageBuilder createSimpleMessageBuilder() {
        MessageBuilder b = new SimpleMessageBuilder(context, uuidGenerator);

        Identity identity = new Identity();
        identity.setName(TEST_IDENTITY_ADDRESS.getPersonal());
        identity.setEmail(TEST_IDENTITY_ADDRESS.getAddress());
        identity.setDescription("test identity");
        identity.setSignatureUse(false);

        b.setSubject(TEST_SUBJECT)
                .setTo(Arrays.asList(TEST_TO))
                .setCc(Arrays.asList(TEST_CC))
                .setBcc(Arrays.asList(TEST_BCC))
                .setInReplyTo("inreplyto")
                .setReferences("references")
                .setRequestReadReceipt(false)
                .setIdentity(identity)
                .setMessageFormat(SimpleMessageFormat.TEXT)
                .setText(TEST_MESSAGE_TEXT)
                .setAttachments(new ArrayList<Attachment>())
                .setSignature("signature")
                .setQuoteStyle(QuoteStyle.PREFIX)
                .setQuotedTextMode(QuotedTextMode.NONE)
                .setQuotedText("quoted text")
                .setQuotedHtmlContent(new InsertableHtmlContent())
                .setReplyAfterQuote(false)
                .setSignatureBeforeQuotedText(false)
                .setIdentityChanged(false)
                .setSignatureChanged(false)
                .setCursorPosition(0)
                .setMessageReference(null)
                .setDraft(false);

        return b;
    }

    private static void assertContentOfBodyEquals(String reason, Body bodyPart, String expected) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            bodyPart.writeTo(bos) ;
            Assert.assertEquals(reason, expected, new String(bos.toByteArray()));
        } catch (IOException | MessagingException e) {
            fail();
        }
    }

}
