package com.fsck.k9.helper;

import junit.framework.Assert;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class HtmlConverterTest {
    // Useful if you want to write stuff to a file for debugging in a browser.
    private static final boolean WRITE_TO_FILE = false;
    private static final String OUTPUT_FILE = "C:/temp/parse.html";

    @Test
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
        String result = HtmlConverter.textToHtml(message, false);
        writeToFile(result);
        Assert.assertEquals("<pre style=\"white-space: pre-wrap; word-wrap:break-word; font-family: sans-serif\">Panama!<br /><br />Bob Barker &lt;bob@aol.com&gt; wrote:<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid rgb(204, 204, 204); padding-left: 1ex;\">a canal<br /><br />Dorothy Jo Gideon &lt;dorothy@aol.com&gt; espoused:<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid rgb(204, 204, 204); padding-left: 1ex;\">A man, a plan...<br /></blockquote>Too easy!</blockquote><br />Nice job :)<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid rgb(204, 204, 204); padding-left: 1ex;\"><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid rgb(204, 204, 204); padding-left: 1ex;\">Guess!</blockquote></blockquote></pre>", result);
    }

    @Test
    public void testTextQuoteToHtmlBlockquoteIndented() {
        String message = "*facepalm*\n" +
            "\n" +
            "Bob Barker <bob@aol.com> wrote:\n" +
            "> A wise man once said...\n" +
            ">\n" +
            ">     LOL F1RST!!!!!\n" +
            ">\n" +
            "> :)";
        String result = HtmlConverter.textToHtml(message, false);
        writeToFile(result);
        Assert.assertEquals("<pre style=\"white-space: pre-wrap; word-wrap:break-word; font-family: sans-serif\">*facepalm*<br /><br />Bob Barker &lt;bob@aol.com&gt; wrote:<br /><blockquote class=\"gmail_quote\" style=\"margin: 0pt 0pt 1ex 0.8ex; border-left: 1px solid rgb(204, 204, 204); padding-left: 1ex;\">A wise man once said...<br /><br />LOL F1RST!!!!!<br /><br />:)</blockquote></pre>", result);
    }

    private void writeToFile(final String content) {
        if(!WRITE_TO_FILE) {
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
