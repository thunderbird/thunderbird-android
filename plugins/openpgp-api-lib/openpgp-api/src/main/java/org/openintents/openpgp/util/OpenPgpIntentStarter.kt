package org.openintents.openpgp.util

import android.app.Activity
import android.app.ActivityOptions
import android.app.ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
import android.content.IntentSender
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * Start OpenPGP crypto provider intents.
 *
 * On Android 14+ we need to use [MODE_BACKGROUND_ACTIVITY_START_ALLOWED] in order to start those intents.
 */
object OpenPgpIntentStarter {
    @Throws(IntentSender.SendIntentException::class)
    fun startIntentSender(activity: Activity, intentSender: IntentSender) {
        val options = buildOptionsBundle()

        activity.startIntentSender(
            intentSender,
            /* fillInIntent = */
            null,
            /* flagsMask = */
            0,
            /* flagsValues = */
            0,
            /* extraFlags = */
            0,
            options,
        )
    }

    @JvmStatic
    @Throws(IntentSender.SendIntentException::class)
    fun startIntentSenderForResult(activity: Activity, intentSender: IntentSender, requestCode: Int) {
        val options = buildOptionsBundle()

        activity.startIntentSenderForResult(
            intentSender,
            requestCode,
            /* fillInIntent = */
            null,
            /* flagsMask = */
            0,
            /* flagsValues = */
            0,
            /* extraFlags = */
            0,
            options,
        )
    }

    @JvmStatic
    @Throws(IntentSender.SendIntentException::class)
    fun startIntentSenderForResult(fragment: Fragment, intentSender: IntentSender, requestCode: Int) {
        val options = buildOptionsBundle()

        @Suppress("DEPRECATION")
        fragment.startIntentSenderForResult(
            intentSender,
            requestCode,
            /* fillInIntent = */
            null,
            /* flagsMask = */
            0,
            /* flagsValues = */
            0,
            /* extraFlags = */
            0,
            options,
        )
    }

    private fun buildOptionsBundle(): Bundle? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic().apply {
                setPendingIntentBackgroundActivityStartMode(MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                setShareIdentityEnabled(true)
            }.toBundle()
        } else {
            null
        }
    }
}
