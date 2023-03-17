package com.fsck.k9.message.html;


import java.util.List;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


public class UriMatcherTest {
    @Test
    public void emptyText() {
        assertNoMatch("");
    }

    @Test
    public void textWithoutUri() {
        assertNoMatch("some text here");
    }

    @Test
    public void simpleUri() {
        assertUrisFound("http://example.org", "http://example.org");
    }

    @Test
    public void uriPrecededBySpace() {
        assertUrisFound(" http://example.org", "http://example.org");
    }

    @Test
    public void uriPrecededByTab() {
        assertUrisFound("\thttp://example.org", "http://example.org");
    }

    @Test
    public void uriPrecededByOpeningParenthesis() {
        assertUrisFound("(http://example.org", "http://example.org");
    }

    @Test
    public void uriPrecededBySomeText() {
        assertUrisFound("Check out my fantastic URI: http://example.org", "http://example.org");
    }

    @Test
    public void uriWithTrailingText() {
        assertUrisFound("http://example.org/ is the best", "http://example.org/");
    }

    @Test
    public void uriEmbeddedInText() {
        assertUrisFound("prefix http://example.org/ suffix", "http://example.org/");
    }

    @Test
    public void uriWithUppercaseScheme() {
        assertUrisFound("HTTP://example.org/", "HTTP://example.org/");
    }

    @Test
    public void uriNotPrecededByValidSeparator() {
        assertNoMatch("myhttp://example.org");
    }

    @Test
    public void uriNotPrecededByValidSeparatorFollowedByValidUri() {
        assertUrisFound("myhttp: http://example.org", "http://example.org");
    }

    @Test
    public void schemaMatchWithInvalidUriInMiddleOfTextFollowedByValidUri() {
        assertUrisFound("prefix http:42 http://example.org", "http://example.org");
    }

    @Test
    public void multipleValidUrisInRow() {
        assertUrisFound("prefix http://uri1.example.org some text http://uri2.example.org/path postfix",
                "http://uri1.example.org", "http://uri2.example.org/path");
    }


    private void assertNoMatch(String text) {
        List<UriMatch> uriMatches = UriMatcher.INSTANCE.findUris(text);
        assertThat(uriMatches).isEmpty();
    }

    private void assertUrisFound(String text, String... uris) {
        List<UriMatch> uriMatches = UriMatcher.INSTANCE.findUris(text);
        assertThat(uriMatches).hasSize(uris.length);

        for (int i = 0, end = uris.length; i < end; i++) {
            String uri = uris[i];
            int startIndex = text.indexOf(uri);
            assertThat(startIndex).isNotEqualTo(-1);

            UriMatch uriMatch = uriMatches.get(i);
            assertThat(uriMatch.getStartIndex()).isEqualTo(startIndex);
            assertThat(uriMatch.getEndIndex()).isEqualTo(startIndex + uri.length());
            assertThat(uriMatch.getUri()).isEqualTo(uri);
        }
    }
}
