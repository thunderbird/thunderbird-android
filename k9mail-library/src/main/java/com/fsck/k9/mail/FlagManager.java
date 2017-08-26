package com.fsck.k9.mail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import timber.log.Timber;


public class FlagManager {

    private static FlagManager flagManager;

    public static synchronized FlagManager getFlagManager() {
        if (flagManager == null) {
            flagManager = new FlagManager();
        }
        return flagManager;
    }


    private static final Set<Flag> syncFlags =
        Collections.unmodifiableSet(new HashSet<Flag>(Arrays.asList(
            Flag.SEEN,
            Flag.FLAGGED,
            Flag.ANSWERED,
            Flag.FORWARDED)));

    private static final String KEYWORD_CODE_PREFIX = "KEYWORD_";

    private final ConcurrentHashMap<String, Keyword> mapExternalCodeToKeyword;
    private final CopyOnWriteArrayList<Keyword> orderedKeywords;

    private FlagManager() {
        mapExternalCodeToKeyword = new ConcurrentHashMap<String, Keyword>();
        orderedKeywords = new CopyOnWriteArrayList<Keyword>();
    }


    /*
     * Manage flags
     */

    // Get flag by code; create a new keyword, if needed.
    public Flag getFlagByCode(String code) throws IllegalArgumentException {
        Flag flag = Flag.getByCode(code);
        if (flag == null) {
            flag = getKeywordByCode(code);
        }
        return flag;
    }

    // Get server flag by external code; create a new keyword, if needed.
    public Flag getFlagByExternalCode(String externalCode)
        throws IllegalArgumentException
    {
        Flag flag = Flag.getByExternalCode(externalCode);
        if (flag == null) {
            flag = getKeywordByExternalCode(externalCode);
        }
        return flag;
    }

    public Set<Flag> getSyncFlags() {
        return syncFlags;
    }

    public List<Flag> parseCodeList(String codeList) {
        LinkedHashSet<Flag> flags = new LinkedHashSet<Flag>();
        if (codeList != null && codeList.length() > 0) {
            for (String code : codeList.split(",")) {
                try {
                    flags.add(getFlagByCode(code));
                }
                catch (Exception e) {
                    if (!"X_BAD_FLAG".equals(code)) {
                        Timber.w("Unable to parse flag %s", code);
                    }
                }
            }
        }
        return new ArrayList<Flag>(flags);
    }


    /*
     * Manage keywords
     */

    // Get keyword by code; create it, if needed.
    public Keyword getKeywordByCode(String code)
        throws IllegalArgumentException
    {
        if (code.startsWith(KEYWORD_CODE_PREFIX)) {
            final String externalCode =
                code.substring(KEYWORD_CODE_PREFIX.length());
            return getKeywordByExternalCode(externalCode);
        }
        throw new IllegalArgumentException(
            "invalid internal code '" + code + "' for a keyword: " +
            "missing prefix '" + KEYWORD_CODE_PREFIX + "'");
    }

    // Get keyword by external code; create it, if needed.
    public Keyword getKeywordByExternalCode(String externalCode)
        throws IllegalArgumentException
    {
        if (externalCode == null) {
            throw new IllegalArgumentException("externalCode is null");
        }

        if (mapExternalCodeToKeyword.containsKey(externalCode)) {
            return mapExternalCodeToKeyword.get(externalCode);
        }

        if (!Keyword.matchesImapKeywordProduction(externalCode)) {
            throw new IllegalArgumentException(
                "invalid IMAP keyword '" + externalCode + "': " +
                "does not match production");
        }
        if (Flag.isExternalCodeOfSystemFlag(externalCode)) {
            throw new IllegalArgumentException(
                "invalid IMAP keyword '" + externalCode + "': " +
                "matches system flag");
        }

        final String code = KEYWORD_CODE_PREFIX + externalCode;
        final String name = externalCode;
        final Keyword kw = new Keyword(code, externalCode, name);
        mapExternalCodeToKeyword.put(externalCode, kw);
        orderedKeywords.add(kw);
        return kw;
    }

    public void deleteKeyword(Keyword keyword) {
        final String externalCode = keyword.getExternalCode();
        if (!mapExternalCodeToKeyword.containsKey(externalCode)) {
            throw new IllegalArgumentException(
                "deletion of unknown IMAP keyword '" + externalCode + "'");
        }
        orderedKeywords.remove(keyword);
        mapExternalCodeToKeyword.remove(externalCode);
    }

    public void moveKeyword(Keyword keyword, int newPosition)
        throws IndexOutOfBoundsException
    {
        if (newPosition < 0 || newPosition >= orderedKeywords.size()) {
            throw new IndexOutOfBoundsException(
                "position " + newPosition + " is out of bounds.");
        }
        final int oldPosition = orderedKeywords.lastIndexOf(keyword);
        if (oldPosition == -1) {
            throw new RuntimeException("internal error: keyword '" +
                keyword.getCode() + "' not found in internal catalog.");
        }
        if (newPosition == oldPosition) {
            return;
        }
        orderedKeywords.remove(oldPosition);
        orderedKeywords.add(newPosition, keyword);
    }

    public ArrayList<Keyword> getKeywords() {
        return new ArrayList<Keyword>(orderedKeywords);
    }

    public ArrayList<Keyword> getVisibleKeywords() {
        ArrayList<Keyword> visibleKeywords = new ArrayList<Keyword>();
        for (Keyword keyword : orderedKeywords) {
            if (keyword.isVisible()) {
                visibleKeywords.add(keyword);
            }
        }
        return visibleKeywords;
    }

    public ArrayList<Keyword> getVisibleKeywords(Collection<Flag> flags)
    {
        HashSet<Flag> flagSet = new HashSet<Flag>(flags);
        ArrayList<Keyword> filteredKeywords = new ArrayList<Keyword>();
        for (Keyword keyword : orderedKeywords) {
            if (keyword.isVisible() && flagSet.contains(keyword)) {
                filteredKeywords.add(keyword);
            }
        }
        return filteredKeywords;
    }
}
