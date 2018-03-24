package com.fsck.k9.crypto;


import com.fsck.k9.Identity;
import org.junit.Test;

import static org.junit.Assert.assertEquals;


public class OpenPgpApiHelperTest {

    @Test
    public void buildUserId_withName_shouldCreateOpenPgpAccountName() {
        Identity identity = new Identity();
        identity.setEmail("user@domain.com");
        identity.setName("Name");

        String result = OpenPgpApiHelper.buildUserId(identity);

        assertEquals("Name <user@domain.com>", result);
    }

    @Test
    public void buildUserId_withoutName_shouldCreateOpenPgpAccountName() {
        Identity identity = new Identity();
        identity.setEmail("user@domain.com");

        String result = OpenPgpApiHelper.buildUserId(identity);

        assertEquals("<user@domain.com>", result);
    }

}
