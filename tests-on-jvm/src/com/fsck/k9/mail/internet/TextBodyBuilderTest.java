package com.fsck.k9.mail.internet;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.experimental.theories.*;
import org.junit.runner.RunWith;

import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.activity.InsertableHtmlContent;

class TestingTextBodyBuilder extends TextBodyBuilder {

    public TestingTextBodyBuilder(
            boolean includeQuotedText,
            boolean isDraft,
            QuoteStyle quoteStyle,
            boolean replyAfterQuote,
            boolean signatureBeforeQuotedText,
            boolean signatureUse,
            String messageContent,
            String signature) {
        super(messageContent);

        if (isDraft || includeQuotedText) {
            this.setIncludeQuotedText(true);
        }
        else {
            this.setIncludeQuotedText(false);
        }

        this.setAppendSignature(!isDraft);
        this.setInsertSeparator(!isDraft);

        this.setSignatureBeforeQuotedText(signatureBeforeQuotedText);

        if (quoteStyle == QuoteStyle.PREFIX && replyAfterQuote) {
            this.setReplyAfterQuote(true);
        }
        else {
            this.setReplyAfterQuote(false);
        }

        if (signatureUse) {
            this.setSignature(signature);
        }
    }

    // HtmlConverter depends on Android.
    // So we use dummy method for tests.
    @Override
    public String textToHtmlFragment(String text) {
        return "<html>" + text + "</html>";
    }
}

@RunWith(Theories.class)
public class TextBodyBuilderTest {

    @DataPoints
    public static boolean[] BOOLEANS = { true, false };

    @DataPoints
    public static QuoteStyle[] QUOTESTYLES = {QuoteStyle.PREFIX, QuoteStyle.HEADER};

    @Theory
    public void testBuildTextPlain(
            boolean includeQuotedText,
            QuoteStyle quoteStyle,
            boolean isReplyAfterQuote,
            boolean isSignatureUse,
            boolean isSignatureBeforeQuotedText,
            boolean isDraft
            ) {

        String expectedText;
        int expectedMessageLength;
        int expectedMessagePosition;

        // 1.quoted text
        // 2.message content
        // 3.signature
        if (quoteStyle == QuoteStyle.PREFIX && isReplyAfterQuote) {
            String expectedQuotedText = "";

            if (isDraft || includeQuotedText) {
                expectedQuotedText = "quoted text" + "\r\n";
            }

            expectedText = expectedQuotedText;

            expectedText += "message content";

            if (!isDraft && isSignatureUse) {
                expectedText += "\r\n" + "signature";
            }
            expectedMessageLength = "message content".length();
            expectedMessagePosition = expectedQuotedText.length();
        }
        // 1.message content
        // 2.signature
        // 3.quoted text
        else if (isSignatureBeforeQuotedText) {
            expectedText = "message content";

            if (!isDraft && isSignatureUse) {
                expectedText += "\r\n" + "signature";
            }

            if (isDraft || includeQuotedText) {
                expectedText += "\r\n\r\nquoted text";
            }

            expectedMessageLength = "message content".length();
            expectedMessagePosition = 0;
        }
        // 1.message content
        // 2.quoted text
        // 3.signature
        else {
            expectedText = "message content";

            if (isDraft || includeQuotedText) {
                expectedText += "\r\n\r\nquoted text";
            }

            if (!isDraft && isSignatureUse) {
                expectedText += "\r\n" + "signature";
            }

            expectedMessageLength = "message content".length();
            expectedMessagePosition = 0;
        }

        String quotedText = "quoted text";
        String messageContent = "message content";
        String signature = "signature";

        TextBody textBody = new TestingTextBodyBuilder(
                includeQuotedText,
                isDraft,
                quoteStyle,
                isReplyAfterQuote,
                isSignatureBeforeQuotedText,
                isSignatureUse,
                messageContent,
                signature
                ).buildTextPlain(quotedText);

        assertThat(textBody, instanceOf(TextBody.class));
        assertThat(textBody.getText(), is(expectedText));
        assertThat(textBody.getComposedMessageLength(), is(expectedMessageLength));
        assertThat(textBody.getComposedMessageOffset(), is(expectedMessagePosition));
        assertThat(textBody.getText().substring(expectedMessagePosition, expectedMessagePosition + expectedMessageLength),
                is("message content"));
    }

    /**
     * generate expected HtmlContent debug string
     * 
     * @param expectedText
     * @param quotedContent
     * @param footerInsertionPoint
     * @param isBefore
     * @param userContent
     * @param compiledResult
     * @return expected string
     * 
     * @see InsertableHtmlContent#toDebugString()
     */
    public String makeExpectedHtmlContent(String expectedText, String quotedContent, int footerInsertionPoint, boolean isBefore,
            String userContent, String compiledResult) {
        String expectedHtmlContent = "InsertableHtmlContent{"
                + "headerInsertionPoint=0,"
                + " footerInsertionPoint=" + footerInsertionPoint + ","
                + " insertionLocation=" + (isBefore ? "BEFORE_QUOTE" : "AFTER_QUOTE") + ","
                + " quotedContent=" + quotedContent + ","
                + " userContent=" + userContent + ","
                + " compiledResult=" + compiledResult
                + "}";
        return expectedHtmlContent;
    }

