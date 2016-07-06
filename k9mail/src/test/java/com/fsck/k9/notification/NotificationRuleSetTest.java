package com.fsck.k9.notification;

import com.fsck.k9.Account;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import static org.mockito.Mockito.mock;

import com.fsck.k9.activity.MessageReference;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class NotificationRuleSetTest {

    private static final String MESSAGE_SENDER_NAME = "sender";
    private static final String MESSAGE_SENDER_ADDRESS = "sender@test.com";
    private static final String MESSAGE_SUBJECT = "message subject";
    private static final String MESSAGE_BODY = "message body";
    private static final String MESSAGE_PREVIEW = MESSAGE_SUBJECT + "\n" + MESSAGE_BODY;
    private static final String MESSAGE_SUMMARY = MESSAGE_SUBJECT + " " + MESSAGE_BODY;

    private static final String MESSAGE_SENDER_NAME_NO_MATCH = "senderNoMatch";
    private static final String MESSAGE_SENDER_ADDRESS_NO_MATCH = "senderNoMatch@test.com";
    private static final String MESSAGE_SUBJECT_NO_MATCH = "message subject no match";
    private static final String MESSAGE_BODY_NO_MATCH = "message body no match";
    private static final String MESSAGE_PREVIEW_NO_MATCH = MESSAGE_SUBJECT_NO_MATCH + "\n" + MESSAGE_BODY_NO_MATCH;


    private NotificationContent notificationContent;

    @Before
    public void setUp() throws Exception {
        notificationContent = createNotificationContent();
    }

    @Test
    public void testCreateNotificationRuleSet() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        assertEquals(notificationRuleSet.getName(), "");
        assertEquals(notificationRuleSet.getSenderName(), "");
        assertEquals(notificationRuleSet.getSenderAddress(), "");
        assertEquals(notificationRuleSet.getSubject(), "");
        assertEquals(notificationRuleSet.getBody(), "");
    }

    @Test
    public void testMatchOnEmptyRule() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        
        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testMatchOnSenderName() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME);
        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderName() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME_NO_MATCH);
        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testMatchOnSenderAddress() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS);
        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderAddress() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS_NO_MATCH);
        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testMatchOnSubject() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSubject(MESSAGE_SUBJECT);
        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSubject() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSubject(MESSAGE_SUBJECT_NO_MATCH);
        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testMatchOnBody() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setBody(MESSAGE_BODY);
        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnBody() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setBody(MESSAGE_BODY_NO_MATCH);
        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testMatchOnSenderNameAndSenderAddress() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS);
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME);

        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderNameAndSenderAddress_bothWrong() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS_NO_MATCH);
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME_NO_MATCH);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderNameAndSenderAddress_wrongSenderName() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS);
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME_NO_MATCH);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }
    @Test
    public void testNoMatchOnSenderNameAndSenderAddress_wrongAddress() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS_NO_MATCH);
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }
    @Test
    public void testMatchOnSenderNameAndSubject() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME);
        notificationRuleSet.setSubject(MESSAGE_SUBJECT);

        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderNameAndSubject_bothWrong() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME_NO_MATCH);
        notificationRuleSet.setSubject(MESSAGE_SUBJECT_NO_MATCH);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderNameAndSubject_wrongSenderName() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME_NO_MATCH);
        notificationRuleSet.setSubject(MESSAGE_SUBJECT);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderNameAndSubject_wrongSubject() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME);
        notificationRuleSet.setSubject(MESSAGE_SUBJECT_NO_MATCH);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testMatchOnSenderNameAndBody() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME);
        notificationRuleSet.setBody(MESSAGE_BODY);

        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderNameAndBody_bothWrong() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME_NO_MATCH);
        notificationRuleSet.setBody(MESSAGE_BODY_NO_MATCH);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderNameAndBody_wrongSender() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME_NO_MATCH);
        notificationRuleSet.setBody(MESSAGE_BODY);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderNameAndBody_wrongBody() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME);
        notificationRuleSet.setBody(MESSAGE_BODY_NO_MATCH);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testMatchOnSenderAddressAndSubject() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS);
        notificationRuleSet.setSubject(MESSAGE_SUBJECT);

        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderAddressAndSubject_bothWrong() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS_NO_MATCH);
        notificationRuleSet.setSubject(MESSAGE_SUBJECT_NO_MATCH);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderAddressAndSubject_wrongSenderAddress() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS_NO_MATCH);
        notificationRuleSet.setSubject(MESSAGE_SUBJECT);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderAddressAndSubject_wrongSubject() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS);
        notificationRuleSet.setSubject(MESSAGE_SUBJECT_NO_MATCH);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testMatchOnSenderAddressAndBody() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS);
        notificationRuleSet.setBody(MESSAGE_BODY);

        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderAddressAndBody_bothWrong() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS_NO_MATCH);
        notificationRuleSet.setBody(MESSAGE_BODY_NO_MATCH);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderAddressAndBody_wrongSenderAddress() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS_NO_MATCH);
        notificationRuleSet.setBody(MESSAGE_BODY);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSenderAddressAndBody_wrongBody() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS);
        notificationRuleSet.setBody(MESSAGE_BODY_NO_MATCH);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testMatchOnSubjectAndBody() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSubject(MESSAGE_SUBJECT);
        notificationRuleSet.setBody(MESSAGE_BODY);

        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSubjectAndBody_bothWrong() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSubject(MESSAGE_SUBJECT_NO_MATCH);
        notificationRuleSet.setBody(MESSAGE_BODY_NO_MATCH);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSubjectAndBody_wrongSubject() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSubject(MESSAGE_SUBJECT_NO_MATCH);
        notificationRuleSet.setBody(MESSAGE_BODY);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testNoMatchOnSubjectAndBody_wrongBody() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSubject(MESSAGE_SUBJECT);
        notificationRuleSet.setBody(MESSAGE_BODY_NO_MATCH);

        assertFalse(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testMatchOnSenderNameAndSenderAddressAndSubject() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME);
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS);
        notificationRuleSet.setSubject(MESSAGE_SUBJECT);

        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testMatchOnSenderNameAndSenderAddressAndBody() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME);
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS);
        notificationRuleSet.setBody(MESSAGE_BODY);

        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    public void testMatchOnSenderAddressAndSubjectAndBody() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS);
        notificationRuleSet.setSubject(MESSAGE_SUBJECT);
        notificationRuleSet.setBody(MESSAGE_BODY);

        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    @Test
    public void testMatchOnAll() throws Exception {
        NotificationRuleSet notificationRuleSet = new NotificationRuleSet();
        notificationRuleSet.setSenderName(MESSAGE_SENDER_NAME);
        notificationRuleSet.setSenderAddress(MESSAGE_SENDER_ADDRESS);
        notificationRuleSet.setSubject(MESSAGE_SUBJECT);
        notificationRuleSet.setBody(MESSAGE_BODY);

        assertTrue(notificationRuleSet.matches(MESSAGE_SENDER_ADDRESS, notificationContent));
    }

    private NotificationContent createNotificationContent() {
        MessageReference messageReference = mock(MessageReference.class);
        NotificationContent notificationContent = new NotificationContent(messageReference,
                MESSAGE_SENDER_NAME,
                MESSAGE_SUBJECT,
                MESSAGE_PREVIEW,
                MESSAGE_SUMMARY,
                false);

        return notificationContent;
    }

}
