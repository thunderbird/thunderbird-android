package com.fsck.k9.provider;

import android.database.Cursor;
import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;
import android.test.ProviderTestCase2;
import android.test.mock.MockContentResolver;

import com.fsck.k9.Account;
import com.fsck.k9.K9;
import com.fsck.k9.Preferences;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class MessageProviderTest extends ProviderTestCase2 {

    private MockContentResolver mMockResolver;

    public MessageProviderTest() {
        super(MessageProvider.class, MessageProvider.AUTHORITY);
    }


    @Before
    public void setUp() throws Exception {
        super.setUp();
        mMockResolver = getMockContentResolver();
        mContext = K9.app;
        Preferences preferences = Preferences.getPreferences(getMockContext());
        List<Account> accountList = preferences.getAccounts();
        for (Account account: accountList) {
            preferences.deleteAccount(account);
        }
    }

    private void createAccount() {
        Preferences preferences = Preferences.getPreferences(getMockContext());
        Account account = preferences.newAccount();
        account.setDescription("TestAccount");
        account.setChipColor(10);
        account.setStoreUri("imap://user@domain.com/");
        account.save(preferences);
    }

    @Test
    public void query_forAccounts_withNoAccounts_returnsEmptyCursor() {
        Cursor cursor = mMockResolver.query(
                Uri.parse("content://" + MessageProvider.AUTHORITY + "/accounts/"),
                null, null, null, null);

        boolean isNotEmpty = cursor.moveToFirst();

        assertFalse(isNotEmpty);
    }

    @Test
    public void query_forAccounts_withAccount_returnsCursorWithData() {
        createAccount();
        Cursor cursor = mMockResolver.query(
                Uri.parse("content://" + MessageProvider.AUTHORITY + "/accounts/"),
                null, null, null, null);

        boolean isNotEmpty = cursor.moveToFirst();

        assertTrue(isNotEmpty);
    }

    @Test
    public void query_forAccounts_withAccount_withNoProjection_returnsNumberAndName() {
        createAccount();

        Cursor cursor = mMockResolver.query(
                Uri.parse("content://" + MessageProvider.AUTHORITY + "/accounts/"),
                null, null, null, null);
        cursor.moveToFirst();

        assertEquals(2, cursor.getColumnCount());
        assertEquals(0, cursor.getColumnIndex(MessageProvider.AccountColumns.ACCOUNT_NUMBER));
        assertEquals(1, cursor.getColumnIndex(MessageProvider.AccountColumns.ACCOUNT_NAME));
        assertEquals(0, cursor.getInt(0));
        assertEquals("TestAccount", cursor.getString(1));
    }


    @Test
    public void query_forInboxMessages_whenEmpty_returnsEmptyCursor() {
        Cursor cursor = mMockResolver.query(
                Uri.parse("content://" + MessageProvider.AUTHORITY + "/inbox_messages/"),
                null, null, null, null);

        boolean isNotEmpty = cursor.moveToFirst();

        assertFalse(isNotEmpty);
    }

    @Test
    public void query_forAccountUnreadMessages_whenNoAccount_returnsEmptyCursor() {
        Cursor cursor = mMockResolver.query(
                Uri.parse("content://" + MessageProvider.AUTHORITY + "/account_unread/0"),
                null, null, null, null);

        boolean isNotEmpty = cursor.moveToFirst();

        assertFalse(isNotEmpty);
    }

    @Test
    public void query_forAccountUnreadMessages_whenNoMessages_returns0Unread() {
        createAccount();
        Cursor cursor = mMockResolver.query(
                Uri.parse("content://" + MessageProvider.AUTHORITY + "/account_unread/0"),
                null, null, null, null);
        cursor.moveToFirst();

        assertEquals(2, cursor.getColumnCount());
        assertEquals(1, cursor.getColumnIndex(MessageProvider.UnreadColumns.ACCOUNT_NAME));
        assertEquals(0, cursor.getColumnIndex(MessageProvider.UnreadColumns.UNREAD));
        assertEquals(0, cursor.getInt(0));
        assertEquals("TestAccount", cursor.getString(1));
    }
}
