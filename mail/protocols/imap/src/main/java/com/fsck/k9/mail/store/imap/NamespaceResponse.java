package com.fsck.k9.mail.store.imap;


import java.util.List;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


class NamespaceResponse {
    private String prefix;
    private String hierarchyDelimiter;


    private NamespaceResponse(String prefix, String hierarchyDelimiter) {
        this.prefix = prefix;
        this.hierarchyDelimiter = hierarchyDelimiter;
    }

    public static NamespaceResponse parse(List<ImapResponse> responses) {
        for (ImapResponse response : responses) {
            NamespaceResponse prefix = parse(response);
            if (prefix != null) {
                return prefix;
            }
        }

        return null;
    }

    static NamespaceResponse parse(ImapResponse response) {
        if (response.size() < 4 || !equalsIgnoreCase(response.get(0), Responses.NAMESPACE)) {
            return null;
        }

        if (!response.isList(1)) {
            return null;
        }

        ImapList personalNamespaces = response.getList(1);
        if (!personalNamespaces.isList(0)) {
            return null;
        }

        ImapList firstPersonalNamespace = personalNamespaces.getList(0);
        if (!firstPersonalNamespace.isString(0) || !firstPersonalNamespace.isString(1)) {
            return null;
        }

        String prefix = firstPersonalNamespace.getString(0);
        String hierarchyDelimiter = firstPersonalNamespace.getString(1);

        return new NamespaceResponse(prefix, hierarchyDelimiter);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getHierarchyDelimiter() {
        return hierarchyDelimiter;
    }
}
