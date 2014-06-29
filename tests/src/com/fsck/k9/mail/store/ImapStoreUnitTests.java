package com.fsck.k9.mail.store;

import android.content.Context;
import android.test.AndroidTestCase;
import com.fsck.k9.Account;
import com.fsck.k9.mail.Folder;
import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.filter.Base64;
import com.fsck.k9.mail.transport.MockTransport;

import java.io.UnsupportedEncodingException;

public class ImapStoreUnitTests extends AndroidTestCase {
    class FakeAccount extends Account {
        public FakeAccount(Context context) {
            super(context);
        }

        // Avoid failing with the K9.app is often null.
        @Override
        protected String getDefaultProviderId() {
            // return StorageManager.getInstance(K9.app).getDefaultProviderId()
            return null;
        }
    }

    /**
     * Folder name encoded in UTF-7.
     */
    private final static String FOLDER_ENCODED = "&ZeU-";

    /* These values are provided by setUp() */
    private ImapStore mStore = null;
    private ImapStore.ImapFolder mFolder = null;

    /**
     * The tag for the current IMAP command; used for mock transport responses
     */
    private int mTag;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        mContext = getContext();

        Account testAccount = new FakeAccount(mContext);
        testAccount.setStoreUri("imap://PLAIN:user:password@server:999");

        mStore = new ImapStore(testAccount);
        mFolder = (ImapStore.ImapFolder) mStore.getFolder("INBOX");
        resetTag();
    }

    /**
     * Confirms simple non-SSL non-TLS login
     */
    public void testSimpleLogin() throws MessagingException, UnsupportedEncodingException {

        MockTransport mockTransport = openAndInjectMockTransport();

        // inject boilerplate commands that match our typical login
        mockTransport.expect(null, "* oK Imap 2000 Ready To Assist You");
        mockTransport.expect(getNextTag(false) + " CAPABILITY",
                new String[]{
                        "* cAPABILITY iMAP4rev1 sTARTTLS aUTH=pLAIN lOGINDISABLED",
                        getNextTag(true) + " oK CAPABILITY completed"
                });
        mockTransport.expect(getNextTag(false) + " AUTHENTICATE PLAIN",
                new String[] {
                        "+"
                });
        mockTransport.expectLiterally(
                new String(Base64.encodeBase64(("\000" + "user" + "\000" + "password").getBytes()), "ISO-8859-1"),
                new String[] {
                        getNextTag(true) + " oK user authenticated (Success)"
                });
        mockTransport.expect(getNextTag(false) + " LIST \"\" \"\"",
                new String[] {
                        "* lIST (\\HAsNoChildren) \"/\" \"inbox\"",
                        "* lIST (\\hAsnochildren) \"/\" \"Drafts\"",
                        "* lIST (\\nOselect) \"/\" \"no select\"",
                        "* lIST (\\HAsNoChildren) \"/\" \"&ZeVnLIqe-\"", // Japanese folder name
                        getNextTag(true) + " oK SUCCESS"
                });
        mockTransport.expect(getNextTag(false) + " EXAMINE \"" + "INBOX" + "\"",
                new String[] {
                        "* fLAGS (\\Answered \\Flagged \\Draft \\Deleted \\Seen)",
                        "* oK [pERMANENTFLAGS (\\Answered \\Flagged \\Draft \\Deleted \\Seen \\*)]",
                        "* 0 eXISTS",
                        "* 0 rECENT",
                        "* OK [uNSEEN 0]",
                        "* OK [uIDNEXT 1]",
                        getNextTag(true) + " oK [" + "rEAD-wRITE" + "] " +
                                "INBOX" + " selected. (Success)"
                });
        // try to open it
        mFolder.open(Folder.OPEN_MODE_RO);
    }

    /**
     * Set up a basic MockTransport. open it, and inject it into mStore
     */
    private MockTransport openAndInjectMockTransport() {
        // Create mock transport and inject it into the POP3Store that's already set up
        MockTransport mockTransport = new MockTransport();
        mStore.setTransport(mockTransport);
        return mockTransport;
    }

    /**
     * Return a tag for use in setting up expect strings.  Typically this is called in pairs,
     * first as getNextTag(false) when emitting the command, then as getNextTag(true) when
     * emitting the final line of the expected response.
     *
     * @param advance true to increment mNextTag for the subsequence command
     * @return a string containing the current tag
     */
    public String getNextTag(boolean advance) {
        String s = Integer.toString(mTag);
        if (advance) ++mTag;
        return s;
    }

    /**
     * Resets the tag back to it's starting value. Do this after the test connection has been
     * closed.
     */
    private int resetTag() {
        return resetTag(1);
    }

    private int resetTag(int tag) {
        int oldTag = mTag;
        mTag = tag;
        return oldTag;
    }
}
