package com.fsck.k9.mail.store.imap

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import net.thunderbird.core.common.mail.Flag
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

    @Test
    fun `encodeString wraps string in double quotes`() {
        assertThat(ImapUtility.encodeString("hello")).isEqualTo("\"hello\"")
    }

    @Test
    fun `encodeString escapes backslash`() {
        assertThat(ImapUtility.encodeString("a\\b")).isEqualTo("\"a\\\\b\"")
    }

    @Test
    fun `encodeString escapes double quote`() {
        assertThat(ImapUtility.encodeString("say \"hi\"")).isEqualTo("\"say \\\"hi\\\"\"")
    }

    @Test
    fun `encodeString escapes backslash before double quote`() {
        assertThat(ImapUtility.encodeString("a\\\"b")).isEqualTo("\"a\\\\\\\"b\"")
    }

    @Test
    fun `encodeString with empty string`() {
        assertThat(ImapUtility.encodeString("")).isEqualTo("\"\"")
    }

    @Test
    fun `combineFlags with no flags returns empty string`() {
        assertThat(ImapUtility.combineFlags(emptyList(), false)).isEqualTo("")
    }

    @Test
    fun `combineFlags with seen flag`() {
        assertThat(ImapUtility.combineFlags(listOf(Flag.SEEN), false))
            .isEqualTo("\\Seen")
    }

    @Test
    fun `combineFlags with deleted flag`() {
        assertThat(ImapUtility.combineFlags(listOf(Flag.DELETED), false))
            .isEqualTo("\\Deleted")
    }

    @Test
    fun `combineFlags with answered flag`() {
        assertThat(ImapUtility.combineFlags(listOf(Flag.ANSWERED), false))
            .isEqualTo("\\Answered")
    }

    @Test
    fun `combineFlags with flagged flag`() {
        assertThat(ImapUtility.combineFlags(listOf(Flag.FLAGGED), false))
            .isEqualTo("\\Flagged")
    }

    @Test
    fun `combineFlags with draft flag`() {
        assertThat(ImapUtility.combineFlags(listOf(Flag.DRAFT), false))
            .isEqualTo("\\Draft")
    }

    @Test
    fun `combineFlags with forwarded flag when canCreateForwardedFlag is true`() {
        assertThat(ImapUtility.combineFlags(listOf(Flag.FORWARDED), true))
            .isEqualTo("\$Forwarded")
    }

    @Test
    fun `combineFlags with forwarded flag when canCreateForwardedFlag is false`() {
        assertThat(ImapUtility.combineFlags(listOf(Flag.FORWARDED), false))
            .isEqualTo("")
    }

    @Test
    fun `combineFlags ignores unknown flags`() {
        assertThat(ImapUtility.combineFlags(listOf(Flag.RECENT), false))
            .isEqualTo("")
    }

    @Test
    fun `combineFlags with multiple flags`() {
        assertThat(
            ImapUtility.combineFlags(listOf(Flag.SEEN, Flag.FLAGGED, Flag.DELETED), false),
        )
            .isEqualTo("\\Seen \\Flagged \\Deleted")
    }
}
