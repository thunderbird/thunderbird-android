package com.fsck.k9.ui.settings.general

import android.net.Uri
import app.cash.turbine.test
import com.fsck.k9.logging.LogFileWriter
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class GeneralSettingsViewModelTest {
    private val logFileWriter = TestLogFileWriter()
    private val contentUri = mock<Uri>()
    private val viewModel = GeneralSettingsViewModel(logFileWriter)
    private val testCoroutineDispatcher = TestCoroutineDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testCoroutineDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `export logs without errors`() = runBlocking {
        viewModel.uiState.test {
            viewModel.exportLogs(contentUri)

            assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Idle)
            assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Exporting)
            assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Success)
            testCoroutineDispatcher.advanceTimeBy(GeneralSettingsViewModel.SNACKBAR_DURATION)
            assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Idle)
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @Test
    fun `export logs with consumer changing while LogFileWriter_writeLogTo is running`() = runBlocking {
        withTimeout(timeMillis = 1000L) {
            logFileWriter.shouldWait()

            val mutex = Mutex(locked = true)

            // The first consumer
            val job = launch(CoroutineName("ConsumerOne")) {
                var first = true
                val states = viewModel.uiState.onEach {
                    if (first) {
                        first = false
                        mutex.unlock()
                    }
                }.take(2).toList()

                assertThat(states[0]).isEqualTo(GeneralSettingsUiState.Idle)
                assertThat(states[1]).isEqualTo(GeneralSettingsUiState.Exporting)
            }

            // Wait until the "ConsumerOne" coroutine has collected the initial UI state
            mutex.lock()

            viewModel.exportLogs(contentUri)

            // Wait until the "ConsumerOne" coroutine has finished collecting items
            job.join()

            // The second consumer
            viewModel.uiState.test {
                assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Exporting)
                logFileWriter.resume()
                assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Success)
                testCoroutineDispatcher.advanceTimeBy(GeneralSettingsViewModel.SNACKBAR_DURATION)
                assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Idle)
                assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
            }
        }
    }

    @Test
    fun `export logs with IOException`() = runBlocking {
        logFileWriter.exception = IOException()

        viewModel.uiState.test {
            viewModel.exportLogs(contentUri)

            assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Idle)
            assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Exporting)
            assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Failure)
            testCoroutineDispatcher.advanceTimeBy(GeneralSettingsViewModel.SNACKBAR_DURATION)
            assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Idle)
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }

    @Test
    fun `export logs with IllegalStateException`() = runBlocking {
        logFileWriter.exception = IllegalStateException()

        viewModel.uiState.test {
            viewModel.exportLogs(contentUri)

            assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Idle)
            assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Exporting)
            assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Failure)
            testCoroutineDispatcher.advanceTimeBy(GeneralSettingsViewModel.SNACKBAR_DURATION)
            assertThat(awaitItem()).isEqualTo(GeneralSettingsUiState.Idle)
            assertThat(cancelAndConsumeRemainingEvents()).isEmpty()
        }
    }
}

class TestLogFileWriter : LogFileWriter {
    var exception: Throwable? = null
    private var mutex: Mutex? = null

    override suspend fun writeLogTo(contentUri: Uri) {
        exception?.let { throw it }

        mutex?.lock()
    }

    fun shouldWait() {
        mutex = Mutex(locked = true)
    }

    fun resume() {
        mutex!!.unlock()
    }
}
