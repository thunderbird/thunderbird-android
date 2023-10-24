package com.fsck.k9.helper

import android.net.Uri
import app.k9mail.core.android.testing.RobolectricTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.helper.MailTo.CaseInsensitiveParamWrapper
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

class MailToTest : RobolectricTest() {

    @get:Rule
    val exception: ExpectedException = ExpectedException.none()

    @Test
    fun testIsMailTo_validMailToURI() {
        val uri = Uri.parse("mailto:nobody")

        val result = MailTo.isMailTo(uri)

        assertThat(result).isTrue()
    }

    @Test
    fun testIsMailTo_invalidMailToUri() {
        val uri = Uri.parse("http://example.org/")

        val result = MailTo.isMailTo(uri)

        assertThat(result).isFalse()
    }

    @Test
    fun testIsMailTo_nullArgument() {
        val uri: Uri? = null

        val result = MailTo.isMailTo(uri)

        assertThat(result).isFalse()
    }

    @Test
    @Throws(Exception::class)
    fun parse_withNullArgument_shouldThrow() {
        exception.expect(NullPointerException::class.java)
        exception.expectMessage("Argument 'uri' must not be null")

        MailTo.parse(null)
    }

    @Test
    @Throws(Exception::class)
    fun parse_withoutMailtoUri_shouldThrow() {
        exception.expect(IllegalArgumentException::class.java)
        exception.expectMessage("Not a mailto scheme")

        val uri = Uri.parse("http://example.org/")

        MailTo.parse(uri)
    }

    @Test
    fun testGetTo_singleEmailAddress() {
        val uri = Uri.parse("mailto:test@abc.com")
        val mailToHelper = MailTo.parse(uri)

        val emailAddressList = mailToHelper.to

        assertThat(emailAddressList[0].address).isEqualTo("test@abc.com")
    }

    @Test
    fun testGetTo_multipleEmailAddress() {
        val uri = Uri.parse("mailto:test1@abc.com?to=test2@abc.com")
        val mailToHelper = MailTo.parse(uri)

        val emailAddressList = mailToHelper.to

        assertThat(emailAddressList[0].address).isEqualTo("test1@abc.com")
        assertThat(emailAddressList[1].address).isEqualTo("test2@abc.com")
    }

    @Test
    fun testGetCc_singleEmailAddress() {
        val uri = Uri.parse("mailto:test1@abc.com?cc=test3@abc.com")
        val mailToHelper = MailTo.parse(uri)

        val emailAddressList = mailToHelper.cc

        assertThat(emailAddressList[0].address).isEqualTo("test3@abc.com")
    }

    @Test
    fun testGetCc_multipleEmailAddress() {
        val uri = Uri.parse("mailto:test1@abc.com?cc=test3@abc.com,test4@abc.com")
        val mailToHelper = MailTo.parse(uri)

        val emailAddressList = mailToHelper.cc

        assertThat(emailAddressList[0].address).isEqualTo("test3@abc.com")
        assertThat(emailAddressList[1].address).isEqualTo("test4@abc.com")
    }

    @Test
    fun testGetBcc_singleEmailAddress() {
        val uri = Uri.parse("mailto:?bcc=test3@abc.com")
        val mailToHelper = MailTo.parse(uri)

        val emailAddressList = mailToHelper.bcc

        assertThat(emailAddressList[0].address).isEqualTo("test3@abc.com")
    }

    @Test
    fun testGetBcc_multipleEmailAddress() {
        val uri = Uri.parse("mailto:?bcc=test3@abc.com&bcc=test4@abc.com")
        val mailToHelper = MailTo.parse(uri)

        val emailAddressList = mailToHelper.bcc

        assertThat(emailAddressList[0].address).isEqualTo("test3@abc.com")
        assertThat(emailAddressList[1].address).isEqualTo("test4@abc.com")
    }

    @Test
    fun testGetSubject() {
        val uri = Uri.parse("mailto:?subject=Hello")
        val mailToHelper = MailTo.parse(uri)

        val subject = mailToHelper.subject

        assertThat(subject).isEqualTo("Hello")
    }

    @Test
    fun testGetBody() {
        val uri = Uri.parse("mailto:?body=Test%20Body&something=else")
        val mailToHelper = MailTo.parse(uri)

        val subject = mailToHelper.body

        assertThat(subject).isEqualTo("Test Body")
    }

    @Test
    fun testCaseInsensitiveParamWrapper() {
        val uri = Uri.parse("scheme://authority?a=one&b=two&c=three")
        val caseInsensitiveParamWrapper = CaseInsensitiveParamWrapper(uri)

        val result = caseInsensitiveParamWrapper.getQueryParameters("b")

        assertThat(result).isEqualTo(listOf("two"))
    }

    @Test
    fun testCaseInsensitiveParamWrapper_multipleMatchingQueryParameters() {
        val uri = Uri.parse("scheme://authority?xname=one&name=two&Name=Three&NAME=FOUR")
        val caseInsensitiveParamWrapper = CaseInsensitiveParamWrapper(uri)

        val result = caseInsensitiveParamWrapper.getQueryParameters("name")

        assertThat(result).isEqualTo(listOf("two", "Three", "FOUR"))
    }

    @Test
    fun testCaseInsensitiveParamWrapper_withoutQueryParameters() {
        val uri = Uri.parse("scheme://authority")
        val caseInsensitiveParamWrapper = CaseInsensitiveParamWrapper(uri)

        val result = caseInsensitiveParamWrapper.getQueryParameters("name")

        assertThat(result.isEmpty()).isTrue()
    }

    @Test
    fun testGetInReplyTo_singleMessageId() {
        val uri = Uri.parse("mailto:?in-reply-to=%3C7C72B202-73F3@somewhere%3E")
        val mailToHelper = MailTo.parse(uri)

        assertThat(mailToHelper.inReplyTo).isEqualTo("<7C72B202-73F3@somewhere>")
    }

    @Test
    fun testGetInReplyTo_multipleMessageIds() {
        val uri = Uri.parse("mailto:?in-reply-to=%3C7C72B202-73F3@somewhere%3E%3C8A39-1A87CB40C114@somewhereelse%3E")

        val mailToHelper = MailTo.parse(uri)

        assertThat(mailToHelper.inReplyTo).isEqualTo("<7C72B202-73F3@somewhere>")
    }

    @Test
    fun testGetInReplyTo_RFC6068Example() {
        val uri = Uri.parse("mailto:list@example.org?In-Reply-To=%3C3469A91.D10AF4C@example.com%3E")

        val mailToHelper = MailTo.parse(uri)

        assertThat(mailToHelper.inReplyTo).isEqualTo("<3469A91.D10AF4C@example.com>")
    }

    @Test
    fun testGetInReplyTo_invalid() {
        val uri = Uri.parse("mailto:?in-reply-to=7C72B202-73F3somewhere")

        val mailToHelper = MailTo.parse(uri)

        assertThat(mailToHelper.inReplyTo).isNull()
    }

    @Test
    fun testGetInReplyTo_empty() {
        val uri = Uri.parse("mailto:?in-reply-to=")

        val mailToHelper = MailTo.parse(uri)

        assertThat(mailToHelper.inReplyTo).isNull()
    }
}
