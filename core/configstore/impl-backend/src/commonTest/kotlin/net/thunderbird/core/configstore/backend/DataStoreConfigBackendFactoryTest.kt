package net.thunderbird.core.configstore.backend

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import java.io.File
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.configstore.ConfigId
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreConfigBackendFactoryTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private lateinit var fileLocation: File

    @Before
    fun setUp() {
        fileLocation = tempFolder.newFolder()
    }

    @Test
    fun `create should return DefaultDataStoreConfigBackend`() {
        // Arrange
        val fileManager = FakeConfigBackendFileManager(fileLocation)
        val testSubject = DataStoreConfigBackendFactory(fileManager)
        val configId = ConfigId("test_backend", "test_feature")

        // Act
        val result = testSubject.create(configId)

        // Assert
        assertThat(result).isInstanceOf(DefaultDataStoreConfigBackend::class.java)
    }

    @Test
    fun `create should use file manager to get file path`() = runTest {
        // Arrange
        val fileManager = FakeConfigBackendFileManager(fileLocation)
        val testSubject = DataStoreConfigBackendFactory(fileManager)
        val configId = ConfigId("test_backend", "test_feature")

        // Act
        val dataStore = testSubject.create(configId)
        dataStore.clear()

        // Assert
        assertThat(fileManager.lastRequestedFileName).isEqualTo("test_id.preferences_pb")
    }

    private class FakeConfigBackendFileManager(private val fileLocation: File) : ConfigBackendFileManager {
        var lastRequestedFileName: String? = null

        override fun getFilePath(backendFileName: String): String {
            lastRequestedFileName = backendFileName
            return fileLocation.resolve(backendFileName).absolutePath
        }
    }
}
