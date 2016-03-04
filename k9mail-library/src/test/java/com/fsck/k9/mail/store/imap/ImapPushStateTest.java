package com.fsck.k9.mail.store.imap;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 21)
public class ImapPushStateTest {
    @Test
    public void parse_withValidArgument() throws Exception {
        ImapPushState result = ImapPushState.parse("uidNext=42");

        assertNotNull(result);
        assertEquals(42L, result.uidNext);
    }

    @Test
    public void parse_withNullArgument_shouldReturnUidNextOfMinusOne() throws Exception {
        ImapPushState result = ImapPushState.parse(null);

        assertNotNull(result);
        assertEquals(-1L, result.uidNext);
    }

    @Test
    public void parse_withEmptyArgument_shouldReturnUidNextOfMinusOne() throws Exception {
        ImapPushState result = ImapPushState.parse("");

        assertNotNull(result);
        assertEquals(-1L, result.uidNext);
    }

    @Test
    public void parse_withInvalidArgument_shouldReturnUidNextOfMinusOne() throws Exception {
        ImapPushState result = ImapPushState.parse("xyz");

        assertNotNull(result);
        assertEquals(-1L, result.uidNext);
    }

    @Test
    public void parse_withIncompleteArgument_shouldReturnUidNextOfMinusOne() throws Exception {
        ImapPushState result = ImapPushState.parse("uidNext=");

        assertNotNull(result);
        assertEquals(-1L, result.uidNext);
    }

    @Test
    public void parse_withoutIntegerAsUidNext_shouldReturnUidNextOfMinusOne() throws Exception {
        ImapPushState result = ImapPushState.parse("uidNext=xyz");

        assertNotNull(result);
        assertEquals(-1L, result.uidNext);
    }

    @Test
    public void toString_shouldReturnExpectedResult() throws Exception {
        ImapPushState imapPushState = new ImapPushState(23L);

        String result = imapPushState.toString();

        assertEquals("uidNext=23", result);
    }
}
