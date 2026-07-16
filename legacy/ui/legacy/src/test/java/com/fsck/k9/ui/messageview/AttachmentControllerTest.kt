package com.fsck.k9.ui.messageview

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ProviderInfo
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import app.k9mail.legacy.message.controller.MessagingListener
import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.fsck.k9.mail.Part
import com.fsck.k9.mailstore.AttachmentViewInfo
import com.fsck.k9.mailstore.LocalBodyPart
import com.fsck.k9.mailstore.LocalPart
import com.fsck.k9.provider.AttachmentTempFileProvider
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.android.account.LegacyAccountDto
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.common.appConfig.PlatformConfigProvider
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.preference.GeneralSettings
import net.thunderbird.core.preference.GeneralSettingsManager
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Before
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

class AttachmentControllerTest : RobolectricTest() {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val logger = TestLogger()

    private val displayName = "document.pdf"
    private val mimeType = "application/pdf"
    private val part = LocalBodyPart(
        "00000000-0000-4000-0000-000000000000",
        null,
        1L,
        42L,
    )

    @Before
    fun setUp() {
        Log.logger = logger
        startKoin {
            modules(
                module {
                    single<GeneralSettingsManager> { FakeGeneralSettingsManager() }
                },
            )
        }
        initializeAttachmentTempFileProvider()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `attachment content is set available if download is success`() = runTest {
        // Arrange
        val fakeAttachment = AttachmentViewInfo(
            mimeType = mimeType,
            displayName = displayName,
            size = 42L,
            internalUri = "content://internal/path".toUri(),
            inlineAttachment = false,
            part = part,
            isContentAvailable = false,
        )

        val fakeAttachmentController = AttachmentController(
            context = context,
            controller = FakeAttachmentLoadingController(),
            attachmentDisplayController = FakeAttachmentDisplayController(),
            attachment = fakeAttachment,
            ioDispatcher = UnconfinedTestDispatcher(),
            logger = logger,
        )
        val fileContent = "This is test data".toByteArray()
        val inputStream = ByteArrayInputStream(fileContent)
        val contentResolverShadow = shadowOf(context.contentResolver)

        contentResolverShadow.registerInputStream("content://internal/path".toUri(), inputStream)

        // Act
        fakeAttachmentController.viewAttachment(this)
        advanceUntilIdle()

        // Assert
        assertThat(fakeAttachment.isContentAvailable).isTrue()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `saveAttachmentTo should write internal URI data into document URI`() = runTest {
        // Arrange
        val internalUri = "content://internal/path".toUri()
        val documentUri = "content://document/path".toUri()

        val thisAttachment = AttachmentViewInfo(
            mimeType = mimeType,
            displayName = displayName,
            size = 42L,
            internalUri = internalUri,
            inlineAttachment = false,
            part = part,
            isContentAvailable = true,
        )

        val controller = AttachmentController(
            context = context,
            controller = FakeAttachmentLoadingController(),
            attachmentDisplayController = FakeAttachmentDisplayController(),
            attachment = thisAttachment,
            ioDispatcher = UnconfinedTestDispatcher(),
            logger = logger,
        )

        val fileContent = "This is test data".toByteArray()
        val inputStream = ByteArrayInputStream(fileContent)
        val outputStream = ByteArrayOutputStream()

        val contentResolverShadow = shadowOf(context.contentResolver)
        contentResolverShadow.registerInputStream(internalUri, inputStream)
        contentResolverShadow.registerOutputStream(documentUri, outputStream)

        // Act
        controller.saveAttachmentTo(this, documentUri)
        advanceUntilIdle()

        // Assert
        val writtenBytes = outputStream.toByteArray()
        assertArrayEquals(fileContent, writtenBytes)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `viewing attachment should an appropriate activity with ACTION intent`() = runTest {
        // Arrange
        val application = ApplicationProvider.getApplicationContext<Application>()
        val shadowApplication = shadowOf(application)
        val contentResolverShadow = shadowOf(context.contentResolver)
        val internalUri = "content://internal/path".toUri()

        val thisAttachment = AttachmentViewInfo(
            mimeType = mimeType,
            displayName = displayName,
            size = 42L,
            internalUri = internalUri,
            inlineAttachment = false,
            part = part,
            isContentAvailable = true,
        )

        val controller = AttachmentController(
            context = context,
            controller = FakeAttachmentLoadingController(),
            attachmentDisplayController = FakeAttachmentDisplayController(),
            attachment = thisAttachment,
            ioDispatcher = UnconfinedTestDispatcher(),
            logger = logger,
        )

        val fileContent = "This is test data".toByteArray()
        val inputStream = ByteArrayInputStream(fileContent)
        contentResolverShadow.registerInputStream(internalUri, inputStream)

        // Act
        controller.viewAttachment(this)
        advanceUntilIdle()

        // Assert
        val intent = shadowApplication.nextStartedActivity
        assertThat(intent).isNotNull()
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals("application/octet-stream", intent.type)
    }

    private fun initializeAttachmentTempFileProvider() {
        val attachmentTempFileProviderAuthority = "${context.packageName}.tempfileprovider"

        val info = ProviderInfo().apply {
            authority = attachmentTempFileProviderAuthority
            grantUriPermissions = true
        }
        Robolectric.buildContentProvider(AttachmentTempFileProvider::class.java).create(info)
    }

    class FakeAttachmentDisplayController : AttachmentDisplayController {
        override fun showAttachmentLoadingDialog() = Unit

        override fun hideAttachmentLoadingDialogOnMainThread() = Unit

        override fun refreshAttachmentThumbnail(attachment: AttachmentViewInfo) = Unit
    }

    class FakeAttachmentLoadingController : AttachmentLoadingController {
        override fun loadAttachment(
            part: Part?,
            listener: MessagingListener,
        ) {
            val localPart = part as? LocalPart
            val account = LegacyAccountDto("00000000-0000-4000-0000-000000000000")
            listener.loadAttachmentFinished(account, localPart?.message, part)
        }
    }

    private class FakeGeneralSettingsManager : GeneralSettingsManager {
        override fun getSettings(): GeneralSettings = getConfig()
        override fun getSettingsFlow(): Flow<GeneralSettings> = getConfigFlow()
        override fun getConfig(): GeneralSettings = GeneralSettings(
            platformConfigProvider = object : PlatformConfigProvider {
                override val isDebug: Boolean = true
            },
        )

        override fun getConfigFlow(): Flow<GeneralSettings> = flowOf(getConfig())
        override fun save(config: GeneralSettings) = Unit
    }
}
