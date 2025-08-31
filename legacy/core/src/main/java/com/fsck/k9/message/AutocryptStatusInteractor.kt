package com.fsck.k9.message

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import androidx.core.content.IntentCompat
import java.io.InputStream
import net.thunderbird.core.logging.legacy.Log.w
import org.openintents.openpgp.OpenPgpError
import org.openintents.openpgp.util.OpenPgpApi

class AutocryptStatusInteractor {
    @WorkerThread
    fun retrieveCryptoProviderRecipientStatus(
        openPgpApi: OpenPgpApi,
        recipientAddresses: Array<String>,
    ): RecipientAutocryptStatus {
        val intent = Intent(OpenPgpApi.ACTION_QUERY_AUTOCRYPT_STATUS)
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, recipientAddresses)

        val result = openPgpApi.executeApi(intent, null as InputStream?, null)

        return when (result.getIntExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR)) {
            OpenPgpApi.RESULT_CODE_SUCCESS -> {
                val type = getRecipientAutocryptStatusFromIntent(result)
                val pendingIntent = IntentCompat.getParcelableExtra(
                    result,
                    OpenPgpApi.RESULT_INTENT,
                    PendingIntent::class.java,
                )
                RecipientAutocryptStatus(type, pendingIntent)
            }

            OpenPgpApi.RESULT_CODE_ERROR -> {
                val error = IntentCompat.getParcelableExtra(
                    result,
                    OpenPgpApi.RESULT_ERROR,
                    OpenPgpError::class.java,
                )
                if (error != null) {
                    w("OpenPGP API Error #%s: %s", error.getErrorId(), error.getMessage())
                } else {
                    w("OpenPGP API Unknown Error")
                }
                RecipientAutocryptStatus(RecipientAutocryptStatusType.ERROR, null)
            }

            OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED -> RecipientAutocryptStatus(

                RecipientAutocryptStatusType.ERROR,
                null,
            )

            else -> RecipientAutocryptStatus(RecipientAutocryptStatusType.ERROR, null)
        }
    }

    private fun getRecipientAutocryptStatusFromIntent(result: Intent): RecipientAutocryptStatusType {
        val allKeysConfirmed = result.getBooleanExtra(OpenPgpApi.RESULT_KEYS_CONFIRMED, false)
        val autocryptStatus =
            result.getIntExtra(OpenPgpApi.RESULT_AUTOCRYPT_STATUS, OpenPgpApi.AUTOCRYPT_STATUS_UNAVAILABLE)

        return when (autocryptStatus) {
            OpenPgpApi.AUTOCRYPT_STATUS_UNAVAILABLE -> {
                RecipientAutocryptStatusType.UNAVAILABLE
            }

            OpenPgpApi.AUTOCRYPT_STATUS_DISCOURAGE -> {
                if (allKeysConfirmed) {
                    RecipientAutocryptStatusType.DISCOURAGE_CONFIRMED
                } else {
                    RecipientAutocryptStatusType.DISCOURAGE_UNCONFIRMED
                }
            }

            OpenPgpApi.AUTOCRYPT_STATUS_AVAILABLE -> {
                if (allKeysConfirmed) {
                    RecipientAutocryptStatusType.AVAILABLE_CONFIRMED
                } else {
                    RecipientAutocryptStatusType.AVAILABLE_UNCONFIRMED
                }
            }

            OpenPgpApi.AUTOCRYPT_STATUS_MUTUAL -> {
                return if (allKeysConfirmed) {
                    RecipientAutocryptStatusType.RECOMMENDED_CONFIRMED
                } else {
                    RecipientAutocryptStatusType.RECOMMENDED_UNCONFIRMED
                }
            }

            else -> error("encountered bad autocrypt status number!")
        }
    }

    data class RecipientAutocryptStatus @VisibleForTesting constructor(
        val type: RecipientAutocryptStatusType,
        val intent: PendingIntent?,
    ) {
        fun hasPendingIntent() = intent != null
    }

    enum class RecipientAutocryptStatusType(
        val canEncrypt: Boolean,
        val isConfirmed: Boolean,
        val isMutual: Boolean,
    ) {
        NO_RECIPIENTS(false, false, false),
        UNAVAILABLE(false, false, false),

        DISCOURAGE_UNCONFIRMED(true, false, false),
        DISCOURAGE_CONFIRMED(true, true, false),

        AVAILABLE_UNCONFIRMED(true, false, false),
        AVAILABLE_CONFIRMED(true, true, false),

        RECOMMENDED_UNCONFIRMED(true, false, true),
        RECOMMENDED_CONFIRMED(true, true, true),

        ERROR(false, false, false),
    }

    companion object {
        val instance: AutocryptStatusInteractor = AutocryptStatusInteractor()
    }
}
