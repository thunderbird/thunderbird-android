package com.fsck.k9.mail.store.imap;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.fsck.k9.mail.Flag;
import com.fsck.k9.mail.filter.PeekableInputStream;

import static com.fsck.k9.mail.Folder.OPEN_MODE_RW;
import static java.util.Arrays.asList;


public class ImapResponseHelper {

    @SafeVarargs
    public static List<List<ImapResponse>> createMultipleImapResponses(List<String>... responses) throws IOException {
        List<List<ImapResponse>> imapResponses = new ArrayList<>();
        for (List<String> response : responses) {
            imapResponses.add(createImapResponseList(response));
        }
        return imapResponses;
    }

    private static List<ImapResponse> createImapResponseList(List<String> responses) throws IOException {
        List<ImapResponse> imapResponses = new ArrayList<>();
        for (String response : responses) {
            imapResponses.add(createImapResponse(response));
        }
        return imapResponses;
    }

    public static ImapResponse createImapResponse(String response) throws IOException {
        String input = response + "\r\n";
        PeekableInputStream inputStream = new PeekableInputStream(new ByteArrayInputStream(input.getBytes()));
        ImapResponseParser parser = new ImapResponseParser(inputStream);

        return parser.readResponse();
    }

    public static List<Long> createNonContiguousIdSet(long start, long end, int interval) {
        List<Long> ids = new ArrayList<>();
        for (long i = start;i <= end;i += interval) {
            ids.add(i);
        }
        return ids;
    }

    static List<ImapResponse> createFolderOpenedResponse(int openMode, Long uidValidity, Long highestModSeq,
            List<String> vanishedUids, Map<String, Set<Flag>> fetchResponses) throws IOException {
        List<ImapResponse> openResponses = new ArrayList<>(asList(
                createImapResponse("* FLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft NonJunk $MDNSent)"),
                createImapResponse("* OK [PERMANENTFLAGS (\\Answered \\Flagged \\Deleted \\Seen \\Draft NonJunk " +
                        "$MDNSent \\*)] Flags permitted."),
                createImapResponse("* 23 EXISTS"),
                createImapResponse("* 0 RECENT"),
                createImapResponse(String.format(Locale.US, "* OK [UIDVALIDITY %d] UIDs valid", uidValidity)),
                createImapResponse("* OK [UIDNEXT 57576] Predicted next UID")
        ));

        String highestModSeqResponse;
        if (highestModSeq == null) {
            highestModSeqResponse = "* OK [NOMODSEQ] Sorry, this mailbox format doesn't support modsequences";
        } else {
            highestModSeqResponse = String.format(Locale.US,
                    "* OK [HIGHESTMODSEQ %d] Highest mailbox mod-sequence", highestModSeq);
        }
        openResponses.add(createImapResponse(highestModSeqResponse));

        if (vanishedUids != null || fetchResponses != null) {
            if (vanishedUids != null) {
                String vanishedUidsString = ImapUtility.join(",", vanishedUids);
                openResponses.add(createImapResponse(String.format("* VANISHED (EARLIER) %s", vanishedUidsString)));
            }

            if (highestModSeq != null && fetchResponses != null) {
                int seqNum = 30;
                for (Map.Entry<String, Set<Flag>> fetchResponse : fetchResponses.entrySet()) {
                    String fetchResponseString = String.format(Locale.US, "* %d FETCH (UID %s FLAGS (%s) MODSEQ (%d))",
                            ++seqNum, fetchResponse.getKey(), ImapUtility.combineFlags(fetchResponse.getValue(), false),
                            highestModSeq--);
                    openResponses.add(createImapResponse(fetchResponseString));
                }
            }
        }

        ImapResponse finishedResponse = (openMode == OPEN_MODE_RW) ?
                createImapResponse("2 OK [READ-WRITE] Select completed.") :
                createImapResponse("2 OK [READ-ONLY] Examine completed.");
        openResponses.add(finishedResponse);
        return openResponses;
    }
}
