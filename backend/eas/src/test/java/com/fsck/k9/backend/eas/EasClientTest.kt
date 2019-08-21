package com.fsck.k9.backend.eas

import com.fsck.k9.backend.eas.dto.*
import com.fsck.k9.mail.AuthenticationFailedException
import com.fsck.k9.mail.ConnectionSecurity
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.MessagingException
import com.fsck.k9.mail.ssl.TrustManagerFactory
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.io.OutputStream
import java.security.KeyStore
import javax.net.ssl.X509TrustManager

class EasClientTest {

    @Before
    fun initTrustManager() {
        val tmf = javax.net.ssl.TrustManagerFactory.getInstance("X509").run {
            init(null as KeyStore?)
            trustManagers.find {
                it is X509TrustManager
            }!! as X509TrustManager
        }

        whenever(trustManager.getTrustManagerForDomain(any(), any())).thenReturn(tmf)
    }

    private val DEVICE_ID = "DevID"

    private val trustManager = mock<TrustManagerFactory>()

    @Test(timeout = 5000)
    fun initialize_shouldCheckConnectionAndVersion() {
        val mockServer = MockWebServer()
        mockServer.enqueue(MockResponse().apply {
            addHeader("ms-asprotocolversions", "2.5,12.0,14.0")
        })

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val cut = EasClient(settings, trustManager, DEVICE_ID)

        cut.initialize()

        val request = mockServer.takeRequest()

        assertEquals(request.method, "OPTIONS")
        assertEquals(request.path, "/Microsoft-Server-ActiveSync?Cmd=OPTIONS&User=user&DeviceId=DevID&DeviceType=K9")
        assertEquals(request.getHeader("User-Agent"), "K9/${BuildConfig.VERSION_NAME}")
        assertEquals(request.getHeader("Authorization"), "Basic dXNlcjpwYXNz")

        // Should only initialize once
        cut.initialize()
    }

    @Test(expected = AuthenticationFailedException::class)
    fun initialize_invalidCredentials_shouldThrow() {
        val mockServer = MockWebServer()
        mockServer.enqueue(MockResponse().setResponseCode(401))

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "wrongpass")

        val cut = EasClient(settings, trustManager, DEVICE_ID)

