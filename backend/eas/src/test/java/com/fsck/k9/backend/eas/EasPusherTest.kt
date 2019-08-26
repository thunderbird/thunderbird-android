package com.fsck.k9.backend.eas

import com.fsck.k9.backend.eas.dto.*
import com.fsck.k9.mail.PushReceiver
import com.fsck.k9.mail.power.PowerManager
import com.fsck.k9.mail.power.WakeLock
import com.nhaarman.mockito_kotlin.*
import org.junit.Test

class EasPusherTest {
    private val client = mock<EasClient>()
    private val wakeLock = mock<WakeLock>()
    private val powerManager = mock<PowerManager>() {
        on { newWakeLock(any()) } doReturn wakeLock
    }

    private val receiver = mock<PushReceiver>()

    @Test
    fun pusher_shouldStartProcessResponseAndStop() {
        val cut = EasPusher(client, powerManager, receiver)

        whenever(client.ping(
                Ping(heartbeatInterval = 800,
                        pingFolders = PingFolders(listOf(
                                PingFolder("Email", "col0"),
                                PingFolder("Email", "col1")))),
                timeout = 820000L))
                .thenReturn(
                        PingResponse(
                                PingResponseFolders(listOf("col1")),
                                status = 2)
                )
                .thenAnswer { Thread.sleep(10000L) } // Simulate long running poll

        cut.start(listOf("col0", "col1"))

        verify(wakeLock, timeout(1000)).setReferenceCounted(false)
        verify(wakeLock).acquire(820000L)
        verify(receiver).setPushActive("col0", true)
        verify(receiver).setPushActive("col1", true)
        verify(receiver).syncFolder(argThat { serverId == "col1" })

        cut.stop()

        verify(receiver, timeout(1000)).setPushActive("col0", false)
        verify(receiver).setPushActive("col1", false)
        verify(wakeLock).release()

        verifyNoMoreInteractions(receiver, wakeLock)
    }
}
