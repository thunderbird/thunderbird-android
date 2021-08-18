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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.textfield.TextInputLayout

/**
 * Configures a [TextInputLayout] so the password can only be revealed after authentication.
 */
fun TextInputLayout.configureAuthenticatedPasswordToggle(
    activity: FragmentActivity,
    title: String,
    subtitle: String,
    needScreenLockMessage: String,
) {
    val viewModel = ViewModelProvider(activity).get(AuthenticatedPasswordToggleViewModel::class.java)
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
                .build()
        )
    }

    val editText = this.editText ?: error("TextInputLayout.editText == null")

    setEndIconOnClickListener {
        if (editText.isPasswordHidden) {
            if (viewModel.isAuthenticated) {
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
    var isAuthenticated = false
    var textInputLayout: TextInputLayout? = null
    var activity: FragmentActivity? = null
        set(value) {
            field = value

            value?.lifecycle?.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun removeReferences() {
                    textInputLayout = null
                    field = null
                }
            })
        }
}