        cut.initialize()
    }

    @Test(expected = MessagingException::class)
    fun initialize_serverVersionHeaderMissing_shouldThrow() {
        val mockServer = MockWebServer()
        mockServer.enqueue(MockResponse())

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val cut = EasClient(settings, trustManager, DEVICE_ID)

        cut.initialize()
    }

    @Test(expected = MessagingException::class)
    fun initialize_serverVersionNotSupported_shouldThrow() {
        val mockServer = MockWebServer()
        mockServer.enqueue(MockResponse().apply {
            addHeader("ms-asprotocolversions", "2.5")
        })

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val cut = EasClient(settings, trustManager, DEVICE_ID)

        cut.initialize()
    }

    @Test
    fun sendMail_shouldInitializeAndPostMessage() {
        val mockServer = MockWebServer()

        mockServer.enqueue(MockResponse().apply {
            addHeader("ms-asprotocolversions", "12.0")
        })

        mockServer.enqueue(MockResponse())

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val message = mock<Message>()
        whenever(message.calculateSize()).thenReturn(17)
        whenever(message.writeTo(any())).thenAnswer {
            (it.getArgument(0) as OutputStream).write("Filter\nNext Line".toByteArray())
        }

        val cut = EasClient(settings, trustManager, DEVICE_ID)
        cut.policyKey = "8546"

        cut.sendMessage(message)

        // Ignore "OPTIONS"
        mockServer.takeRequest()

        val request = mockServer.takeRequest()

        assertEquals(request.method, "POST")
        assertEquals(request.path, "/Microsoft-Server-ActiveSync?Cmd=SendMail&User=user&DeviceId=DevID&DeviceType=K9&SaveInSent=T")
        assertEquals(request.getHeader("Content-Type"), "message/rfc822")
        assertEquals(request.getHeader("Content-Length"), "17")
        assertEquals(request.getHeader("User-Agent"), "K9/${BuildConfig.VERSION_NAME}")
        assertEquals(request.getHeader("Authorization"), "Basic dXNlcjpwYXNz")
        assertEquals(request.getHeader("MS-ASProtocolVersion"), "12.0")
        assertEquals(request.getHeader("X-MS-PolicyKey"), "8546")
        assertArrayEquals(request.body.readByteArray(), "Filter\r\nNext Line".toByteArray())
    }

    @Test(expected = AuthenticationFailedException::class)
    fun sendMail_cant_auth_shouldThrow() {
        val mockServer = MockWebServer()

        mockServer.enqueue(MockResponse().apply {
            addHeader("ms-asprotocolversions", "12.0")
        })

        mockServer.enqueue(MockResponse().setResponseCode(401))

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val message = mock<Message>()
        whenever(message.calculateSize()).thenReturn(17)
        whenever(message.writeTo(any())).thenAnswer {
            (it.getArgument(0) as OutputStream).write("Filter\nNext Line".toByteArray())
        }

        val cut = EasClient(settings, trustManager, DEVICE_ID)

        cut.sendMessage(message)
    }

    @Test(expected = UnprovisionedException::class)
    fun sendMail_unprovisioned_shouldThrow() {
        val mockServer = MockWebServer()

        mockServer.enqueue(MockResponse().apply {
            addHeader("ms-asprotocolversions", "12.0")
        })

        mockServer.enqueue(MockResponse().setResponseCode(449))

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val message = mock<Message>()
        whenever(message.calculateSize()).thenReturn(17)
        whenever(message.writeTo(any())).thenAnswer {
            (it.getArgument(0) as OutputStream).write("Filter\nNext Line".toByteArray())
        }

        val cut = EasClient(settings, trustManager, DEVICE_ID)

        cut.sendMessage(message)
    }

    @Test(expected = IOException::class)
    fun sendMail_unknownStatusCode_shouldThrow() {
        val mockServer = MockWebServer()

        mockServer.enqueue(MockResponse().apply {
            addHeader("ms-asprotocolversions", "12.0")
        })

        mockServer.enqueue(MockResponse().setResponseCode(404))

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val message = mock<Message>()
        whenever(message.writeTo(any())).thenAnswer {
            (it.getArgument(0) as OutputStream).write("Filter\nNext Line".toByteArray())
        }

        val cut = EasClient(settings, trustManager, DEVICE_ID)

        cut.sendMessage(message)
    }

    @Test
    fun provision_shouldInitializeAndProvision() {
        val mockServer = MockWebServer()

        mockServer.enqueue(MockResponse().apply {
            addHeader("ms-asprotocolversions", "12.0")
        })

        val expectedRequest = Provision(ProvisionPolicies(ProvisionPolicy("Type")))

        val expectedResponse = Provision(status = 2)

        mockServer.enqueue(MockResponse().setBody(Buffer().apply {
            WbXmlMapper.serialize(ProvisionDTO(expectedResponse), this.outputStream())
        }))
        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val cut = EasClient(settings, trustManager, DEVICE_ID)
        cut.policyKey = "8546"

        val actualResponse = cut.provision(expectedRequest)

        // Ignore "OPTIONS"
        mockServer.takeRequest()

        val request = mockServer.takeRequest()

        assertEquals(request.method, "POST")
        assertEquals(request.path, "/Microsoft-Server-ActiveSync?Cmd=Provision&User=user&DeviceId=DevID&DeviceType=K9")
        assertEquals(request.getHeader("Content-Type"), "application/vnd.ms-sync.wbxml")
        assertEquals(request.getHeader("User-Agent"), "K9/${BuildConfig.VERSION_NAME}")
        assertEquals(request.getHeader("Authorization"), "Basic dXNlcjpwYXNz")
        assertEquals(request.getHeader("MS-ASProtocolVersion"), "12.0")
        assertEquals(request.getHeader("X-MS-PolicyKey"), "8546")

        val actualRequest = WbXmlMapper.parse<ProvisionDTO>(request.body.inputStream()).provision

        assertEquals(expectedResponse, actualResponse)
        assertEquals(expectedRequest, actualRequest)
    }

    @Test(expected = AuthenticationFailedException::class)
    fun provision_cant_auth_shouldThrow() {
        val mockServer = MockWebServer()

        mockServer.enqueue(MockResponse().apply {
            addHeader("ms-asprotocolversions", "12.0")
        })

        mockServer.enqueue(MockResponse().setResponseCode(401))

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val cut = EasClient(settings, trustManager, DEVICE_ID)

        cut.provision(Provision())
    }

    @Test
    fun folderSync_shouldInitializeAndFolderSync() {
        val mockServer = MockWebServer()

        mockServer.enqueue(MockResponse().apply {
            addHeader("ms-asprotocolversions", "12.0")
        })

        val expectedRequest = FolderSync("key0")

        val expectedResponse = FolderSync("key1")

        mockServer.enqueue(MockResponse().setBody(Buffer().apply {
            WbXmlMapper.serialize(FolderSyncDTO(expectedResponse), this.outputStream())
        }))

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val cut = EasClient(settings, trustManager, DEVICE_ID)
        cut.policyKey = "8546"

        val actualResponse = cut.folderSync(expectedRequest)

        // Ignore "OPTIONS"
        mockServer.takeRequest()

        val request = mockServer.takeRequest()

        assertEquals(request.method, "POST")
        assertEquals(request.path, "/Microsoft-Server-ActiveSync?Cmd=FolderSync&User=user&DeviceId=DevID&DeviceType=K9")
        assertEquals(request.getHeader("Content-Type"), "application/vnd.ms-sync.wbxml")
        assertEquals(request.getHeader("User-Agent"), "K9/${BuildConfig.VERSION_NAME}")
        assertEquals(request.getHeader("Authorization"), "Basic dXNlcjpwYXNz")
        assertEquals(request.getHeader("MS-ASProtocolVersion"), "12.0")
        assertEquals(request.getHeader("X-MS-PolicyKey"), "8546")

        val actualRequest = WbXmlMapper.parse<FolderSyncDTO>(request.body.inputStream()).folderSync

        assertEquals(expectedResponse, actualResponse)
        assertEquals(expectedRequest, actualRequest)
    }

    @Test(expected = AuthenticationFailedException::class)
    fun folderSync_cant_auth_shouldThrow() {
        val mockServer = MockWebServer()

        mockServer.enqueue(MockResponse().apply {
            addHeader("ms-asprotocolversions", "12.0")
        })

        mockServer.enqueue(MockResponse().setResponseCode(401))

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val cut = EasClient(settings, trustManager, DEVICE_ID)

        cut.folderSync(FolderSync("123"))
    }

    @Test
    fun sync_shouldInitializeAndSync() {
        val mockServer = MockWebServer()

        mockServer.enqueue(MockResponse().apply {
            addHeader("ms-asprotocolversions", "12.0")
        })

        val expectedRequest = Sync(SyncCollections(SyncCollection(syncKey = "key0")))

        val expectedResponse = Sync(SyncCollections(SyncCollection(syncKey = "key1")))

        mockServer.enqueue(MockResponse().setBody(Buffer().apply {
            WbXmlMapper.serialize(SyncDTO(expectedResponse), this.outputStream())
        }))

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val cut = EasClient(settings, trustManager, DEVICE_ID)
        cut.policyKey = "8546"

        val actualResponse = cut.sync(expectedRequest)

        // Ignore "OPTIONS"
        mockServer.takeRequest()

        val request = mockServer.takeRequest()

        assertEquals(request.method, "POST")
        assertEquals(request.path, "/Microsoft-Server-ActiveSync?Cmd=Sync&User=user&DeviceId=DevID&DeviceType=K9")
        assertEquals(request.getHeader("Content-Type"), "application/vnd.ms-sync.wbxml")
        assertEquals(request.getHeader("User-Agent"), "K9/${BuildConfig.VERSION_NAME}")
        assertEquals(request.getHeader("Authorization"), "Basic dXNlcjpwYXNz")
        assertEquals(request.getHeader("MS-ASProtocolVersion"), "12.0")
        assertEquals(request.getHeader("X-MS-PolicyKey"), "8546")

        val actualRequest = WbXmlMapper.parse<SyncDTO>(request.body.inputStream()).sync

        assertEquals(expectedResponse, actualResponse)
        assertEquals(expectedRequest, actualRequest)
    }

    @Test(expected = AuthenticationFailedException::class)
    fun sync_cant_auth_shouldThrow() {
        val mockServer = MockWebServer()

        mockServer.enqueue(MockResponse().apply {
            addHeader("ms-asprotocolversions", "12.0")
        })

        mockServer.enqueue(MockResponse().setResponseCode(401))

        val url = mockServer.url("/")
        val settings = EasServerSettings(url.host(), url.port(), ConnectionSecurity.NONE, "user", "pass")

        val cut = EasClient(settings, trustManager, DEVICE_ID)

        cut.sync(Sync(SyncCollections(SyncCollection(syncKey = "key"))))
    }
}
