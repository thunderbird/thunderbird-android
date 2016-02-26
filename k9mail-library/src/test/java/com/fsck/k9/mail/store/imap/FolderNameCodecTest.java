package com.fsck.k9.mail.store.imap;


import java.nio.charset.CharacterCodingException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class FolderNameCodecTest {
    private FolderNameCodec folderNameCode;


    @Before
    public void setUp() throws Exception {
        folderNameCode = FolderNameCodec.newInstance();
    }

    @Test
    public void encode_withAsciiArgument_shouldReturnInput() throws Exception {
        String folderName = "ASCII";

        String result = folderNameCode.encode(folderName);

        assertEquals(folderName, result);
    }

    @Test
    public void encode_withNonAsciiArgument_shouldReturnEncodedString() throws Exception {
        String folderName = "über";

        String result = folderNameCode.encode(folderName);

        assertEquals("&APw-ber", result);
    }

    @Test
    public void decode_withEncodedArgument_shouldReturnDecodedString() throws Exception {
        String encodedFolderName = "&ANw-bergr&APYA3w-entr&AOQ-ger";

        String result = folderNameCode.decode(encodedFolderName);

        assertEquals("Übergrößenträger", result);
    }

    @Test
    public void decode_withInvalidEncodedArgument_shouldThrow() throws Exception {
        String encodedFolderName = "&12-foo";

        try {
            folderNameCode.decode(encodedFolderName);
            fail("Expected exception");
        } catch (CharacterCodingException ignored) {
        }
    }
}
