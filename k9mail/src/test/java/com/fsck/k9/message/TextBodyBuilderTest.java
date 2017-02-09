package com.fsck.k9.message;

import com.fsck.k9.Account.QuoteStyle;
import com.fsck.k9.mail.internet.TextBody;

import com.fsck.k9.message.quote.InsertableHtmlContent;
import org.junit.Ignore;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

//TODO: Get rid of 'Theories' and write simple tests
@Ignore
@RunWith(Theories.class)
public class TextBodyBuilderTest {

    @DataPoints
    public static boolean[] BOOLEANS = { true, false };

    @DataPoints
    public static QuoteStyle[] QUOTESTYLES = { QuoteStyle.PREFIX, QuoteStyle.HEADER };

    @Theory
    public void testBuildTextPlain(boolean includeQuotedText,
            QuoteStyle quoteStyle,
            boolean isReplyAfterQuote,
            boolean isSignatureUse,
            boolean isSignatureBeforeQuotedText,
            boolean isDraft) {

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
        String messageText = "message content";
        String signatureText = "signature";

        TestingTextBodyBuilder textBodyBuilder = new TestingTextBodyBuilder(
                includeQuotedText,
                isDraft,
                quoteStyle,
                isReplyAfterQuote,
                isSignatureBeforeQuotedText,
                isSignatureUse,
                messageText,
                signatureText
        );
        textBodyBuilder.setQuotedText(quotedText);
        TextBody textBody = textBodyBuilder.buildTextPlain();

        assertThat(textBody, instanceOf(TextBody.class));
        assertThat(textBody.getRawText(), is(expectedText));
        assertThat(textBody.getComposedMessageLength(), is(expectedMessageLength));
        assertThat(textBody.getComposedMessageOffset(), is(expectedMessagePosition));
        assertThat(textBody.getRawText().substring(expectedMessagePosition, expectedMessagePosition + expectedMessageLength),
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
    public String makeExpectedHtmlContent(String expectedText, String quotedContent,
            int footerInsertionPoint, boolean isBefore,
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
    public void testBuildTextHtml(boolean includeQuotedText,
            QuoteStyle quoteStyle,
            boolean isReplyAfterQuote,
            boolean isSignatureUse,
            boolean isSignatureBeforeQuotedText,
            boolean isDraft) {
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
            } else {
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
            } else {
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
                } else {
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
            } else {
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
        String messageText = "message content";
        String signatureText = "signature";

        TestingTextBodyBuilder textBodyBuilder = new TestingTextBodyBuilder(
                includeQuotedText,
                isDraft,
                quoteStyle,
                isReplyAfterQuote,
                isSignatureBeforeQuotedText,
                isSignatureUse,
                messageText,
                signatureText
        );
        textBodyBuilder.setQuotedTextHtml(insertableHtmlContent);
        TextBody textBody = textBodyBuilder.buildTextHtml();

        assertThat(textBody, instanceOf(TextBody.class));
        assertThat(textBody.getRawText(), is(expectedText));
        assertThat(textBody.getComposedMessageLength(), is(expectedMessageLength));
        assertThat(textBody.getComposedMessageOffset(), is(expectedMessagePosition));
        assertThat(insertableHtmlContent.toDebugString(), is(expectedHtmlContent));
    }


    static class TestingTextBodyBuilder extends TextBodyBuilder {

        public TestingTextBodyBuilder(boolean includeQuotedText,
                                      boolean isDraft,
                                      QuoteStyle quoteStyle,
                                      boolean replyAfterQuote,
                                      boolean signatureBeforeQuotedText,
                                      boolean useSignature,
                                      String messageText,
                                      String signatureText) {
            super(messageText);

            includeQuotedText = (isDraft || includeQuotedText);
            if (includeQuotedText) {
                this.setIncludeQuotedText(true);
                this.setReplyAfterQuote(quoteStyle == QuoteStyle.PREFIX && replyAfterQuote);
            } else {
                this.setIncludeQuotedText(false);
            }

            this.setInsertSeparator(!isDraft);

            useSignature = (!isDraft && useSignature);
            if (useSignature) {
                this.setAppendSignature(true);
                this.setSignature(signatureText);
                this.setSignatureBeforeQuotedText(signatureBeforeQuotedText);
            } else {
                this.setAppendSignature(false);
            }
        }

        // HtmlConverter depends on Android.
        // So we use dummy method for tests.
        @Override
        protected String textToHtmlFragment(String text) {
            return "<html>" + text + "</html>";
        }
    }
}
