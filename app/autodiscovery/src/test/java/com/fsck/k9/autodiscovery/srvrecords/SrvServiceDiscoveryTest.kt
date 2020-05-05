package com.fsck.k9.autodiscovery.srvrecords

import com.fsck.k9.RobolectricTest
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert
import org.junit.Test

class SrvServiceDiscoveryTest : RobolectricTest() {

    @Test
    fun discover_whenNoMailServices_shouldReturnNull() {
        val srvResolver = mock<SrvResolver> {
            on { lookup(eq("example.com"), any<SrvType>()) } doReturn listOf()
        }
        val srvServiceDiscovery = SrvServiceDiscovery(srvResolver)
        val result = srvServiceDiscovery.discover("test@example.com")
        Assert.assertNull(result)
        verify(srvResolver).lookup("example.com", SrvType.SUBMISSION)
        verify(srvResolver).lookup("example.com", SrvType.IMAP)
        verify(srvResolver).lookup("example.com", SrvType.IMAPS)
    }
}
