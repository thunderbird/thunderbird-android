package com.fsck.k9.mail.store.imap;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.support.annotation.Nullable;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;

class ListResponse {
    private final List<String> attributes;
    private final String hierarchyDelimiter;
    private final String name;


    private ListResponse(List<String> attributes, String hierarchyDelimiter, String name) {
        this.attributes = Collections.unmodifiableList(attributes);
        this.hierarchyDelimiter = hierarchyDelimiter;
        this.name = name;
    }

    public static List<ListResponse> parseList(List<ImapResponse> responses) {
        return parse(responses, Responses.LIST);
    }

    public static List<ListResponse> parseLsub(List<ImapResponse> responses) {
        return parse(responses, Responses.LSUB);
    }

    private static List<ListResponse> parse(List<ImapResponse> responses, String commandResponse) {
        List<ListResponse> listResponses = new ArrayList<>();

        for (ImapResponse response : responses) {
            ListResponse listResponse = parseSingleLine(response, commandResponse);
            if (listResponse != null) {
                listResponses.add(listResponse);
            }
        }

        return Collections.unmodifiableList(listResponses);
    }

    private static ListResponse parseSingleLine(ImapResponse response, String commandResponse) {
        if (response.size() < 4 || !equalsIgnoreCase(response.get(0), commandResponse)) {
            return null;
        }

        // We have special support for LIST responses in ImapResponseParser so we can relax the length/type checks here

        List<String> attributes = extractAttributes(response);
        if (attributes == null) {
            return null;
        }

        String hierarchyDelimiter = response.getString(2);
        if (hierarchyDelimiter != null && hierarchyDelimiter.length() != 1) {
            return null;
        }

        String name = response.getString(3);

        return new ListResponse(attributes, hierarchyDelimiter, name);
    }

    private static List<String> extractAttributes(ImapResponse response) {
        ImapList nameAttributes = response.getList(1);
        List<String> attributes = new ArrayList<>(nameAttributes.size());

        for (Object nameAttribute : nameAttributes) {
            if (!(nameAttribute instanceof String)) {
                return null;
            }

            String attribute = (String) nameAttribute;
            attributes.add(attribute);
        }

        return attributes;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public boolean hasAttribute(String attribute) {
        for (String attributeInResponse : attributes) {
            if (attributeInResponse.equalsIgnoreCase(attribute)) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    public String getHierarchyDelimiter() {
        return hierarchyDelimiter;
    }

    public String getName() {
        return name;
    }
}
