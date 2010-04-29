/**
 *
 */
package com.fsck.k9.activity;

import com.fsck.k9.R;
import com.fsck.k9.mail.Flag;

enum SearchModifier
{
    FLAGGED(R.string.flagged_modifier, new Flag[] { Flag.FLAGGED}, null), UNREAD(R.string.unread_modifier, null, new Flag[] { Flag.SEEN});

    final int resId;
    final Flag[] requiredFlags;
    final Flag[] forbiddenFlags;

    SearchModifier(int nResId, Flag[] nRequiredFlags, Flag[] nForbiddenFlags)
    {
        resId = nResId;
        requiredFlags = nRequiredFlags;
        forbiddenFlags = nForbiddenFlags;
    }

}