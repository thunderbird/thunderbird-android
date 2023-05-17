package com.fsck.k9.mail.store.imap

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.fsck.k9.mail.AuthenticationFailedException
import java.io.IOException
import java.net.SocketException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import org.junit.Test

private const val FOLDER_SERVER_ID = "folder"
private const val TEST_TIMEOUT_SECONDS = 5L
private const val IDLE_TIMEOUT_MS = 28 * 60 * 1000L

class RealImapFolderIdlerTest {
    private val idleRefreshManager = TestIdleRefreshManager()
    private val wakeLock = TestWakeLock(timeoutSeconds = TEST_TIMEOUT_SECONDS, isHeld = true)
    private val imapConnection = TestImapConnection(timeout = TEST_TIMEOUT_SECONDS)
    private val imapFolder = TestImapFolder(FOLDER_SERVER_ID, imapConnection)
    private val imapStore = TestImapStore(imapFolder)
    private val idleRefreshTimeoutProvider = object : IdleRefreshTimeoutProvider {
        override val idleRefreshTimeoutMs = IDLE_TIMEOUT_MS
    }
    private val idler = RealImapFolderIdler(
        idleRefreshManager,
        wakeLock,
        imapStore,
        imapStore,
        FOLDER_SERVER_ID,
        idleRefreshTimeoutProvider,
    )

    @Test
    fun `new message during IDLE`() {
        val latch = CountDownLatch(1)

        thread {
            val idleResult = idler.idle()

            assertThat(idleResult).isEqualTo(IdleResult.SYNC)
            latch.countDown()
        }

        imapConnection.waitForCommand("IDLE")
        imapConnection.enqueueContinuationServerResponse()
        imapConnection.enqueueUntaggedServerResponse("1 EXISTS")
        imapConnection.waitForCommand("DONE")
        imapConnection.enqueueTaggedServerResponse("OK")

        latch.awaitWithTimeout()
        assertThat(imapFolder.isOpen).isFalse()
    }

    @Test
    fun `flag change during IDLE`() {
        val latch = CountDownLatch(1)

        thread {
            val idleResult = idler.idle()

            assertThat(idleResult).isEqualTo(IdleResult.SYNC)
            latch.countDown()
        }

        imapConnection.waitForCommand("IDLE")
        imapConnection.enqueueContinuationServerResponse()
        imapConnection.enqueueUntaggedServerResponse("42 FETCH (FLAGS (\\Seen))")
        imapConnection.waitForCommand("DONE")
        imapConnection.enqueueTaggedServerResponse("OK")

        latch.awaitWithTimeout()
        assertThat(imapFolder.isOpen).isFalse()
    }

    @Test
    fun `expunge during IDLE`() {
        val latch = CountDownLatch(1)

        thread {
            val idleResult = idler.idle()

            assertThat(idleResult).isEqualTo(IdleResult.SYNC)
            latch.countDown()
        }

        imapConnection.waitForCommand("IDLE")
        imapConnection.enqueueContinuationServerResponse()
        imapConnection.enqueueUntaggedServerResponse("23 EXPUNGE")
        imapConnection.waitForCommand("DONE")
        imapConnection.enqueueTaggedServerResponse("OK")

        latch.awaitWithTimeout()
        assertThat(imapFolder.isOpen).isFalse()
    }

    @Test
    fun `refresh IDLE connection`() {
        val latch = CountDownLatch(1)

        thread {
            val idleResult = idler.idle()

            assertThat(idleResult).isEqualTo(IdleResult.SYNC)
            latch.countDown()
        }

        imapConnection.waitForCommand("IDLE")
        assertThat(wakeLock.isHeld).isTrue()
        imapConnection.enqueueContinuationServerResponse()
        wakeLock.waitForRelease()
        idleRefreshManager.resetTimers()
        imapConnection.waitForCommand("DONE")
        imapConnection.enqueueTaggedServerResponse("OK")

        imapConnection.waitForCommand("IDLE")
        assertThat(wakeLock.isHeld).isTrue()
        imapConnection.enqueueContinuationServerResponse()
        wakeLock.waitForRelease()
        imapConnection.enqueueUntaggedServerResponse("1 EXISTS")
        imapConnection.waitForCommand("DONE")
        imapConnection.enqueueTaggedServerResponse("OK")

        latch.awaitWithTimeout()
        assertThat(imapFolder.isOpen).isFalse()
        assertThat(wakeLock.isHeld).isTrue()
    }

    @Test
    fun `stop ImapFolderIdler while IDLE`() {
        val latch = CountDownLatch(1)

        thread {
            val idleResult = idler.idle()

            assertThat(idleResult).isEqualTo(IdleResult.STOPPED)
            latch.countDown()
        }

        imapConnection.waitForCommand("IDLE")
        imapConnection.enqueueContinuationServerResponse()
        wakeLock.waitForRelease()
        idler.stop()
        imapConnection.waitForCommand("DONE")
        imapConnection.enqueueTaggedServerResponse("OK")

        latch.awaitWithTimeout()
        assertThat(imapFolder.isOpen).isFalse()
    }

