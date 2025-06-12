package net.thunderbird.android

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.util.DisplayMetrics
import androidx.lifecycle.LifecycleOwner
import androidx.work.WorkerParameters
import app.k9mail.feature.account.common.domain.entity.InteractionMode
import com.fsck.k9.account.AccountRemoverWorker
import com.fsck.k9.job.MailSyncWorker
import com.fsck.k9.mailstore.AttachmentResolver
import com.fsck.k9.message.html.DisplayHtml
import com.fsck.k9.message.html.HtmlSettings
import com.fsck.k9.ui.changelog.ChangeLogMode
import com.fsck.k9.ui.changelog.ChangelogViewModel
import com.fsck.k9.view.K9WebViewClient
import com.fsck.k9.view.MessageWebView
import net.openid.appauth.AppAuthConfiguration
import net.thunderbird.feature.account.AccountId
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.test.verify.definition
import org.koin.test.verify.injectedParameters
import org.koin.test.verify.verify
import org.openintents.openpgp.OpenPgpApiManager

class DependencyInjectionTest {

    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun testDependencyTree() {
        appModule.verify(
            extraTypes = listOf(
                AccountId::class,
                AppAuthConfiguration::class,
                Application::class,
                AssetManager::class,
                Configuration::class,
                Context::class,
                DisplayMetrics::class,
                InteractionMode::class,
                NotificationManager::class,
                Resources::class,
            ),
            injections = injectedParameters(
                definition<AccountRemoverWorker>(WorkerParameters::class),
                definition<ChangelogViewModel>(ChangeLogMode::class),
                definition<DisplayHtml>(HtmlSettings::class),
                definition<K9WebViewClient>(AttachmentResolver::class, MessageWebView.OnPageFinishedListener::class),
                definition<MailSyncWorker>(WorkerParameters::class),
                definition<OpenPgpApiManager>(LifecycleOwner::class),
            ),
        )
    }
}
