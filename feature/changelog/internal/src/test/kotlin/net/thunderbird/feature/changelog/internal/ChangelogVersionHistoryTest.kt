package net.thunderbird.feature.changelog.internal

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ChangelogVersionHistoryTest {

    private val context = mock<Context>()
    private val packageManager = mock<PackageManager>()
    private val packageInfo = mock<PackageInfo>()
    private val changeLogProvider = mock<ChangelogProvider>()
    private val logger = mock<Logger>()
    private val storagePersister = mock<StoragePersister>()
    private val storageEditor = mock<StorageEditor>()
    private val storage = mock<Storage>()

    private val testCoroutineDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testCoroutineDispatcher)

    @Before
    fun setUp() {
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(context.packageName).thenReturn("net.thunderbird.android")
        whenever(storagePersister.loadValues()).thenReturn(storage)

        whenever(
            packageManager.getPackageInfo(
                "net.thunderbird.android",
                0,
            ),
        ).thenReturn(packageInfo)

        packageInfo.versionCode = CURRENT_VERSION_CODE
    }

    @Test
    fun `should return current version code`() = runTest {
        givenStoredVersions(
            firstVersionCode = FIRST_VERSION_CODE,
            lastVersionCode = LAST_VERSION_CODE,
        )

        val subject = createSubject()

        assertThat(subject.getCurrentVersionCode()).isEqualTo(CURRENT_VERSION_CODE)
    }

    @Test
    fun `should return true when current version is newer than last version`() = runTest {
        givenStoredVersions(
            firstVersionCode = FIRST_VERSION_CODE,
            lastVersionCode = LAST_VERSION_CODE,
        )

        val subject = createSubject()

        assertThat(subject.isFirstRun()).isTrue()
    }

    @Test
    fun `should return false when current version is same as last version`() = runTest {
        givenStoredVersions(
            firstVersionCode = FIRST_VERSION_CODE,
            lastVersionCode = CURRENT_VERSION_CODE,
        )

        val subject = createSubject()

        assertThat(subject.isFirstRun()).isFalse()
    }

    @Test
    fun `should return true when current version is first version`() = runTest {
        givenStoredVersions(
            firstVersionCode = CURRENT_VERSION_CODE,
            lastVersionCode = CURRENT_VERSION_CODE,
        )

        val subject = createSubject()

        assertThat(subject.isFirstRunEver()).isTrue()
    }

    @Test
    fun `should return false when current version is not first version`() = runTest {
        givenStoredVersions(
            firstVersionCode = FIRST_VERSION_CODE,
            lastVersionCode = CURRENT_VERSION_CODE,
        )

        val subject = createSubject()

        assertThat(subject.isFirstRunEver()).isFalse()
    }

    @Test
    fun `should initialize first and last version when no version history exists`() = runTest {
        givenStoredVersions(
            firstVersionCode = NO_VERSION,
            lastVersionCode = NO_VERSION,
        )

        createSubject()

        testCoroutineDispatcher.scheduler.advanceUntilIdle()

        verify(storageEditor).putInt(
            FIRST_VERSION_KEY,
            CURRENT_VERSION_CODE,
        )
        verify(storageEditor).putInt(
            LAST_VERSION_KEY,
            CURRENT_VERSION_CODE,
        )
        verify(storageEditor).commit()
    }

    @Test
    fun `should initialize first version from last version when first version is missing`() =
        runTest {
            givenStoredVersions(
                firstVersionCode = NO_VERSION,
                lastVersionCode = LAST_VERSION_CODE,
            )

            createSubject()

            testCoroutineDispatcher.scheduler.advanceUntilIdle()

            verify(storageEditor).putInt(
                FIRST_VERSION_KEY,
                LAST_VERSION_CODE,
            )
            verify(storageEditor).putInt(
                LAST_VERSION_KEY,
                LAST_VERSION_CODE,
            )
            verify(storageEditor).commit()
        }

    @Test
    fun `should not initialize version history when first version already exists`() = runTest {
        givenStoredVersions(
            firstVersionCode = FIRST_VERSION_CODE,
            lastVersionCode = LAST_VERSION_CODE,
        )

        createSubject()

        testCoroutineDispatcher.scheduler.advanceUntilIdle()

        verify(storageEditor, never()).putInt(any(), any())
        verify(storageEditor, never()).commit()
    }

    @Test
    fun `should write current version`() = runTest {
        givenStoredVersions(
            firstVersionCode = FIRST_VERSION_CODE,
            lastVersionCode = LAST_VERSION_CODE,
        )

        val subject = createSubject()

        subject.writeCurrentVersion()

        testCoroutineDispatcher.scheduler.advanceUntilIdle()

        verify(storageEditor).putInt(
            LAST_VERSION_KEY,
            CURRENT_VERSION_CODE,
        )
        verify(storageEditor).commit()

        assertThat(subject.isFirstRun()).isFalse()
    }

    @Test
    fun `should return no version when package is not found`() = runTest {
        givenStoredVersions(
            firstVersionCode = FIRST_VERSION_CODE,
            lastVersionCode = LAST_VERSION_CODE,
        )

        whenever(
            packageManager.getPackageInfo(
                "net.thunderbird.android",
                0,
            ),
        ).thenThrow(
            PackageManager.NameNotFoundException("Package not found"),
        )

        val subject = createSubject()

        assertThat(subject.getCurrentVersionCode()).isEqualTo(NO_VERSION)
    }

    private fun createSubject(): ChangelogVersionHistory {
        return ChangelogVersionHistory(
            context = context,
            changeLogProvider = changeLogProvider,
            logger = logger,
            storagePersister = storagePersister,
            storageEditor = storageEditor,
            ioDispatcher = testCoroutineDispatcher,
            scope = testScope,
        )
    }

    private fun givenStoredVersions(
        firstVersionCode: Int,
        lastVersionCode: Int,
    ) {
        whenever(
            storage.getInt(
                FIRST_VERSION_KEY,
                NO_VERSION,
            ),
        ).thenReturn(firstVersionCode)

        whenever(
            storage.getInt(
                LAST_VERSION_KEY,
                NO_VERSION,
            ),
        ).thenReturn(lastVersionCode)
    }

    private companion object {
        const val FIRST_VERSION_KEY = "ckChangeLog_first_version_code"
        const val LAST_VERSION_KEY = "ckChangeLog_last_version_code"
        const val NO_VERSION = -1

        const val FIRST_VERSION_CODE = 80
        const val LAST_VERSION_CODE = 90
        const val CURRENT_VERSION_CODE = 100
    }
}
