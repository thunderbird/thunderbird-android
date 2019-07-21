package com.fsck.k9.fragment

import android.database.Cursor
import com.fsck.k9.Account
import com.fsck.k9.Preferences
import junit.framework.Assert.assertEquals
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock

class AccountRetrieverTest {

    @Test fun shouldRetrieveAccountCorrectly() {
        // given
        val someUuid = "someUuid";
        val expectedAccount = Account(someUuid)
        val preferences = mock(Preferences::class.java)
        given(preferences.getAccount(anyString()))
                .willReturn(expectedAccount)
        val accountRetriever = AccountRetriever(preferences)
        val mockedParamCursor = mock(Cursor::class.java)
        given(mockedParamCursor.getString(anyInt()))
                .willReturn(someUuid)

        // when
        val actualAccount = accountRetriever(mockedParamCursor)

        // then
        assertEquals(expectedAccount, actualAccount)
    }
}