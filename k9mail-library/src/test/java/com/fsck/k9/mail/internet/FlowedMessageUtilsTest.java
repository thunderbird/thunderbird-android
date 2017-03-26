package com.fsck.k9.mail.internet;


import org.junit.Test;

import static com.fsck.k9.mail.internet.FlowedMessageUtils.isDelSp;
import static com.fsck.k9.mail.internet.FlowedMessageUtils.isFormatFlowed;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class FlowedMessageUtilsTest {
    @Test
    public void isFormatFlowed_withTextPlainFormatFlowed_shouldReturnTrue() throws Exception {
        assertTrue(isFormatFlowed("text/plain; format=flowed"));
    }

    @Test
    public void isFormatFlowed_withTextPlain_shouldReturnFalse() throws Exception {
        assertFalse(isFormatFlowed("text/plain"));
    }

    @Test
    public void isFormatFlowed_withTextHtmlFormatFlowed_shouldReturnFalse() throws Exception {
        assertFalse(isFormatFlowed("text/html; format=flowed"));
    }

    @Test
    public void isDelSp_withFormatFlowed_shouldReturnTrue() throws Exception {
        assertTrue(isDelSp("text/plain; format=flowed; delsp=yes"));
    }

    @Test
    public void isDelSp_withTextPlainFormatFlowed_shoulReturnFalse() throws Exception {
        assertFalse(isDelSp("text/plain; format=flowed"));
    }

    @Test
    public void isDelSp_withoutFormatFlowed_shouldReturnFalse() throws Exception {
        assertFalse(isDelSp("text/plain; delsp=yes"));
    }

    @Test
    public void idDelSp_withTextHtmlFormatFlowed_shouldReturnFalse() throws Exception {
        assertFalse(isDelSp("text/html; format=flowed; delsp=yes"));
    }
}
