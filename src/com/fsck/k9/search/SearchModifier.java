package com.fsck.k9.search;

import com.fsck.k9.R;
import com.fsck.k9.mail.Flag;

/**
 * This enum represents filtering parameters used by {@link com.fsck.k9.search.SearchAccount}.
 */
public enum SearchModifier {
    FLAGGED(R.string.flagged_modifier, new Flag[] { Flag.FLAGGED }, null),
    UNREAD(R.string.unread_modifier, null, new Flag[] { Flag.SEEN });

    public final int resId;
    public final Flag[] requiredFlags;
    public final Flag[] forbiddenFlags;

    SearchModifier(int nResId, Flag[] nRequiredFlags, Flag[] nForbiddenFlags) {
        resId = nResId;
        requiredFlags = nRequiredFlags;
        forbiddenFlags = nForbiddenFlags;
    }

}