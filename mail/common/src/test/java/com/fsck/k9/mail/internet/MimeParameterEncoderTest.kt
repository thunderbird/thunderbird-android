package com.fsck.k9.mail.internet

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.mail.testing.crlf
import org.junit.Test

class MimeParameterEncoderTest {
    @Test
    fun valueWithoutParameters() {
        val header = MimeParameterEncoder.encode("inline", emptyMap())

        assertThat(header).isEqualTo("inline")
    }

    @Test
    fun simpleParameterValue() {
        val header = MimeParameterEncoder.encode("attachment", mapOf("filename" to "kitten.png"))

        assertThat(header).isEqualTo(
            """
            |attachment;
            | filename=kitten.png
            """.trimMargin().crlf(),
        )
    }

    @Test
    fun backslashesInParameterValue() {
        val header = MimeParameterEncoder.encode(
            "attachment",
            mapOf("filename" to "Important Document \\Confidential\\.pdf"),
        )

        assertThat(header).isEqualTo(
            """
            |attachment;
            | filename="Important Document \\Confidential\\.pdf"
            """.trimMargin().crlf(),
        )
    }

    @Test
    fun nonAsciiCharactersInParameterValue() {
        val header = MimeParameterEncoder.encode("attachment", mapOf("filename" to "Übergrößenträger.dat"))

        assertThat(header).isEqualTo(
            """
            |attachment;
            | filename*=UTF-8''%C3%9Cbergr%C3%B6%C3%9Fentr%C3%A4ger.dat
            """.trimMargin().crlf(),
        )
    }

    @Test
    fun longParameterValueWithAsciiOnlyCharacters() {
        val header = MimeParameterEncoder.encode(
            "attachment",
            mapOf(
                "filename" to "This file name is quite long and exceeds the recommended header line length " +
                    "of 78 characters.txt",
            ),
        )

        // For now this is encoded like parameters that contain non-ASCII characters. However we could use
        // continuations without character set encoding to make it look like this:
        //
        // attachment;
        //  filename*0="This file name is quite long and exceeds the recommended header";
        //  filename*1=" line length of 78 characters.txt"
        assertThat(header).isEqualTo(
            """
            |attachment;
            | filename*0*=UTF-8''This%20file%20name%20is%20quite%20long%20and%20exceeds%20;
            | filename*1*=the%20recommended%20header%20line%20length%20of%2078%20character;
            | filename*2*=s.txt
            """.trimMargin().crlf(),
        )
    }

    @Test
    fun longParameterValueWithNonAsciiCharacters() {
        val header = MimeParameterEncoder.encode(
            "attachment",
            mapOf("filename" to "üüüüüüüüüüüüüüüüüüüüüü.txt", "size" to "54321"),
        )

        assertThat(header).isEqualTo(
            """
            |attachment;
            | filename*0*=UTF-8''%C3%BC%C3%BC%C3%BC%C3%BC%C3%BC%C3%BC%C3%BC%C3%BC%C3%BC;
            | filename*1*=%C3%BC%C3%BC%C3%BC%C3%BC%C3%BC%C3%BC%C3%BC%C3%BC%C3%BC%C3%BC;
            | filename*2*=%C3%BC%C3%BC%C3%BC.txt;
            | size=54321
            """.trimMargin().crlf(),
        )
    }

    @Test
    fun parameterValueWithControlCharacter() {
        val header = MimeParameterEncoder.encode(
            "value",
            mapOf("something" to "foo\u0000bar"),
        )

        assertThat(header).isEqualTo(
            """
            |value;
            | something*=UTF-8''foo%00bar
            """.trimMargin().crlf(),
        )
    }

    @Test
    fun mixedParameterValues() {
        val header = MimeParameterEncoder.encode(
            "value",
            mapOf(
                "token" to "foobar",
                "quoted" to "something containing spaces",
                "non-ascii" to "Grüße",
                "long" to "one~two~three~four~five~six~seven~eight~nine~ten~eleven~twelve~thirteen~fourteen~fifteen",
            ),
        )

        assertThat(header).isEqualTo(
            """
            |value;
            | token=foobar;
            | quoted="something containing spaces";
            | non-ascii*=UTF-8''Gr%C3%BC%C3%9Fe;
            | long*0*=UTF-8''one~two~three~four~five~six~seven~eight~nine~ten~eleven~twelv;
            | long*1*=e~thirteen~fourteen~fifteen
            """.trimMargin().crlf(),
        )
    }

    @Test
    fun nonAttributeCharactersInParameterValue() {
        val header = MimeParameterEncoder.encode(
            "value",
            mapOf(
                "param1" to "*'%",
                "param2" to "=*'%",
                "param3" to "ü*'%",
            ),
        )

        assertThat(header).isEqualTo(
            """
            |value;
            | param1=*'%;
            | param2="=*'%";
            | param3*=UTF-8''%C3%BC%2A%27%25
            """.trimMargin().crlf(),
        )
    }

    @Test(expected = UnsupportedOperationException::class)
    fun overlyLongParameterName_shouldThrow() {
        MimeParameterEncoder.encode(
            "attachment",
            mapOf("parameter_name_that_exceeds_the_line_length_recommendation_almost_on_its_own" to "foobar"),
        )
    }
}
