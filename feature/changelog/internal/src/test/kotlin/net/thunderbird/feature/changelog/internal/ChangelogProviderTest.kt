package net.thunderbird.feature.changelog.internal

import android.content.Context
import android.content.res.Resources
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import java.io.ByteArrayInputStream
import net.thunderbird.core.logging.Logger
import net.thunderbird.feature.navigation.changelog.api.ChangelogConfigProvider
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ChangelogProviderTest {

    private val context = mock<Context>()
    private val resources = mock<Resources>()
    private val provider = mock<ChangelogConfigProvider>()
    private val logger = mock<Logger>()

    private lateinit var subject: ChangelogProvider

    @Before
    fun setUp() {
        whenever(context.resources).thenReturn(resources)
        whenever(context.packageName).thenReturn("net.thunderbird.android")

        subject = ChangelogProvider(
            context = context,
            provider = provider,
            logger = logger,
        )
    }

    @Test
    fun `should return changelog releases`() {
        whenever(provider.changelogIndexResId).thenReturn(1)

        whenever(resources.openRawResource(1)).thenReturn(
            jsonResource(
                indexJson(
                    indexRelease(
                        version = "10.0",
                        versionCode = 100,
                        resourceName = "changelog_release_100",
                    ),
                ),
            ),
        )

        whenever(
            resources.getIdentifier(
                "changelog_release_100",
                "raw",
                "net.thunderbird.android",
            ),
        ).thenReturn(2)

        whenever(resources.openRawResource(2)).thenReturn(
            jsonResource(
                """
                {
                    "schemaVersion": 1,
                    "version": "10.0",
                    "versioncode": 100,
                    "date": "2026-09-01",
                    "notes": [
                        {
                            "type": "new",
                            "text": "Added a new feature",
                            "source": "test"
                        },
                        {
                            "type": "fixed",
                            "text": "Fixed a crash",
                            "source": "test"
                        }
                    ]
                }
                """,
            ),
        )

        val result = subject.getChangeLog()

        assertThat(result).containsExactly(
            ReleaseItem(
                versionCode = 100,
                versionName = "10.0",
                date = "2026-09-01",
                changes = listOf(
                    "New:Added a new feature",
                    "Fixed:Fixed a crash",
                ),
            ),
        )
    }

    @Test
    fun `should skip changelog release when resource is not found`() {
        whenever(provider.changelogIndexResId).thenReturn(1)

        whenever(resources.openRawResource(1)).thenReturn(
            jsonResource(
                indexJson(
                    indexRelease(
                        version = "10.0",
                        versionCode = 100,
                        resourceName = "changelog_release_100",
                    ),
                ),
            ),
        )

        whenever(
            resources.getIdentifier(
                "changelog_release_100",
                "raw",
                "net.thunderbird.android",
            ),
        ).thenReturn(0)

        val result = subject.getChangeLog()

        assertThat(result).isEmpty()
    }

    @Test
    fun `should skip changelog release with unsupported schema version`() {
        whenever(provider.changelogIndexResId).thenReturn(1)

        whenever(resources.openRawResource(1)).thenReturn(
            jsonResource(
                indexJson(
                    indexRelease(
                        version = "10.0",
                        versionCode = 100,
                        resourceName = "changelog_release_100",
                    ),
                ),
            ),
        )

        whenever(
            resources.getIdentifier(
                "changelog_release_100",
                "raw",
                "net.thunderbird.android",
            ),
        ).thenReturn(2)

        whenever(resources.openRawResource(2)).thenReturn(
            jsonResource(
                """
                {
                    "schemaVersion": 2,
                    "version": "10.0",
                    "versioncode": 100,
                    "date": "2026-09-01",
                    "notes": []
                }
                """,
            ),
        )

        val result = subject.getChangeLog()

        assertThat(result).isEmpty()
    }

    @Test
    fun `should return empty list when changelog index is malformed`() {
        whenever(provider.changelogIndexResId).thenReturn(1)

        whenever(resources.openRawResource(1)).thenReturn(
            jsonResource(
                """
                {
                    "schemaVersion": 1,
                    "releases":
                }
                """,
            ),
        )

        val result = subject.getChangeLog()

        assertThat(result).isEmpty()
    }

    @Test
    fun `should skip changelog release when JSON is malformed`() {
        whenever(provider.changelogIndexResId).thenReturn(1)

        whenever(resources.openRawResource(1)).thenReturn(
            jsonResource(
                indexJson(
                    indexRelease(
                        version = "10.0",
                        versionCode = 100,
                        resourceName = "changelog_release_100",
                    ),
                ),
            ),
        )

        whenever(
            resources.getIdentifier(
                "changelog_release_100",
                "raw",
                "net.thunderbird.android",
            ),
        ).thenReturn(2)

        whenever(resources.openRawResource(2)).thenReturn(
            jsonResource(
                """
                {
                    "schemaVersion": 1,
                    "version": "10.0",
                    "versioncode":
                }
                """,
            ),
        )

        val result = subject.getChangeLog()

        assertThat(result).isEmpty()
    }

    @Test
    fun `should return changes since last version code`() {
        whenever(provider.changelogIndexResId).thenReturn(1)

        whenever(resources.openRawResource(1)).thenReturn(
            jsonResource(
                indexJson(
                    indexRelease(
                        version = "10.0",
                        versionCode = 100,
                        resourceName = "changelog_release_100",
                    ),
                    indexRelease(
                        version = "10.1",
                        versionCode = 101,
                        resourceName = "changelog_release_101",
                    ),
                    indexRelease(
                        version = "10.2",
                        versionCode = 102,
                        resourceName = "changelog_release_102",
                    ),
                ),
            ),
        )

        whenever(
            resources.getIdentifier(
                any(),
                eq("raw"),
                eq("net.thunderbird.android"),
            ),
        ).thenAnswer { invocation ->
            when (invocation.getArgument<String>(0)) {
                "changelog_release_100" -> 2
                "changelog_release_101" -> 3
                "changelog_release_102" -> 4
                else -> 0
            }
        }

        whenever(resources.openRawResource(2)).thenReturn(
            jsonResource(releaseJson("10.0", 100)),
        )

        whenever(resources.openRawResource(3)).thenReturn(
            jsonResource(releaseJson("10.1", 101)),
        )

        whenever(resources.openRawResource(4)).thenReturn(
            jsonResource(releaseJson("10.2", 102)),
        )

        val result = subject.getChangeLogSince(100)

        assertThat(result.map { it.versionCode }).containsExactly(
            101,
            102,
        )
    }

    private fun jsonResource(json: String) =
        ByteArrayInputStream(
            json.trimIndent().toByteArray(Charsets.UTF_8),
        )

    private fun indexJson(vararg releases: String) = """
        {
            "schemaVersion": 1,
            "releases": [
                ${releases.joinToString(",")}
            ]
        }
    """.trimIndent()

    private fun indexRelease(
        version: String,
        versionCode: Int,
        resourceName: String,
        date: String = "2026-09-01",
    ) = """
        {
            "version": "$version",
            "versioncode": $versionCode,
            "date": "$date",
            "resourceName": "$resourceName"
        }
    """.trimIndent()

    private fun releaseJson(
        version: String,
        versionCode: Int,
    ) = """
        {
            "schemaVersion": 1,
            "version": "$version",
            "versioncode": $versionCode,
            "date": "2026-09-01",
            "notes": []
        }
    """.trimIndent()
}
