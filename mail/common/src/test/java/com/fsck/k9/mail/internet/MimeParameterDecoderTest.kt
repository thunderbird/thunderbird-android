package com.fsck.k9.mail.internet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MimeParameterDecoderTest {
    @Test
    fun rfc2045_example1() {
        val mimeValue = MimeParameterDecoder.decode("text/plain; charset=us-ascii (Plain text)")

        assertEquals("text/plain", mimeValue.value)
        assertParametersEquals(mimeValue, "charset" to "us-ascii")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun rfc2045_example2() {
        val mimeValue = MimeParameterDecoder.decode("text/plain; charset=\"us-ascii\"")

        assertEquals("text/plain", mimeValue.value)
        assertParametersEquals(mimeValue, "charset" to "us-ascii")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun rfc2231_example1() {
        val mimeValue = MimeParameterDecoder.decode("message/external-body; access-type=URL;\r\n" +
                " URL*0=\"ftp://\";\r\n" +
                " URL*1=\"cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar\"")

        assertEquals("message/external-body", mimeValue.value)
        assertParametersEquals(mimeValue,
                "access-type" to "URL",
                "url" to "ftp://cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun rfc2231_example2() {
        val mimeValue = MimeParameterDecoder.decode("message/external-body; access-type=URL;\r\n" +
                " URL=\"ftp://cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar\"")

        assertEquals("message/external-body", mimeValue.value)
        assertParametersEquals(mimeValue,
                "access-type" to "URL",
                "url" to "ftp://cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun rfc2231_example3() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff;\r\n" +
                " name*=us-ascii'en-us'This%20is%20%2A%2A%2Afun%2A%2A%2A")

        assertEquals("application/x-stuff", mimeValue.value)
        assertParametersEquals(mimeValue, "name" to "This is ***fun***")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun rfc2231_example4() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff;\r\n" +
                " name*0*=us-ascii'en'This%20is%20even%20more%20;\r\n" +
                " name*1*=%2A%2A%2Afun%2A%2A%2A%20;\r\n" +
                " name*2=\"isn't it!\"")

        assertEquals("application/x-stuff", mimeValue.value)
        assertParametersEquals(mimeValue, "name" to "This is even more ***fun*** isn't it!")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun multiple_sections_out_of_order() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff;\r\n" +
                " name*2=\"[three]\";\r\n" +
                " name*1=\"[two]\";\r\n" +
                " name*0=\"[one]\"")

        assertParametersEquals(mimeValue, "name" to "[one][two][three]")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun multiple_sections_differently_cased() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff;\r\n" +
                " name*0=\"[one]\";\r\n" +
                " name*1=\"[two]\";\r\n" +
                " name*2=\"[three]\"")

        assertParametersEquals(mimeValue, "name" to "[one][two][three]")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun multiple_sections_switching_between_extended_and_regular_value() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff;\r\n" +
                " name*0*=utf-8'en'%5Bone%5D;\r\n" +
                " name*1*=%5btwo%5d;\r\n" +
                " name*2=\"[three]\";\r\n" +
                " name*3*=%5Bfour%5D;\r\n" +
                " name*4=\"[five]\";\r\n" +
                " name*5=six")

        assertParametersEquals(mimeValue, "name" to "[one][two][three][four][five]six")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun rfc2045_and_rfc2231_style_parameters_should_use_rfc2231() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff;\r\n" +
                " name=\"filename.ext\";\r\n" +
                " name*=utf-8''filen%C3%A4me.ext")

        assertParametersEquals(mimeValue, "name" to "filenäme.ext")
        assertIgnoredParametersEquals(mimeValue, "name" to "filename.ext")
    }

    @Test
    fun duplicate_parameter_names() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff;\r\n" +
                " name=one;\r\n" +
                " extra=something;\r\n" +
                " name=two")

        assertParametersEquals(mimeValue, "extra" to "something")
        assertIgnoredParametersEquals(mimeValue, "name" to "one", "name" to "two")
    }

    @Test
    fun duplicate_parameter_names_differing_in_case() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff;\r\n" +
                " name=one;\r\n" +
                " extra=something;\r\n" +
                " NAME=two")

        assertParametersEquals(mimeValue, "extra" to "something")
        assertIgnoredParametersEquals(mimeValue, "name" to "one", "name" to "two")
    }

    @Test
    fun name_only_parameter() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; parameter")

        assertEquals(30, mimeValue.parserErrorIndex)
        assertTrue(mimeValue.parameters.isEmpty())
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun missing_parameter_value() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; parameter=")

        assertEquals(31, mimeValue.parserErrorIndex)
        assertTrue(mimeValue.parameters.isEmpty())
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun comments_everywhere() {
        val mimeValue = MimeParameterDecoder.decode("(comment)application(comment)/(comment)x-stuff" +
                "(comment);(comment)\r\n" +
                " (comment)name(comment)=(comment)one(comment);(comment)\r\n" +
                "  (comment) extra (comment) = (comment) something (comment)")

        assertParametersEquals(mimeValue, "name" to "one", "extra" to "something")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun iso8859_1_charset() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*=iso-8859-1''filen%E4me.ext")

        assertParametersEquals(mimeValue, "name" to "filenäme.ext")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun missing_charset() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*=''filen%AAme.ext")

        assertParametersEquals(mimeValue, "name" to "filen%AAme.ext")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun unknown_charset() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*=foobar''filen%AAme.ext")

        assertParametersEquals(mimeValue, "name" to "filen%AAme.ext")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun section_index_missing() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name**=utf-8''filename")

        assertParametersEquals(mimeValue, "name**" to "utf-8''filename")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun section_index_not_a_number() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*x*=filename")

        assertParametersEquals(mimeValue, "name*x*" to "filename")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun section_index_prefixed_with_plus() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*+0=filename")

        assertParametersEquals(mimeValue, "name*+0" to "filename")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun section_index_prefixed_with_minus() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*-0=filename")

        assertParametersEquals(mimeValue, "name*-0" to "filename")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun section_index_with_two_zeros() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*00=filename")

        assertParametersEquals(mimeValue, "name*00" to "filename")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun section_index_with_leading_zero() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff;\r\n" +
                " name*0=one;\r\n" +
                " name*01=two")

        assertParametersEquals(mimeValue, "name" to "one", "name*01" to "two")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun section_index_with_huge_number() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff;\r\n" +
                " name*10000000000000000000=filename")

        assertParametersEquals(mimeValue, "name*10000000000000000000" to "filename")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun extended_parameter_name_with_additional_asterisk() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*0**=utf-8''filename")

        assertParametersEquals(mimeValue, "name*0**" to "utf-8''filename")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun extended_parameter_name_with_additional_text() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*0*x=utf-8''filename")

        assertParametersEquals(mimeValue, "name*0*x" to "utf-8''filename")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun extended_parameter_value_with_quoted_string() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*0*=\"utf-8''filename\"")

        assertParametersEquals(mimeValue, "name*0*" to "utf-8''filename")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun extended_initial_parameter_value_missing_single_quotes() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*0*=filename")

        assertParametersEquals(mimeValue, "name*0*" to "filename")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun extended_initial_parameter_value_missing_second_single_quote() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*0*='")

        assertParametersEquals(mimeValue, "name*0*" to "'")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun extended_parameter_value_with_trailing_percent_sign() {
        val mimeValue = MimeParameterDecoder.decode("attachment; filename*=utf-8''file%")

        assertParametersEquals(mimeValue, "filename*" to "utf-8''file%")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun extended_parameter_value_with_invalid_percent_encoding() {
        val mimeValue = MimeParameterDecoder.decode("attachment; filename*=UTF-8''f%oo.html")

        assertParametersEquals(mimeValue, "filename*" to "UTF-8''f%oo.html")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun section_0_missing() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*1=filename")

        assertParametersEquals(mimeValue, "name*1" to "filename")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun semicolon_in_parameter_value() {
        val mimeValue = MimeParameterDecoder.decode("attachment; filename=\"Here's a semicolon;.txt\"")

        assertParametersEquals(mimeValue, "filename" to "Here's a semicolon;.txt")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun rfc2047_encoded() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff;\r\n" +
                " name=\"=?UTF-8?Q?filn=C3=A4me=2Eext?=\"")

        assertParametersEquals(mimeValue, "name" to "filnäme.ext")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    @Test
    fun rfc2047_encoded_multiple_lines() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff;\r\n" +
                " name=\"=?UTF-8?Q?File_name_that_is_so_long_it_likes_to_be_wrapped_i?=\r\n" +
                " =?UTF-8?Q?nto_multiple_lines=2E_Also_?=\r\n" +
                " =?UTF-8?Q?non-ASCII_characters=3A_=C3=A4=E2=82=AC=F0=9F=8C=9E?=\"")

        assertParametersEquals(mimeValue, "name" to "File name that is so long it likes to be wrapped " +
                "into multiple lines. Also non-ASCII characters: ä€\uD83C\uDF1E")
        assertTrue(mimeValue.ignoredParameters.isEmpty())
    }

    private fun assertParametersEquals(mimeValue: MimeValue, vararg expected: Pair<String, String>) {
        assertEquals(expected.toSet(), mimeValue.parameters.toPairSet())
    }

    private fun assertIgnoredParametersEquals(mimeValue: MimeValue, vararg expected: Pair<String, String>) {
        assertEquals(expected.toSet(), mimeValue.ignoredParameters.toSet())
    }

    private fun Map<String, String>.toPairSet(): Set<Pair<String, String>> {
        return this.map { (key, value) -> key to value }.toSet()
    }
}
