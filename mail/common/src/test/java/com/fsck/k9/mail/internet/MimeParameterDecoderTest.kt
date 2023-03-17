package com.fsck.k9.mail.internet

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import org.junit.Test

class MimeParameterDecoderTest {
    @Test
    fun rfc2045_example1() {
        val mimeValue = MimeParameterDecoder.decode("text/plain; charset=us-ascii (Plain text)")

        assertThat(mimeValue.value).isEqualTo("text/plain")
        assertThat(mimeValue.parameters).containsOnly("charset" to "us-ascii")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun rfc2045_example2() {
        val mimeValue = MimeParameterDecoder.decode("text/plain; charset=\"us-ascii\"")

        assertThat(mimeValue.value).isEqualTo("text/plain")
        assertThat(mimeValue.parameters).containsOnly("charset" to "us-ascii")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun rfc2231_example1() {
        val mimeValue = MimeParameterDecoder.decode(
            "message/external-body; access-type=URL;\r\n" +
                " URL*0=\"ftp://\";\r\n" +
                " URL*1=\"cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar\"",
        )

        assertThat(mimeValue.value).isEqualTo("message/external-body")
        assertThat(mimeValue.parameters).containsOnly(
            "url" to "ftp://cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar",
            "access-type" to "URL",
        )
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun rfc2231_example2() {
        val mimeValue = MimeParameterDecoder.decode(
            "message/external-body; access-type=URL;\r\n" +
                " URL=\"ftp://cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar\"",
        )

        assertThat(mimeValue.value).isEqualTo("message/external-body")
        assertThat(mimeValue.parameters).containsOnly(
            "access-type" to "URL",
            "url" to "ftp://cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar",
        )
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun rfc2231_example3() {
        val mimeValue = MimeParameterDecoder.decode(
            "application/x-stuff;\r\n" +
                " name*=us-ascii'en-us'This%20is%20%2A%2A%2Afun%2A%2A%2A",
        )

        assertThat(mimeValue.value).isEqualTo("application/x-stuff")
        assertThat(mimeValue.parameters).containsOnly("name" to "This is ***fun***")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun rfc2231_example4() {
        val mimeValue = MimeParameterDecoder.decode(
            "application/x-stuff;\r\n" +
                " name*0*=us-ascii'en'This%20is%20even%20more%20;\r\n" +
                " name*1*=%2A%2A%2Afun%2A%2A%2A%20;\r\n" +
                " name*2=\"isn't it!\"",
        )

        assertThat(mimeValue.value).isEqualTo("application/x-stuff")
        assertThat(mimeValue.parameters).containsOnly("name" to "This is even more ***fun*** isn't it!")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun multiple_sections_out_of_order() {
        val mimeValue = MimeParameterDecoder.decode(
            "application/x-stuff;\r\n" +
                " name*2=\"[three]\";\r\n" +
                " name*1=\"[two]\";\r\n" +
                " name*0=\"[one]\"",
        )

        assertThat(mimeValue.parameters).containsOnly("name" to "[one][two][three]")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun multiple_sections_differently_cased() {
        val mimeValue = MimeParameterDecoder.decode(
            "application/x-stuff;\r\n" +
                " name*0=\"[one]\";\r\n" +
                " NAME*1=\"[two]\";\r\n" +
                " nAmE*2=\"[three]\"",
        )

        assertThat(mimeValue.parameters).containsOnly("name" to "[one][two][three]")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun multiple_sections_switching_between_extended_and_regular_value() {
        val mimeValue = MimeParameterDecoder.decode(
            "application/x-stuff;\r\n" +
                " name*0*=utf-8'en'%5Bone%5D;\r\n" +
                " name*1*=%5btwo%5d;\r\n" +
                " name*2=\"[three]\";\r\n" +
                " name*3*=%5Bfour%5D;\r\n" +
                " name*4=\"[five]\";\r\n" +
                " name*5=six",
        )

        assertThat(mimeValue.parameters).containsOnly("name" to "[one][two][three][four][five]six")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun rfc2045_and_rfc2231_style_parameters_should_use_rfc2231() {
        val mimeValue = MimeParameterDecoder.decode(
            "application/x-stuff;\r\n" +
                " name=\"filename.ext\";\r\n" +
                " name*=utf-8''filen%C3%A4me.ext",
        )

        assertThat(mimeValue.parameters).containsOnly("name" to "filenäme.ext")
        assertThat(mimeValue.ignoredParameters).containsOnly("name" to "filename.ext")
    }

    @Test
    fun duplicate_parameter_names() {
        val mimeValue = MimeParameterDecoder.decode(
            "application/x-stuff;\r\n" +
                " name=one;\r\n" +
                " extra=something;\r\n" +
                " name=two",
        )

        assertThat(mimeValue.parameters).containsOnly("extra" to "something")
        assertThat(mimeValue.ignoredParameters).containsOnly("name" to "one", "name" to "two")
    }

    @Test
    fun duplicate_parameter_names_differing_in_case() {
        val mimeValue = MimeParameterDecoder.decode(
            "application/x-stuff;\r\n" +
                " name=one;\r\n" +
                " extra=something;\r\n" +
                " NAME=two",
        )

        assertThat(mimeValue.parameters).containsOnly("extra" to "something")
        assertThat(mimeValue.ignoredParameters).containsOnly("name" to "one", "name" to "two")
    }

    @Test
    fun name_only_parameter() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; parameter")

        assertThat(mimeValue.parserErrorIndex).isEqualTo(30)
        assertThat(mimeValue.parameters).isEmpty()
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun missing_parameter_value() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; parameter=")

        assertThat(mimeValue.parserErrorIndex).isEqualTo(31)
        assertThat(mimeValue.parameters).isEmpty()
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun comments_everywhere() {
        val mimeValue = MimeParameterDecoder.decode(
            "(comment)application(comment)/(comment)x-stuff" +
                "(comment);(comment)\r\n" +
                " (comment)name(comment)=(comment)one(comment);(comment)\r\n" +
                "  (comment) extra (comment) = (comment) something (comment)",
        )

        assertThat(mimeValue.parameters).containsOnly("name" to "one", "extra" to "something")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun iso8859_1_charset() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*=iso-8859-1''filen%E4me.ext")

        assertThat(mimeValue.parameters).containsOnly("name" to "filenäme.ext")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun missing_charset() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*=''filen%AAme.ext")

        assertThat(mimeValue.parameters).containsOnly("name" to "filen%AAme.ext")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun unknown_charset() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*=foobar''filen%AAme.ext")

        assertThat(mimeValue.parameters).containsOnly("name" to "filen%AAme.ext")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun section_index_missing() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name**=utf-8''filename")

        assertThat(mimeValue.parameters).containsOnly("name**" to "utf-8''filename")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun section_index_not_a_number() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*x*=filename")

        assertThat(mimeValue.parameters).containsOnly("name*x*" to "filename")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun section_index_prefixed_with_plus() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*+0=filename")

        assertThat(mimeValue.parameters).containsOnly("name*+0" to "filename")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun section_index_prefixed_with_minus() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*-0=filename")

        assertThat(mimeValue.parameters).containsOnly("name*-0" to "filename")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun section_index_with_two_zeros() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*00=filename")

        assertThat(mimeValue.parameters).containsOnly("name*00" to "filename")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun section_index_with_leading_zero() {
        val mimeValue = MimeParameterDecoder.decode(
            "application/x-stuff;\r\n" +
                " name*0=one;\r\n" +
                " name*01=two",
        )

        assertThat(mimeValue.parameters).containsOnly("name" to "one", "name*01" to "two")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun section_index_with_huge_number() {
        val mimeValue = MimeParameterDecoder.decode(
            "application/x-stuff;\r\n" +
                " name*10000000000000000000=filename",
        )

        assertThat(mimeValue.parameters).containsOnly("name*10000000000000000000" to "filename")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun extended_parameter_name_with_additional_asterisk() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*0**=utf-8''filename")

        assertThat(mimeValue.parameters).containsOnly("name*0**" to "utf-8''filename")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun extended_parameter_name_with_additional_text() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*0*x=utf-8''filename")

        assertThat(mimeValue.parameters).containsOnly("name*0*x" to "utf-8''filename")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun extended_parameter_value_with_quoted_string() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*0*=\"utf-8''filename\"")

        assertThat(mimeValue.parameters).containsOnly("name*0*" to "utf-8''filename")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun extended_initial_parameter_value_missing_single_quotes() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*0*=filename")

        assertThat(mimeValue.parameters).containsOnly("name*0*" to "filename")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun extended_initial_parameter_value_missing_second_single_quote() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*0*='")

        assertThat(mimeValue.parameters).containsOnly("name*0*" to "'")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun extended_parameter_value_with_trailing_percent_sign() {
        val mimeValue = MimeParameterDecoder.decode("attachment; filename*=utf-8''file%")

        assertThat(mimeValue.parameters).containsOnly("filename*" to "utf-8''file%")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun extended_parameter_value_with_invalid_percent_encoding() {
        val mimeValue = MimeParameterDecoder.decode("attachment; filename*=UTF-8''f%oo.html")

        assertThat(mimeValue.parameters).containsOnly("filename*" to "UTF-8''f%oo.html")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun section_0_missing() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name*1=filename")

        assertThat(mimeValue.parameters).containsOnly("name*1" to "filename")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun semicolon_in_parameter_value() {
        val mimeValue = MimeParameterDecoder.decode("attachment; filename=\"Here's a semicolon;.txt\"")

        assertThat(mimeValue.parameters).containsOnly("filename" to "Here's a semicolon;.txt")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun rfc2047_encoded() {
        val mimeValue = MimeParameterDecoder.decode(
            "application/x-stuff;\r\n" +
                " name=\"=?UTF-8?Q?filn=C3=A4me=2Eext?=\"",
        )

        assertThat(mimeValue.parameters).containsOnly("name" to "filnäme.ext")
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun rfc2047_encoded_multiple_lines() {
        val mimeValue = MimeParameterDecoder.decode(
            "application/x-stuff;\r\n" +
                " name=\"=?UTF-8?Q?File_name_that_is_so_long_it_likes_to_be_wrapped_i?=\r\n" +
                " =?UTF-8?Q?nto_multiple_lines=2E_Also_?=\r\n" +
                " =?UTF-8?Q?non-ASCII_characters=3A_=C3=A4=E2=82=AC=F0=9F=8C=9E?=\"",
        )

        assertThat(mimeValue.parameters).containsOnly(
            "name" to "File name that is so long it likes to be wrapped " +
                "into multiple lines. Also non-ASCII characters: ä€\uD83C\uDF1E",
        )
        assertThat(mimeValue.ignoredParameters).isEmpty()
    }

    @Test
    fun `UTF-8 data in header value`() {
        val mimeValue = MimeParameterDecoder.decode("application/x-stuff; name=\"filenäme.ext\"")

        assertThat(mimeValue.parameters).containsOnly("name" to "filenäme.ext")

        assertThat(mimeValue.ignoredParameters).isEmpty()
    }
}
