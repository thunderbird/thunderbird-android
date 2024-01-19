package com.fsck.k9.helper

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import org.apache.commons.io.IOUtils
import timber.log.Timber

object FileHelper {
    @JvmStatic
    fun touchFile(parentDir: File?, name: String?) {
        val file = File(parentDir, name)
        try {
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    Timber.d("Unable to create file: %s", file.absolutePath)
                }
            } else {
                if (!file.setLastModified(System.currentTimeMillis())) {
                    Timber.d("Unable to change last modification date: %s", file.absolutePath)
                }
            }
        } catch (e: Exception) {
            Timber.d(e, "Unable to touch file: %s", file.absolutePath)
        }
    }

    @Throws(IOException::class)
    private fun copyFile(from: File, to: File) {
        val `in` = FileInputStream(from)
        val out = FileOutputStream(to)
        try {
            val buffer = ByteArray(1024)
            var count: Int
            while (`in`.read(buffer).also { count = it } > 0) {
                out.write(buffer, 0, count)
            }
            out.close()
        } finally {
            IOUtils.closeQuietly(`in`)
            IOUtils.closeQuietly(out)
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun renameOrMoveByCopying(from: File, to: File) {
        deleteFileIfExists(to)
        val renameFailed = !from.renameTo(to)
        if (renameFailed) {
            copyFile(from, to)
            val deleteFromFailed = !from.delete()
            if (deleteFromFailed) {
                Timber.e("Unable to delete source file after copying to destination!")
            }
        }
    }

    @Throws(IOException::class)
    private fun deleteFileIfExists(to: File) {
        val fileDoesNotExist = !to.exists()
        if (fileDoesNotExist) {
            return
        }
        val deleteOk = to.delete()
        if (deleteOk) {
            return
        }
        throw IOException("Unable to delete file: " + to.absolutePath)
    }

    fun move(from: File, to: File): Boolean {
        if (to.exists()) {
            if (!to.delete()) {
                Timber.d("Unable to delete file: %s", to.absolutePath)
            }
        }
        if (!to.getParentFile().mkdirs()) {
            Timber.d("Unable to make directories: %s", to.getParentFile().absolutePath)
        }
        return try {
            copyFile(from, to)
            val deleteFromFailed = !from.delete()
            if (deleteFromFailed) {
                Timber.e("Unable to delete source file after copying to destination!")
            }
            true
        } catch (e: Exception) {
            Timber.w(e, "cannot move %s to %s", from.absolutePath, to.absolutePath)
            false
        }
    }

    @JvmStatic
    fun moveRecursive(fromDir: File, toDir: File) {
        if (!fromDir.exists()) {
            return
        }
        if (!fromDir.isDirectory()) {
            if (toDir.exists()) {
                if (!toDir.delete()) {
                    Timber.w("cannot delete already existing file/directory %s", toDir.absolutePath)
                }
            }
            if (!fromDir.renameTo(toDir)) {
                Timber.w("cannot rename %s to %s - moving instead", fromDir.absolutePath, toDir.absolutePath)
                move(fromDir, toDir)
            }
            return
        }
        if (!toDir.exists() || !toDir.isDirectory()) {
            if (toDir.exists()) {
                if (!toDir.delete()) {
                    Timber.d("Unable to delete file: %s", toDir.absolutePath)
                }
            }
            if (!toDir.mkdirs()) {
                Timber.w("cannot create directory %s", toDir.absolutePath)
            }
        }
        val files = fromDir.listFiles()
        for (file in files) {
            if (file.isDirectory()) {
                moveRecursive(file, File(toDir, file.getName()))
                if (!file.delete()) {
                    Timber.d("Unable to delete file: %s", toDir.absolutePath)
                }
            } else {
                val target = File(toDir, file.getName())
                if (!file.renameTo(target)) {
                    Timber.w(
                        "cannot rename %s to %s - moving instead",
                        file.absolutePath, target.absolutePath,
                    )
                    move(file, target)
                }
            }
        }
        if (!fromDir.delete()) {
            Timber.w("cannot delete %s", fromDir.absolutePath)
        }
    }
}
