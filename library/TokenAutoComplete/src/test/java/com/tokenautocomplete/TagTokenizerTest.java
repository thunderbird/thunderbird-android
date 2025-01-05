package com.tokenautocomplete;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TagTokenizerTest {

    private TagTokenizer tokenizer;

    @Before
    public void setup() {
        tokenizer = new TagTokenizer();
    }

    @Test
    public void testBasicTagDetection() {
        String test = "@bears #tokens";
        List<Range> ranges = tokenizer.findTokenRanges(test, 0, test.length());
        assertEquals(Arrays.asList(new Range(0, 6), new Range(7, 14)), ranges);
        assertEquals("@bears", test.substring(ranges.get(0).start, ranges.get(0).end));
        assertEquals("#tokens", test.substring(ranges.get(1).start, ranges.get(1).end));
    }

    @Test
    public void testSequentialTagDetection() {
        String test = "@@bears#tokens#";
        assertEquals(Arrays.asList(new Range(1,7), new Range(7,14), new Range(14, 15)),
                tokenizer.findTokenRanges(test, 0, test.length()));
    }

    @Test
    public void testNonTokenInput() {
        String test = "This is some input with @names and #hash #tags inside";
        assertEquals(Arrays.asList(new Range(24,30), new Range(35,40), new Range(41,46)),
                tokenizer.findTokenRanges(test, 0, test.length()));
    }

    @Test
    public void testMissingTokenContentInput() {
        String test = "@token       @ asdm      @asjdfhajks      sdfasdf";
        assertEquals(Arrays.asList(new Range(0,6), new Range(25, 36)),
                tokenizer.findTokenRanges(test, 0, test.length()));
    }

    @Test
    public void allowsOneCharacterCandidateRangeMatches() {
        String text = "#";

        List<Range> ranges = tokenizer.findTokenRanges(text, 0, text.length());
        assertEquals(Collections.singletonList(new Range(0,1)), ranges);
    }

    @Test
    public void allowsOneCharacterCandidateRangeMatchesWithWhitespace() {
        String text = " #";

        List<Range> ranges = tokenizer.findTokenRanges(text, 0, text.length());
        assertEquals(Collections.singletonList(new Range(1,2)), ranges);
    }

    @Test
    public void matchesSingleLetterTokens() {
        String text = "#t#r #a##b";

        List<Range> ranges = tokenizer.findTokenRanges(text, 0, text.length());
        assertEquals(Arrays.asList(new Range(0, 2), new Range(2,4), new Range(5,7), new Range(8,10)), ranges);
    }
}
