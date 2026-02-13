package net.thunderbird.feature.applock.impl.ui

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.provider.Settings
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.thunderbird.core.outcome.Outcome
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.applock.api.AppLockAuthenticatorFactory
import net.thunderbird.feature.applock.api.AppLockCoordinator
import net.thunderbird.feature.applock.api.AppLockError
import net.thunderbird.feature.applock.api.AppLockGate
import net.thunderbird.feature.applock.api.AppLockState
import net.thunderbird.feature.applock.api.UnavailableReason
import net.thunderbird.feature.applock.api.isUnlocked
import net.thunderbird.feature.applock.impl.R

private const val OVERLAY_TAG_PLAIN = "applock_overlay_plain"
private const val OVERLAY_TAG_CONTENT = "applock_overlay_content"

/**
 * Default implementation of [AppLockGate] that handles lock overlay and biometric authentication.
 *
 * This class observes the app lock coordinator state and:
 * - Shows/hides a lock overlay based on lock state
 * - Triggers biometric authentication when the activity resumes in a locked state
 * - Handles authentication results (success finishes normally, cancel closes app)
 */
@Suppress("TooManyFunctions")
internal class DefaultAppLockGate(
    private val activity: FragmentActivity,
    private val coordinator: AppLockCoordinator,
    private val authenticatorFactory: AppLockAuthenticatorFactory,
    private val themeProvider: FeatureThemeProvider,
) : AppLockGate {

    private sealed interface ContentOverlayState {
        data class Failed(val error: AppLockError) : ContentOverlayState
        data class Unavailable(val reason: UnavailableReason) : ContentOverlayState
    }

    private var lockOverlay: View? = null
    private var currentContentOverlayState: ContentOverlayState? = null
    private var lastAttemptId: Long? = null
    private var stateObserverJob: Job? = null
    private var authenticationJob: Job? = null
    private var isResumed: Boolean = false

    override fun onStart(owner: LifecycleOwner) {
        // Start observing state changes to update overlay and trigger auth if needed
        stateObserverJob = activity.lifecycleScope.launch {
            coordinator.state.collect { state ->
                // Update overlay based on state
                when {
                    state.isUnlocked() -> hideLockOverlay()
                    state is AppLockState.Failed -> showFailedOverlay(state.error)
                    state is AppLockState.Unavailable -> showUnavailableOverlay(state.reason)
                    else -> showLockOverlay()
                }

                // Trigger authentication if activity is resumed and we're in a locked state
                if (isResumed) {
                    triggerAuthenticationIfNeeded()
                }
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        isResumed = true

        // Refresh availability in case user set up authentication in Settings
        if (coordinator.state.value is AppLockState.Unavailable) {
            coordinator.refreshAvailability()
        }

        triggerAuthenticationIfNeeded()

        // Hide privacy overlay if still unlocked (for quick pause/resume)
        // Done after triggerAuthenticationIfNeeded to avoid ordering flicker
        if (coordinator.state.value.isUnlocked()) {
            hideLockOverlay()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        isResumed = false

        // Show privacy overlay to obscure content in task switcher.
        // Skip if a content overlay (Failed/Unavailable) is already visible — those overlays
        // already hide app content, and replacing them with the plain overlay would discard the
        // actionable UI. StateFlow won't re-emit the same state on resume, so the content
        // overlay would never be restored, leaving users stuck behind a non-interactive screen.
        if (coordinator.config.isEnabled && lockOverlay?.tag != OVERLAY_TAG_CONTENT) {
            showLockOverlay()
        }

        // Do NOT cancel authenticationJob here or in onStop. BiometricPrompt with device
        // credentials (PIN/pattern/password) launches a system activity that causes both onPause
        // and onStop on the host activity. Cancelling the auth would discard the result when the
        // user successfully authenticates on that screen. The auth job is scoped to lifecycleScope
        // and is cleaned up automatically on DESTROYED.
    }

    override fun onStop(owner: LifecycleOwner) {
        stateObserverJob?.cancel()
        stateObserverJob = null
        lastAttemptId = null // Allow relaunch on next start
    }

    override fun onDestroy(owner: LifecycleOwner) {
        hideLockOverlay()
        lastAttemptId = null
    }

    private fun triggerAuthenticationIfNeeded() {
        when (val state = coordinator.state.value) {
            is AppLockState.Unlocking -> {
                val attemptId = state.attemptId
                if (attemptId != lastAttemptId) {
                    lastAttemptId = attemptId
                    launchAuthentication()
                }
            }
            AppLockState.Locked -> {
                // Request unlock - coordinator will transition to Unlocking
                if (coordinator.ensureUnlocked()) {
                    val newState = coordinator.state.value
                    if (newState is AppLockState.Unlocking) {
                        lastAttemptId = newState.attemptId
                        launchAuthentication()
                    }
                }
            }
            is AppLockState.Failed -> {
                // Don't auto-retry on failure to prevent infinite prompt loop.
                // User can close app and reopen to retry. Overlay remains visible.
            }
            is AppLockState.Unavailable -> {
                // Auth unavailable - show guidance overlay, no auth to trigger
            }
            AppLockState.Disabled, is AppLockState.Unlocked -> {
                // Nothing to do
            }
        }
    }

    private fun launchAuthentication() {
        // Don't launch if already in progress
        if (authenticationJob?.isActive == true) return

        // Don't launch if another activity already started authentication
        if (coordinator.state.value !is AppLockState.Unlocking) return

        val authenticator = authenticatorFactory.create(activity)

        authenticationJob = activity.lifecycleScope.launch {
            try {
                val result = coordinator.authenticate(authenticator)
                // UnableToStart is expected in multi-window when another activity is already
                // authenticating. Clear lastAttemptId so the state observer can retry when
                // the other activity's auth completes and the coordinator state changes.
                if (result is Outcome.Failure &&
                    result.error is AppLockError.UnableToStart &&
                    coordinator.state.value is AppLockState.Unlocking
                ) {
                    lastAttemptId = null
                }
            } finally {
                authenticationJob = null
            }
        }
    }

    private fun showLockOverlay() {
        // Already showing plain lock overlay
        if (lockOverlay?.tag == OVERLAY_TAG_PLAIN) {
            currentContentOverlayState = null
            return
        }

        // Remove any existing overlay (e.g., failed overlay)
        hideLockOverlay()

        val contentView = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

        // Use a plain View instead of ComposeView for synchronous rendering.
        // This minimizes the timing gap where the task switcher could capture
        // actual content before the overlay renders.
        val overlay = View(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            tag = OVERLAY_TAG_PLAIN
            isFocusable = true
            isClickable = true
            setBackgroundColor(resolveWindowBackgroundColor())
        }

        contentView.addView(overlay)
        lockOverlay = overlay
        currentContentOverlayState = null
    }

    private fun resolveWindowBackgroundColor(): Int {
        val typedValue = TypedValue()

        val isWindowBackgroundColor =
            activity.theme.resolveAttribute(android.R.attr.windowBackground, typedValue, true) &&
                typedValue.type >= TypedValue.TYPE_FIRST_COLOR_INT &&
                typedValue.type <= TypedValue.TYPE_LAST_COLOR_INT

        val isColorBackground = !isWindowBackgroundColor &&
            activity.theme.resolveAttribute(android.R.attr.colorBackground, typedValue, true)

        return when {
            isWindowBackgroundColor || isColorBackground -> typedValue.data
            else -> Color.BLACK
        }
    }

    private fun showFailedOverlay(error: AppLockError) {
        showContentOverlay(ContentOverlayState.Failed(error)) {
            AppLockFailedOverlay(
                errorMessage = getErrorMessage(error),
                onRetryClick = ::onRetryClicked,
                onCloseClick = { activity.finishAffinity() },
            )
        }
    }

    private fun showUnavailableOverlay(reason: UnavailableReason) {
        val actionButtonText = when (reason) {
            UnavailableReason.NOT_ENROLLED -> activity.getString(R.string.applock_button_open_settings)
            UnavailableReason.TEMPORARILY_UNAVAILABLE,
            UnavailableReason.UNKNOWN,
            -> activity.getString(R.string.applock_button_try_again)
            UnavailableReason.NO_HARDWARE -> null
        }

        val onActionClick: (() -> Unit)? = when (reason) {
            UnavailableReason.NOT_ENROLLED -> ::openSecuritySettings
            UnavailableReason.TEMPORARILY_UNAVAILABLE,
            UnavailableReason.UNKNOWN,
            -> ::onUnavailableRetryClicked
            UnavailableReason.NO_HARDWARE -> null
        }

        showContentOverlay(ContentOverlayState.Unavailable(reason)) {
            AppLockUnavailableOverlay(
                hintMessage = getUnavailableHint(reason),
                actionButtonText = actionButtonText,
                onActionClick = onActionClick,
                onCloseClick = { activity.finishAffinity() },
            )
        }
    }

    private fun showContentOverlay(state: ContentOverlayState, content: @Composable () -> Unit) {
        // Avoid removing and re-adding when showing the exact same content.
        if (lockOverlay?.tag == OVERLAY_TAG_CONTENT && currentContentOverlayState == state) return

        hideLockOverlay()

        val contentView = activity.findViewById<ViewGroup>(android.R.id.content) ?: return

        val overlay = ComposeView(activity).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            tag = OVERLAY_TAG_CONTENT
            isFocusable = true
            isClickable = true
            setContent {
                BackHandler { activity.finishAffinity() }
                themeProvider.WithTheme {
                    content()
                }
            }
        }

        contentView.addView(overlay)
        lockOverlay = overlay
        currentContentOverlayState = state
    }

    private fun getUnavailableHint(reason: UnavailableReason): String {
        return when (reason) {
            UnavailableReason.NO_HARDWARE -> activity.getString(R.string.applock_error_not_available)
            UnavailableReason.NOT_ENROLLED -> activity.getString(R.string.applock_requirements_hint)
            UnavailableReason.TEMPORARILY_UNAVAILABLE -> {
                activity.getString(R.string.applock_error_temporarily_unavailable)
            }
            UnavailableReason.UNKNOWN -> activity.getString(R.string.applock_error_unknown_unavailable)
        }
    }

    private fun openSecuritySettings() {
        listOf(
            Intent(Settings.ACTION_SECURITY_SETTINGS),
            Intent(Settings.ACTION_SETTINGS),
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", activity.packageName, null)),
        ).firstOrNull { it.resolveActivity(activity.packageManager) != null }
            ?.let { activity.startActivity(it) }
    }

    private fun onRetryClicked() {
        // Just request unlock - the state collector will observe the transition
        // to Unlocking and trigger authentication via triggerAuthenticationIfNeeded()
        coordinator.ensureUnlocked()
    }

    private fun onUnavailableRetryClicked() {
        coordinator.refreshAvailability()
        if (coordinator.state.value == AppLockState.Locked) {
            coordinator.ensureUnlocked()
        }
    }

    private fun getErrorMessage(error: AppLockError): String {
        return when (error) {
            is AppLockError.NotAvailable -> activity.getString(R.string.applock_error_not_available)
            is AppLockError.NotEnrolled -> activity.getString(R.string.applock_error_not_enrolled)
            is AppLockError.Failed -> activity.getString(R.string.applock_error_failed)
            is AppLockError.Canceled -> activity.getString(R.string.applock_error_canceled)
            is AppLockError.Interrupted -> activity.getString(R.string.applock_error_failed)
            is AppLockError.Lockout -> {
                when {
                    error.durationSeconds == AppLockError.Lockout.DURATION_PERMANENT -> {
                        activity.getString(R.string.applock_error_lockout_permanent)
                    }
                    error.durationSeconds > 0 -> {
                        // Temporary lockout with known duration
                        activity.resources.getQuantityString(
                            R.plurals.applock_error_lockout,
                            error.durationSeconds,
                            error.durationSeconds,
                        )
                    }
                    else -> {
                        // Temporary lockout with unknown duration
                        activity.getString(R.string.applock_error_lockout_unknown)
                    }
                }
            }
            is AppLockError.UnableToStart -> {
                activity.getString(R.string.applock_error_unable_to_start, error.message)
            }
        }
    }

    private fun hideLockOverlay() {
        lockOverlay?.let { overlay ->
            (overlay.parent as? ViewGroup)?.removeView(overlay)
            lockOverlay = null
        }
        currentContentOverlayState = null
    }
}

/**
 * Factory for creating [DefaultAppLockGate] instances.
 */
internal class DefaultAppLockGateFactory(
    private val coordinator: AppLockCoordinator,
    private val authenticatorFactory: AppLockAuthenticatorFactory,
    private val themeProvider: FeatureThemeProvider,
) : AppLockGate.Factory {
    override fun create(activity: FragmentActivity): AppLockGate {
        return DefaultAppLockGate(
            activity = activity,
            coordinator = coordinator,
            authenticatorFactory = authenticatorFactory,
            themeProvider = themeProvider,
        )
    }
}
