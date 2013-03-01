package com.fsck.k9.helper;

import junit.framework.TestCase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class HtmlConverterTest extends TestCase {
    // Useful if you want to write stuff to a file for debugging in a browser.
    private static final boolean WRITE_TO_FILE = Boolean.parseBoolean(System.getProperty("k9.htmlConverterTest.writeToFile", "false"));
    private static final String OUTPUT_FILE = "C:/temp/parse.html";

    public void testTextQuoteToHtmlBlockquote() {
        String message = "Panama!\n" +
            "\n" +
            "Bob Barker <bob@aol.com> wrote:\n" +
            "> a canal\n" +
            ">\n" +
            "> Dorothy Jo Gideon <dorothy@aol.com> espoused:\n" +
            "> >A man, a plan...\n" +
            "> Too easy!\n" +
            "\n" +
            "Nice job :)\n" +
            ">> Guess!";
        String result = HtmlConverter.textToHtml(message);
        writeToFile(result);
        assertEquals("<pre class=\"k9mail\">Panama!<br /><br />Bob Barker &lt;bob@aol.com&gt; wrote:<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #729fcf; padding-left: 1ex;\">a canal<br /><br />Dorothy Jo Gideon &lt;dorothy@aol.com&gt; espoused:<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #ad7fa8; padding-left: 1ex;\">A man, a plan...<br /></blockquote>Too easy!</blockquote><br />Nice job :)<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #729fcf; padding-left: 1ex;\"><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #ad7fa8; padding-left: 1ex;\">Guess!</blockquote></blockquote></pre>", result);
    }

    public void testTextQuoteToHtmlBlockquoteIndented() {
        String message = "*facepalm*\n" +
            "\n" +
            "Bob Barker <bob@aol.com> wrote:\n" +
            "> A wise man once said...\n" +
            ">\n" +
            ">     LOL F1RST!!!!!\n" +
            ">\n" +
            "> :)";
        String result = HtmlConverter.textToHtml(message);
        writeToFile(result);
        assertEquals("<pre class=\"k9mail\">*facepalm*<br /><br />Bob Barker &lt;bob@aol.com&gt; wrote:<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #729fcf; padding-left: 1ex;\">A wise man once said...<br /><br />LOL F1RST!!!!!<br /><br />:)</blockquote></pre>", result);
    }

    public void testQuoteDepthColor() {
        assertEquals(HtmlConverter.getQuoteColor(1), HtmlConverter.QUOTE_COLOR_LEVEL_1);
        assertEquals(HtmlConverter.getQuoteColor(2), HtmlConverter.QUOTE_COLOR_LEVEL_2);
        assertEquals(HtmlConverter.getQuoteColor(3), HtmlConverter.QUOTE_COLOR_LEVEL_3);
        assertEquals(HtmlConverter.getQuoteColor(4), HtmlConverter.QUOTE_COLOR_LEVEL_4);
        assertEquals(HtmlConverter.getQuoteColor(5), HtmlConverter.QUOTE_COLOR_LEVEL_5);

        assertEquals(HtmlConverter.getQuoteColor(-1), HtmlConverter.QUOTE_COLOR_DEFAULT);
        assertEquals(HtmlConverter.getQuoteColor(0), HtmlConverter.QUOTE_COLOR_DEFAULT);
        assertEquals(HtmlConverter.getQuoteColor(6), HtmlConverter.QUOTE_COLOR_DEFAULT);

        String message = "zero\n" +
            "> one\n" +
            ">> two\n" +
            ">>> three\n" +
            ">>>> four\n" +
            ">>>>> five\n" +
            ">>>>>> six";
        String result = HtmlConverter.textToHtml(message);
        writeToFile(result);
        assertEquals("<pre class=\"k9mail\">zero<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #729fcf; padding-left: 1ex;\">one<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #ad7fa8; padding-left: 1ex;\">two<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #8ae234; padding-left: 1ex;\">three<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #fcaf3e; padding-left: 1ex;\">four<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #e9b96e; padding-left: 1ex;\">five<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid #ccc; padding-left: 1ex;\">six</blockquote></blockquote></blockquote></blockquote></blockquote></blockquote></pre>", result);
    }

    private void writeToFile(final String content) {
        if (!WRITE_TO_FILE) {
            return;
        }
        try {
            System.err.println(content);

            File f = new File(OUTPUT_FILE);
            f.delete();

            FileWriter fstream = new FileWriter(OUTPUT_FILE);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(content);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
