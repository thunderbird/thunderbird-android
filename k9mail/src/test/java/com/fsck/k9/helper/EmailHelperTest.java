package com.fsck.k9.helper;


import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EmailHelperTest {

    @Test
    public void getDomainFromEmailAddress_withRegularEmail_shouldReturnsDomain() {
        String result = EmailHelper.getDomainFromEmailAddress("user@domain.com");

        assertEquals("domain.com", result);
    }

    @Test
    public void getDomainFromEmailAddress_withInvalidEmail_shouldReturnNull() {
        String result = EmailHelper.getDomainFromEmailAddress("user");

        assertNull(result);
    }

    @Test
    public void getDomainFromEmailAddress_withTLD_shouldReturnDomain() {
        String result = EmailHelper.getDomainFromEmailAddress("user@domain");

        assertEquals("domain", result);
    }
}
