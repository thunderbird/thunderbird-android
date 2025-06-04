package com.fsck.k9.helper

import java.io.File
import java.io.IOException
import net.thunderbird.core.logging.legacy.Log

object FileHelper {

    @JvmStatic
    @Suppress("TooGenericExceptionCaught")
    fun touchFile(parentDir: File, name: String) {
        val file = File(parentDir, name)
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    Log.d("Unable to create file: %s", file.absolutePath)
                }
            } else {
                if (!file.setLastModified(System.currentTimeMillis())) {
                    Log.d("Unable to change last modification date: %s", file.absolutePath)
                }
            }
        } catch (e: Exception) {
            Log.d(e, "Unable to touch file: %s", file.absolutePath)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun renameOrMoveByCopying(from: File, to: File) {
        deleteFileIfExists(to)
        val renameFailed = !from.renameTo(to)
        if (renameFailed) {
            from.copyTo(target = to, overwrite = true)
            val deleteFromFailed = !from.delete()
            if (deleteFromFailed) {
                Log.e("Unable to delete source file after copying to destination!")
            }
        }
    }

    @Throws(IOException::class)
    private fun deleteFileIfExists(file: File) {
        if (file.exists() && !file.delete()) {
            throw IOException("Unable to delete file: ${file.absolutePath}")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun move(from: File, to: File): Boolean {
        if (to.exists()) {
            if (!to.delete()) {
                Log.d("Unable to delete file: %s", to.absolutePath)
            }
        }

        val parent = to.parentFile
        if (parent != null && !parent.mkdirs()) {
            Log.d("Unable to make directories: %s", parent.absolutePath)
        }
        return try {
            from.copyTo(target = to, overwrite = true)
            val deleteFromFailed = !from.delete()
            if (deleteFromFailed) {
                Log.e("Unable to delete source file after copying to destination!")
            }
            true
        } catch (e: Exception) {
            Log.w(e, "cannot move %s to %s", from.absolutePath, to.absolutePath)
            false
        }
    }
}
