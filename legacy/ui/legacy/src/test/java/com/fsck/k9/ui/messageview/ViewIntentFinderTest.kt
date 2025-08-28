package com.fsck.k9.ui.messageview

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.ProviderInfo
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import assertk.all
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import com.fsck.k9.provider.AttachmentTempFileProvider
import kotlin.test.Test
import net.thunderbird.core.android.testing.RobolectricTest
import net.thunderbird.core.preference.GeneralSettingsManager
import org.junit.After
import org.junit.Before
import org.koin.core.context.GlobalContext.stopKoin
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.mockito.kotlin.mock
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf

class ViewIntentFinderTest : RobolectricTest() {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val attachmentTempFileProviderAuthority = "${context.packageName}.tempfileprovider"
    private val viewIntentFinder = ViewIntentFinder(context)

    @Before
    fun setUp() {
        startKoin {
            modules(
                module {
                    single { mock<GeneralSettingsManager>() }
                },
            )
        }
        // The AttachmentTempFileProvider methods called by ViewIntentFinder require the AUTHORITY property to be
        // set. The most robust way to accomplish this is to properly initialize the content provider.
        initializeAttachmentTempFileProvider()
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `provided non-default content type should be preferred`() {
        addViewerAppFor(mimeType = "application/pdf")
        addViewerAppFor(mimeType = "application/octet-stream")
        val contentUri = "content://$attachmentTempFileProviderAuthority/id".toUri()
        val displayName = "document.pdf"
        val mimeType = "application/pdf"

        val result = viewIntentFinder.getBestViewIntent(contentUri, displayName, mimeType)

        assertThat(result).all {
            prop(Intent::getAction).isEqualTo(Intent.ACTION_VIEW)
            prop(Intent::getData).isEqualTo(
                "content://$attachmentTempFileProviderAuthority/id?mime_type=application%2Fpdf".toUri(),
            )
            prop(Intent::getType).isEqualTo("application/pdf")
        }
    }

    @Test
    fun `inferred content type should be preferred over provided default content type`() {
        addViewerAppFor(mimeType = "application/pdf")
        addViewerAppFor(mimeType = "application/octet-stream")
        val contentUri = "content://$attachmentTempFileProviderAuthority/id".toUri()
        val displayName = "document.pdf"
        val mimeType = "application/octet-stream"

        val result = viewIntentFinder.getBestViewIntent(contentUri, displayName, mimeType)

        assertThat(result).all {
            prop(Intent::getAction).isEqualTo(Intent.ACTION_VIEW)
            prop(Intent::getData).isEqualTo(
                "content://$attachmentTempFileProviderAuthority/id?mime_type=application%2Fpdf".toUri(),
            )
            prop(Intent::getType).isEqualTo("application/pdf")
        }
    }

    @Test
    fun `inferred content type should be used when no app is installed for provided content type`() {
        addViewerAppFor(mimeType = "text/plain")
        addViewerAppFor(mimeType = "application/octet-stream")
        val contentUri = "content://$attachmentTempFileProviderAuthority/id".toUri()
        val displayName = "document.txt"
        val mimeType = "text/fancy-format"

        val result = viewIntentFinder.getBestViewIntent(contentUri, displayName, mimeType)

        assertThat(result).all {
            prop(Intent::getAction).isEqualTo(Intent.ACTION_VIEW)
            prop(Intent::getData).isEqualTo(
                "content://$attachmentTempFileProviderAuthority/id?mime_type=text%2Fplain".toUri(),
            )
            prop(Intent::getType).isEqualTo("text/plain")
        }
    }

    @Test
    fun `fall back to default content type when no app is installed for provided content type`() {
        addViewerAppFor(mimeType = "application/octet-stream")
        val contentUri = "content://$attachmentTempFileProviderAuthority/id".toUri()
        val displayName = "document.pdf"
        val mimeType = "application/pdf"

        val result = viewIntentFinder.getBestViewIntent(contentUri, displayName, mimeType)

        assertThat(result).all {
            prop(Intent::getAction).isEqualTo(Intent.ACTION_VIEW)
            prop(Intent::getData).isEqualTo(
                "content://$attachmentTempFileProviderAuthority/id?mime_type=application%2Foctet-stream".toUri(),
            )
            prop(Intent::getType).isEqualTo("application/octet-stream")
        }
    }

    private fun initializeAttachmentTempFileProvider() {
        val info = ProviderInfo().apply {
            authority = attachmentTempFileProviderAuthority
            grantUriPermissions = true
        }
        Robolectric.buildContentProvider(AttachmentTempFileProvider::class.java).create(info)
    }

    private var packageCounter = 1

    private fun addViewerAppFor(mimeType: String) {
        val viewerPackageName = "test.viewerapp.$packageCounter"
        val viewerActivityName = "$viewerPackageName.activity"
        packageCounter++

        val packageManager = shadowOf(context.packageManager)

        packageManager.installPackage(
            PackageInfo().apply {
                packageName = viewerPackageName
            },
        )

        val viewerActivityComponentName = ComponentName(viewerPackageName, viewerActivityName)
        packageManager.addActivityIfNotPresent(viewerActivityComponentName)

        val intentFilter = IntentFilter(Intent.ACTION_VIEW).apply {
            addDataType(mimeType)
            addDataScheme("content")
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        packageManager.addIntentFilterForActivity(viewerActivityComponentName, intentFilter)
    }
}