    @Test
    fun `idle refresh timeout`() {
        val latch = CountDownLatch(1)

        thread {
            val idleResult = idler.idle()

            assertThat(idleResult).isEqualTo(IdleResult.STOPPED)
            latch.countDown()
        }

        imapConnection.waitForCommand("IDLE")
        imapConnection.enqueueContinuationServerResponse()
        wakeLock.waitForRelease()
        assertThat(idleRefreshManager.getTimeoutValue()).isEqualTo(IDLE_TIMEOUT_MS)
        idler.stop()
        imapConnection.waitForCommand("DONE")
        imapConnection.enqueueTaggedServerResponse("OK")

        latch.awaitWithTimeout()
        assertThat(imapFolder.isOpen).isFalse()
    }

    @Test
    fun `socket read timeouts`() {
        val latch = CountDownLatch(1)

        thread {
            val idleResult = idler.idle()

            assertThat(idleResult).isEqualTo(IdleResult.STOPPED)
            latch.countDown()
        }

        imapConnection.waitForCommand("IDLE")
        imapConnection.enqueueContinuationServerResponse()
        wakeLock.waitForRelease()
        assertThat(imapConnection.currentSocketReadTimeout).isGreaterThan(IDLE_TIMEOUT_MS.toInt())
        idler.stop()
        imapConnection.waitForCommand("DONE")
        assertThat(imapConnection.currentSocketReadTimeout).isEqualTo(imapConnection.defaultSocketReadTimeout)
        imapConnection.enqueueTaggedServerResponse("OK")

        latch.awaitWithTimeout()
        assertThat(imapFolder.isOpen).isFalse()
    }

    @Test
    fun `IDLE not supported`() {
        val latch = CountDownLatch(1)
        imapConnection.setIdleNotSupported()

        thread {
            val idleResult = idler.idle()

            assertThat(idleResult).isEqualTo(IdleResult.NOT_SUPPORTED)
            latch.countDown()
        }

        latch.awaitWithTimeout()
        assertThat(imapFolder.isOpen).isFalse()
    }

    @Test
    fun `authentication error`() {
        val latch = CountDownLatch(1)
        imapFolder.throwOnOpen { throw AuthenticationFailedException("Authentication failure for test") }

        thread {
            assertFailure {
                idler.idle()
            }.isInstanceOf<AuthenticationFailedException>()
                .hasMessage("Authentication failure for test")

            latch.countDown()
        }

        latch.awaitWithTimeout()
        assertThat(imapFolder.isOpen).isFalse()
    }

    @Test
    fun `network error on folder open`() {
        val latch = CountDownLatch(1)
        imapFolder.throwOnOpen { throw IOException("I/O error for test") }

        thread {
            assertFailure {
                idler.idle()
            }.isInstanceOf<IOException>()
                .hasMessage("I/O error for test")

            latch.countDown()
        }

        latch.awaitWithTimeout()
        assertThat(imapFolder.isOpen).isFalse()
    }

    @Test
    fun `network error on IDLE`() {
        val latch = CountDownLatch(1)

        thread {
            assertFailure {
                idler.idle()
            }.isInstanceOf<IOException>()
                .hasMessage("Socket closed during IDLE")

            latch.countDown()
        }

        imapConnection.waitForCommand("IDLE")
        imapConnection.enqueueContinuationServerResponse()
        imapConnection.waitForBlockingRead()
        imapConnection.throwOnRead { throw SocketException("Socket closed during IDLE") }

        latch.awaitWithTimeout()
        assertThat(imapFolder.isOpen).isFalse()
    }

    @Test
    fun `NO response to IDLE command`() {
        val latch = CountDownLatch(1)

        thread {
            val idleResult = idler.idle()

            assertThat(idleResult).isEqualTo(IdleResult.NOT_SUPPORTED)
            latch.countDown()
        }

        imapConnection.waitForCommand("IDLE")
        imapConnection.enqueueTaggedServerResponse("NO")

        latch.awaitWithTimeout()
        assertThat(imapFolder.isOpen).isFalse()
    }

    @Test
    fun `irrelevant untagged response to IDLE command before continuation request`() {
        val latch = CountDownLatch(1)

        thread {
            val idleResult = idler.idle()

            assertThat(idleResult).isEqualTo(IdleResult.STOPPED)
            latch.countDown()
        }

        imapConnection.waitForCommand("IDLE")
        imapConnection.enqueueUntaggedServerResponse("OK irrelevant")
        imapConnection.enqueueContinuationServerResponse()

        wakeLock.waitForRelease()
        idler.stop()
        imapConnection.waitForCommand("DONE")
        imapConnection.enqueueTaggedServerResponse("OK")
        latch.awaitWithTimeout()
    }

    @Test
    fun `relevant untagged response to IDLE command before continuation request`() {
        val latch = CountDownLatch(1)

        thread {
            val idleResult = idler.idle()

            assertThat(idleResult).isEqualTo(IdleResult.SYNC)
            latch.countDown()
        }

        imapConnection.waitForCommand("IDLE")
        imapConnection.enqueueUntaggedServerResponse("1 EXISTS")
        imapConnection.enqueueContinuationServerResponse()
        imapConnection.waitForCommand("DONE")
        imapConnection.enqueueTaggedServerResponse("OK")
        latch.awaitWithTimeout()
    }
}

private fun CountDownLatch.awaitWithTimeout() {
    assertThat(await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS), "Test timed out").isTrue()
}
