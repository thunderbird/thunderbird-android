package com.fsck.k9.mail.store.imap.response;


import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fsck.k9.mail.store.imap.ImapList;
import com.fsck.k9.mail.store.imap.ImapResponse;
import com.fsck.k9.mail.store.imap.Responses;
import com.fsck.k9.mail.store.imap.command.ImapCommandFactory;

import static com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase;


public class CapabilityResponse extends BaseResponse {

    private final Set<String> capabilities;

    private CapabilityResponse(ImapCommandFactory commandFactory, List<ImapResponse> imapResponses, Set<String> capabilities) {
        super(commandFactory, imapResponses);
        this.capabilities = capabilities;
    }

    public static CapabilityResponse parse(ImapCommandFactory commandFactory, List<ImapResponse> responses) {
        for (ImapResponse response : responses) {
            CapabilityResponse result;
            if (!response.isEmpty() && equalsIgnoreCase(response.get(0), Responses.OK) && response.isList(1)) {
                ImapList capabilityList = response.getList(1);
                result = parse(commandFactory, capabilityList);
            } else if (response.getTag() == null) {
                result = parse(commandFactory, response);
            } else {
                result = null;
            }

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    private static CapabilityResponse parse(ImapCommandFactory commandFactory, ImapList capabilityList) {
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

        return new CapabilityResponse(commandFactory, null, capabilities);
    }

    @Override
    public void parseResponse(List<ImapResponse> imapResponses) {
        //This is never called
    }

    @Override
    public void combine(BaseResponse baseResponse) {
        super.combine(baseResponse);
        //This is never called
    }

    @Override
    void handleUntaggedResponses(List<ImapResponse> responses) {
        super.handleUntaggedResponses(responses);
    }

    public Set<String> getCapabilities() {
        return capabilities;
    }
}
