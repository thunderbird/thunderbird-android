package net.thunderbird.feature.applock.impl.domain

import androidx.fragment.app.FragmentActivity
import net.thunderbird.feature.applock.api.AppLockAuthenticator
import net.thunderbird.feature.applock.api.AppLockAuthenticatorFactory
import net.thunderbird.feature.applock.impl.R

internal class BiometricAuthenticatorFactory : AppLockAuthenticatorFactory {
    override fun create(activity: FragmentActivity): AppLockAuthenticator {
        return BiometricAuthenticator(
            activity = activity,
            title = activity.getString(R.string.applock_prompt_title),
            subtitle = activity.getString(R.string.applock_prompt_subtitle),
        )
    }
}
