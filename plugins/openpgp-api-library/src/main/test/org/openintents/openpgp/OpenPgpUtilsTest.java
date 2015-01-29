package test.org.openintents.openpgp;

import org.openintents.openpgp.util.OpenPgpUtils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.util.OpenPgpUtils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class OpenPgpUtilsTest {
    @Test
    public void splitCompleteUserIdShouldReturnAll3Components() throws Exception {
        OpenPgpUtils.UserInfo info = OpenPgpUtils.splitUserId("Max Mustermann (this is a comment) <max@example.com>");
        assertEquals("Max Mustermann", info.name);
        assertEquals("this is a comment", info.comment);
        assertEquals("max@example.com", info.email);
    }

    @Test
    public void splitUserIdWithAllButCommentShouldReturnNameAndEmail() throws Exception {
        OpenPgpUtils.UserInfo info = OpenPgpUtils.splitUserId("Max Mustermann <max@example.com>");
        assertEquals("Max Mustermann", info.name);
        assertNull(info.comment);
        assertEquals("max@example.com", info.email);
    }

    @Test
    public void splitUserIdWithAllButEmailShouldReturnNameAndComment() throws Exception {
        OpenPgpUtils.UserInfo info = OpenPgpUtils.splitUserId("Max Mustermann (this is a comment)");
        assertEquals(info.name, "Max Mustermann");
        assertEquals(info.comment, "this is a comment");
        assertNull(info.email);
    }

    @Test
    public void splitUserIdWithOnlyNameShouldReturnNameOnly() throws Exception {
        OpenPgpUtils.UserInfo info = OpenPgpUtils.splitUserId("Max Mustermann [this is a nothing]");
        assertEquals("Max Mustermann", info.name);
        assertNull(info.comment);
        assertNull(info.email);
    }
}