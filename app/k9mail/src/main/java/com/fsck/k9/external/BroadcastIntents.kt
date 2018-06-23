package com.fsck.k9.external

import com.fsck.k9.BuildConfig

internal object BroadcastIntents {
    const val ACTION_EMAIL_RECEIVED = BuildConfig.APPLICATION_ID + ".intent.action.EMAIL_RECEIVED"
    const val ACTION_EMAIL_DELETED = BuildConfig.APPLICATION_ID + ".intent.action.EMAIL_DELETED"
    const val ACTION_REFRESH_OBSERVER = BuildConfig.APPLICATION_ID + ".intent.action.REFRESH_OBSERVER"
    const val EXTRA_ACCOUNT = BuildConfig.APPLICATION_ID + ".intent.extra.ACCOUNT"
    const val EXTRA_FOLDER = BuildConfig.APPLICATION_ID + ".intent.extra.FOLDER"
    const val EXTRA_SENT_DATE = BuildConfig.APPLICATION_ID + ".intent.extra.SENT_DATE"
    const val EXTRA_FROM = BuildConfig.APPLICATION_ID + ".intent.extra.FROM"
    const val EXTRA_TO = BuildConfig.APPLICATION_ID + ".intent.extra.TO"
    const val EXTRA_CC = BuildConfig.APPLICATION_ID + ".intent.extra.CC"
    const val EXTRA_BCC = BuildConfig.APPLICATION_ID + ".intent.extra.BCC"
    const val EXTRA_SUBJECT = BuildConfig.APPLICATION_ID + ".intent.extra.SUBJECT"
    const val EXTRA_FROM_SELF = BuildConfig.APPLICATION_ID + ".intent.extra.FROM_SELF"
}