    @Theory
    public void testBuildTextHtml(
            boolean includeQuotedText,
            QuoteStyle quoteStyle,
            boolean isReplyAfterQuote,
            boolean isSignatureUse,
            boolean isSignatureBeforeQuotedText,
            boolean isDraft
            ) {
        String expectedText;
        int expectedMessageLength;
        int expectedMessagePosition = 0;
        String expectedHtmlContent;

        String expectedPrefix = "";

        if (includeQuotedText && quoteStyle == QuoteStyle.PREFIX && isReplyAfterQuote && !isDraft) {
            expectedPrefix = "<br clear=\"all\">";
        }
        String expectedPostfix = "";
        if (!isDraft && includeQuotedText) {
            expectedPostfix = "<br><br>";
        }

        // 1.quoted text
        // 2.message content
        // 3.signature
        if (quoteStyle == QuoteStyle.PREFIX && isReplyAfterQuote) {
            expectedText = expectedPrefix
                    + "<html>message content";
            if (!isDraft && isSignatureUse) {
                expectedText += "\r\n" + "signature";
            }
            expectedText += "</html>";
            expectedMessageLength = expectedText.length();
            String quotedContent = "quoted text";

            if (isDraft || includeQuotedText) {
                expectedHtmlContent = makeExpectedHtmlContent(expectedText, quotedContent,
                        0,
                        false,
                        expectedText,
                        expectedText + quotedContent);
                expectedText += quotedContent;
            }
            else {
                expectedHtmlContent = makeExpectedHtmlContent(expectedText, quotedContent,
                        0,
                        true,
                        "",
                        quotedContent);
                // expectedText += quotedContent;
            }
        }
        // 1.message content
        // 2.signature
        // 3.quoted text
        else if (isSignatureBeforeQuotedText) {
            expectedText = expectedPrefix
                    + "<html>message content";
            if (!isDraft && isSignatureUse) {
                expectedText += "\r\n" + "signature";
            }
            expectedText += "</html>";
            expectedText += expectedPostfix;

            expectedMessageLength = expectedText.length();
            String quotedContent = "quoted text";

            if (isDraft || includeQuotedText) {
                expectedHtmlContent = makeExpectedHtmlContent(expectedText, quotedContent,
                        0,
                        true,
                        expectedText,
                        expectedText + quotedContent);
                expectedText += quotedContent;
            }
            else {
                expectedHtmlContent = makeExpectedHtmlContent(expectedText, quotedContent,
                        0,
                        true,
                        "",
                        quotedContent);
                // expectedText += quotedContent;
            }
        }
        // 1.message content
        // 2.quoted text
        // 3.signature
        else {
            String expectedSignature = "";

            expectedText = expectedPrefix
                    + "<html>message content";

            if (!isDraft && isSignatureUse) {
                if (!includeQuotedText) {
                    expectedText += "\r\n" + "signature";
                }
                else {
                    expectedSignature = "<html>\r\nsignature</html>";
                }
            }
            expectedText += "</html>";
            expectedText += expectedPostfix;

            expectedMessageLength = expectedText.length();
            String quotedContent = "quoted text";

            if (isDraft || includeQuotedText) {
                expectedHtmlContent = makeExpectedHtmlContent(expectedText, expectedSignature + quotedContent,
                        expectedSignature.length(),
                        true,
                        expectedText,
                        expectedText + expectedSignature + quotedContent);
                expectedText += expectedSignature + quotedContent;
            }
            else {
                expectedHtmlContent = makeExpectedHtmlContent(expectedText, quotedContent,
                        0,
                        true,
                        "",
                        quotedContent);
                // expectedText += quotedContent;
            }
        }

        InsertableHtmlContent insertableHtmlContent = new InsertableHtmlContent();

        String quotedText = "quoted text";
        insertableHtmlContent.setQuotedContent(new StringBuilder(quotedText));
        String messageContent = "message content";
        String signature = "signature";
        TextBody textBody = new TestingTextBodyBuilder(
                includeQuotedText,
                isDraft,
                quoteStyle,
                isReplyAfterQuote,
                isSignatureBeforeQuotedText,
                isSignatureUse,
                messageContent,
                signature
                ).buildTextHtml(insertableHtmlContent);

        assertThat(textBody, instanceOf(TextBody.class));
        assertThat(textBody.getText(), is(expectedText));
        assertThat(textBody.getComposedMessageLength(), is(expectedMessageLength));
        assertThat(textBody.getComposedMessageOffset(), is(expectedMessagePosition));
        assertThat(insertableHtmlContent.toDebugString(), is(expectedHtmlContent));
    }

}
