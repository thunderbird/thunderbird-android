package net.discdd.k9.onboarding.util

import android.content.Context
import android.util.Log
import net.discdd.k9.onboarding.repository.AuthRepository.AuthState
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AuthStateConfig(
    private val context: Context,
    private val dddDir: File = context.filesDir.resolve("ddd"),
    private val configFile: File = dddDir.resolve("auth.state")
) {
    private fun createConfig() {
        if (!dddDir.exists()) {
            dddDir.mkdirs()
        }
        configFile.createNewFile()
    }

    fun writeState(state: AuthState) {
        createConfig()
        try {
            var os = FileOutputStream(configFile)
            os.write(state.name.toByteArray())
            os.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    fun readState(): AuthState {
        if (!configFile.exists()) return AuthState.LOGGED_OUT
        var state: String? = configFile.readLines().firstOrNull()

        when (state) {
            "PENDING" -> return AuthState.PENDING
            "LOGGED_IN" -> return AuthState.LOGGED_IN
        }

        return AuthState.LOGGED_OUT
    }

    fun deleteState() {
        if (configFile.delete()) {
            Log.d("DDDOnboarding", "Deleted auth state config file successfully")
        }
        Log.d("DDDOnboarding","auth state config file failed to delete")
    }
}
