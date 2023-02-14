@file:JvmName("TextInputLayoutHelper")

package com.fsck.k9.ui.base.extensions

import android.annotation.SuppressLint
import android.text.method.PasswordTransformationMethod
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import android.widget.EditText
import android.widget.Toast
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.get
import com.google.android.material.textfield.TextInputLayout

/**
 * Configures a [TextInputLayout] so the password can only be revealed after authentication.
 *
 * **IMPORTANT**: Only call this after the instance state has been restored! Otherwise, restoring the previous state
 * after the initial state has been set will be detected as replacing the whole text. In that case showing the password
 * will be allowed without authentication.
 */
fun TextInputLayout.configureAuthenticatedPasswordToggle(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    needScreenLockMessage: String,
) {
    val viewModel = ViewModelProvider(activity).get<AuthenticatedPasswordToggleViewModel>()
    viewModel.textInputLayout = this
    viewModel.activity = activity

    fun authenticateUserAndShowPassword(activity: FragmentActivity) {
        val mainExecutor = ContextCompat.getMainExecutor(activity)

        val context = activity.applicationContext
        val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                // The Activity might have been recreated since this callback object was created (e.g. due to an
                // orientation change). So we fetch the (new) references from the ViewModel.
                viewModel.isAuthenticated = true
                viewModel.activity?.setSecure(true)
                viewModel.textInputLayout?.editText?.showPassword()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT ||
                    errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL ||
                    errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS
                ) {
                    Toast.makeText(context, needScreenLockMessage, Toast.LENGTH_SHORT).show()
                } else if (errString.isNotEmpty()) {
                    Toast.makeText(context, errString, Toast.LENGTH_SHORT).show()
                }
            }
        }

        BiometricPrompt(activity, mainExecutor, authenticationCallback).authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
                .setTitle(title)
                .setSubtitle(subtitle)
                .build(),
        )
    }

    val editText = this.editText ?: error("TextInputLayout.editText == null")

    editText.doOnTextChanged { text, _, before, count ->
        // Check if the password field is empty or if all of the previous text was replaced
        if (text != null && before > 0 && (text.isEmpty() || text.length - count == 0)) {
            viewModel.isNewPassword = true
        }
    }

    setEndIconOnClickListener {
        if (editText.isPasswordHidden) {
            if (viewModel.isShowPasswordAllowed) {
                activity.setSecure(true)
                editText.showPassword()
            } else {
                authenticateUserAndShowPassword(activity)
            }
        } else {
            viewModel.isAuthenticated = false
            editText.hidePassword()
            activity.setSecure(false)
        }
    }
}

private val EditText.isPasswordHidden: Boolean
    get() = transformationMethod is PasswordTransformationMethod

private fun EditText.showPassword() {
    transformationMethod = null
}

private fun EditText.hidePassword() {
    transformationMethod = PasswordTransformationMethod.getInstance()
}

private fun FragmentActivity.setSecure(secure: Boolean) {
    window.setFlags(if (secure) FLAG_SECURE else 0, FLAG_SECURE)
}

@SuppressLint("StaticFieldLeak")
class AuthenticatedPasswordToggleViewModel : ViewModel() {
    val isShowPasswordAllowed: Boolean
        get() = isAuthenticated || isNewPassword

    var isNewPassword = false
    var isAuthenticated = false
    var textInputLayout: TextInputLayout? = null
    var activity: FragmentActivity? = null
        set(value) {
            field = value

            value?.lifecycle?.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onDestroy(owner: LifecycleOwner) {
                        textInputLayout = null
                        field = null
                    }
                },
            )
        }
}
