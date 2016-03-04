package com.fsck.k9.mail.store.imap;


import java.io.IOException;
import java.util.List;

import org.junit.Test;

import static com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;


public class ListResponseTest {
    @Test
    public void parseList_withValidResponses_shouldReturnListResponses() throws Exception {
        List<ImapResponse> responses = asList(
                createImapResponse("* LIST () \"/\" blurdybloop"),
                createImapResponse("* LIST (\\Noselect) \"/\" foo"),
                createImapResponse("* LIST () \"/\" foo/bar"),
                createImapResponse("X OK LIST completed")
        );

        List<ListResponse> result = ListResponse.parseList(responses);

        assertEquals(3, result.size());
        assertListResponseEquals(noAttributes(), "/", "blurdybloop", result.get(0));
        assertListResponseEquals(singletonList("\\Noselect"), "/", "foo", result.get(1));
        assertListResponseEquals(noAttributes(), "/", "foo/bar", result.get(2));
    }

    @Test
    public void parseList_withValidResponse_shouldReturnListResponse() throws Exception {
        List<ListResponse> result = parseSingle("* LIST () \".\" \"Folder\"");

        assertEquals(1, result.size());
        assertListResponseEquals(noAttributes(), ".", "Folder", result.get(0));
    }

    @Test
    public void parseList_withValidResponseContainingAttributes_shouldReturnListResponse() throws Exception {
        List<ListResponse> result = parseSingle("* LIST (\\HasChildren \\Noselect) \".\" \"Folder\"");

        assertEquals(1, result.size());
        assertListResponseEquals(asList("\\HasChildren", "\\Noselect"), ".", "Folder", result.get(0));
    }

    @Test
    public void parseList_withoutListResponse_shouldReturnEmptyList() throws Exception {
        List<ListResponse> result = parseSingle("* LSUB () \".\" INBOX");

        assertEquals(emptyList(), result);
    }

    @Test
    public void parseList_withMalformedListResponse1_shouldReturnEmptyList() throws Exception {
        List<ListResponse> result = parseSingle("* LIST ([inner list]) \"/\" \"Folder\"");

        assertEquals(emptyList(), result);
    }

    @Test
    public void parseList_withMalformedListResponse2_shouldReturnEmptyList() throws Exception {
        List<ListResponse> result = parseSingle("* LIST () \"ab\" \"Folder\"");

        assertEquals(emptyList(), result);
    }

    @Test
    public void parseLsub_withValidResponse_shouldReturnListResponse() throws Exception {
        List<ImapResponse> responses = singletonList(createImapResponse("* LSUB () \".\" \"Folder\""));

        List<ListResponse> result = ListResponse.parseLsub(responses);

        assertEquals(1, result.size());
        assertListResponseEquals(noAttributes(), ".", "Folder", result.get(0));
    }

    private List<ListResponse> parseSingle(String response) throws IOException {
        List<ImapResponse> responses = singletonList(createImapResponse(response));

        return ListResponse.parseList(responses);
    }

    private List<String> noAttributes() {
        return emptyList();
    }

    private void assertListResponseEquals(List<String> attributes, String delimiter, String name,
            ListResponse listResponse) {
        assertEquals(attributes, listResponse.getAttributes());
        assertEquals(delimiter, listResponse.getHierarchyDelimiter());
        assertEquals(name, listResponse.getName());
    }
}
