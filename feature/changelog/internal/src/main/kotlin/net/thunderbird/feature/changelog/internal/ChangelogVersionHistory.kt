package net.thunderbird.feature.changelog.internal

import android.content.Context
import android.content.pm.PackageManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister

private const val TAG = "ChangelogTracker"
private const val FIRST_VERSION_KEY: String = "ckChangeLog_first_version_code"
private const val LAST_VERSION_KEY: String = "ckChangeLog_last_version_code"
private const val NO_VERSION: Int = -1

class ChangelogVersionHistory(
    private val context: Context,
    private val changeLogProvider: ChangelogProvider,
    private val logger: Logger,
    private val storagePersister: StoragePersister,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
) {
    val changeLog by lazy { changeLogProvider.getChangeLog() }
    val recentChanges by lazy { changeLogProvider.getChangeLogSince(lastVersionCode) }
    private val mutex = Mutex()
    private val storage: Storage
        get() = storagePersister.loadValues()

    private var firstVersionCode = 0
    private var lastVersionCode = 0
    private var currentVersionCode = 0
    private var currentVersionName: String? = null

    init {
        firstVersionCode = storage.getInt(FIRST_VERSION_KEY, NO_VERSION)
        lastVersionCode = storage.getInt(LAST_VERSION_KEY, NO_VERSION)
        logger.debug(TAG) { "init: firstVersionCode: $firstVersionCode, lastVersionCode: $lastVersionCode" }

        try {
            val packageInfo = context.packageManager.getPackageInfo(context.getPackageName(), 0)

            currentVersionCode = packageInfo.versionCode
            currentVersionName = packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            logger.error(TAG) { "Error:${e.message}" }
            currentVersionCode = NO_VERSION
        }

        if (firstVersionCode == NO_VERSION) {
            firstVersionCode = if (lastVersionCode != NO_VERSION) lastVersionCode else currentVersionCode
            scope.launch(ioDispatcher) {
                mutex.withLock {
                    storageEditor.putInt(FIRST_VERSION_KEY, firstVersionCode)
                    storageEditor.putInt(LAST_VERSION_KEY, firstVersionCode)
                    storageEditor.commit()
                    logger.debug(TAG) {
                        "saved: firstVersionCode: $firstVersionCode, lastVersionCode: $lastVersionCode"
                    }
                }
            }
        }
    }

    fun getCurrentVersionCode(): Int {
        return currentVersionCode
    }

    fun isFirstRun(): Boolean {
        return lastVersionCode < currentVersionCode
    }

    fun isFirstRunEver(): Boolean {
        return firstVersionCode == currentVersionCode
    }

    fun writeCurrentVersion() {
        lastVersionCode = currentVersionCode

        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putInt(LAST_VERSION_KEY, currentVersionCode)
                storageEditor.commit()
                logger.debug(TAG) { "saved: currentVersionCode: $currentVersionCode" }
            }
        }
    }
}
