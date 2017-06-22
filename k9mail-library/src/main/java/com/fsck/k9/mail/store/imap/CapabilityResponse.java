package com.fsck.k9.mail.store.imap;


import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


class CapabilityResponse {
    private final Set<String> capabilities;


    private CapabilityResponse(Set<String> capabilities) {
        this.capabilities = Collections.unmodifiableSet(capabilities);
    }

    public static CapabilityResponse parse(List<ImapResponse> responses) {
        for (ImapResponse response : responses) {
            CapabilityResponse result;
            if (!response.isEmpty() && equalsIgnoreCase(response.get(0), Responses.OK) && response.isList(1)) {
                ImapList capabilityList = response.getList(1);
                result = parse(capabilityList);
            } else if (response.getTag() == null) {
                result = parse(response);
            } else {
                result = null;
            }

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    static CapabilityResponse parse(ImapList capabilityList) {
        if (capabilityList.isEmpty() || !equalsIgnoreCase(capabilityList.get(0), Responses.CAPABILITY)) {
            return null;
        }

        int size = capabilityList.size();
        HashSet<String> capabilities = new HashSet<>(size - 1);

        for (int i = 1; i < size; i++) {
            if (!capabilityList.isString(i)) {
                return null;
            }

            String uppercaseCapability = capabilityList.getString(i).toUpperCase(Locale.US);
            capabilities.add(uppercaseCapability);
        }

        return new CapabilityResponse(capabilities);
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }
}
