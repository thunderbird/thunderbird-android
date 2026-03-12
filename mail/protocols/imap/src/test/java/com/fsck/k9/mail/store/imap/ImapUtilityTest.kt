package com.fsck.k9.mail.store.imap

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Before
import org.junit.Test

class ImapUtilityTest {

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    @Test
    fun `getImapSequenceValues with single value`() {
        assertThat(ImapUtility.getImapSequenceValues("1")).containsExactly("1")
    }

    @Test
    fun `getImapSequenceValues with value above Integer MAX_VALUE`() {
        assertThat(ImapUtility.getImapSequenceValues("2147483648")).containsExactly("2147483648")
    }

    @Test
    fun `getImapSequenceValues with max 32-bit value`() {
        assertThat(ImapUtility.getImapSequenceValues("4294967295")).containsExactly("4294967295")
    }

    @Test
    fun `getImapSequenceValues with multiple values`() {
        assertThat(ImapUtility.getImapSequenceValues("1,3,2")).containsExactly("1", "3", "2")
    }

    @Test
    fun `getImapSequenceValues with ascending range`() {
        assertThat(ImapUtility.getImapSequenceValues("4:6")).containsExactly("4", "5", "6")
    }

    @Test
    fun `getImapSequenceValues with descending range`() {
        assertThat(ImapUtility.getImapSequenceValues("9:7")).containsExactly("9", "8", "7")
    }

    @Test
    fun `getImapSequenceValues with mixed values and ranges`() {
        assertThat(ImapUtility.getImapSequenceValues("1,2:4,9:7"))
            .containsExactly("1", "2", "3", "4", "9", "8", "7")
    }

    @Test
    fun `getImapSequenceValues with range crossing Integer MAX_VALUE`() {
        assertThat(ImapUtility.getImapSequenceValues("2147483646:2147483648"))
            .containsExactly("2147483646", "2147483647", "2147483648")
    }

    @Test
    fun `getImapSequenceValues with partially invalid set`() {
        assertThat(ImapUtility.getImapSequenceValues("1,x,5")).containsExactly("1", "5")
    }

    @Test
    fun `getImapSequenceValues with invalid range and valid range`() {
        assertThat(ImapUtility.getImapSequenceValues("a:d,1:3")).containsExactly("1", "2", "3")
    }

    @Test
    fun `getImapSequenceValues with empty string`() {
        assertThat(ImapUtility.getImapSequenceValues("")).isEmpty()
    }

    @Test
    fun `getImapSequenceValues with null`() {
        assertThat(ImapUtility.getImapSequenceValues(null)).isEmpty()
    }

    @Test
    fun `getImapSequenceValues with non-numeric string`() {
        assertThat(ImapUtility.getImapSequenceValues("a")).isEmpty()
    }

    @Test
    fun `getImapSequenceValues with invalid range`() {
        assertThat(ImapUtility.getImapSequenceValues("1:x")).isEmpty()
    }

    @Test
    fun `getImapSequenceValues with value exceeding 32 bits`() {
        assertThat(ImapUtility.getImapSequenceValues("4294967296:4294967297")).isEmpty()
    }

    @Test
    fun `getImapSequenceValues with single value exceeding 32 bits`() {
        assertThat(ImapUtility.getImapSequenceValues("4294967296")).isEmpty()
    }

    @Test
    fun `getImapRangeValues with ascending range`() {
        assertThat(ImapUtility.getImapRangeValues("1:3")).containsExactly("1", "2", "3")
    }

    @Test
    fun `getImapRangeValues with descending range`() {
        assertThat(ImapUtility.getImapRangeValues("16:14")).containsExactly("16", "15", "14")
    }

    @Test
    fun `getImapRangeValues with empty string`() {
        assertThat(ImapUtility.getImapRangeValues("")).isEmpty()
    }

    @Test
    fun `getImapRangeValues with null`() {
        assertThat(ImapUtility.getImapRangeValues(null)).isEmpty()
    }

    @Test
    fun `getImapRangeValues with non-numeric string`() {
        assertThat(ImapUtility.getImapRangeValues("a")).isEmpty()
    }

    @Test
    fun `getImapRangeValues with single number`() {
        assertThat(ImapUtility.getImapRangeValues("6")).isEmpty()
    }

    @Test
    fun `getImapRangeValues with range and extra segment`() {
        assertThat(ImapUtility.getImapRangeValues("1:3,6")).isEmpty()
    }

    @Test
    fun `getImapRangeValues with invalid upper bound`() {
        assertThat(ImapUtility.getImapRangeValues("1:x")).isEmpty()
    }

    @Test
    fun `getImapRangeValues with wildcard upper bound`() {
        assertThat(ImapUtility.getImapRangeValues("1:*")).isEmpty()
    }
}
