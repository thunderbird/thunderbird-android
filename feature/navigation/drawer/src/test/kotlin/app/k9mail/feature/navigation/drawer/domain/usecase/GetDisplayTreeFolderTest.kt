package app.k9mail.feature.navigation.drawer.domain.usecase

import app.k9mail.core.mail.folder.api.Folder
import app.k9mail.core.mail.folder.api.FolderType
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccountFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayTreeFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolder
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayUnifiedFolderType
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

class GetDisplayTreeFolderTest {

    @Test
    fun `should create tree from regular folder`() {
        // arrange
        val regularFolder = createDisplayAccountFolder(
            folderId = 1L,
            folderName = "folder",
            unreadMessageCount = 5,
            starredMessageCount = 2,
        )
        val folders = listOf(
            regularFolder,
        )
        val testSubject = GetDisplayTreeFolder()

        // act
        val result = testSubject(folders, 1)

        // assert
        val expected = createDisplayTreeFolder(
            totalUnreadCount = regularFolder.unreadMessageCount,
            totalStarredCount = regularFolder.starredMessageCount,
            children = persistentListOf(
                createDisplayTreeFolder(
                    displayFolder = regularFolder,
                    displayName = regularFolder.folder.name,
                    totalUnreadCount = regularFolder.unreadMessageCount,
                    totalStarredCount = regularFolder.starredMessageCount,
                ),
            ),
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should create tree from display unified folder`() {
        // arrange
        val unifiedFolder = createDisplayUnifiedFolder(
            unifiedFolderType = DisplayUnifiedFolderType.INBOX,
            unreadMessageCount = 5,
            starredMessageCount = 2,
        )
        val folders = listOf(
            unifiedFolder,
        )
        val testSubject = GetDisplayTreeFolder()

        // act
        val result = testSubject(folders, 1)

        // assert
        val expected = createDisplayTreeFolder(
            totalUnreadCount = 0,
            totalStarredCount = 0,
            children = persistentListOf(
                createDisplayTreeFolder(
                    displayFolder = unifiedFolder,
                    displayName = unifiedFolder.unifiedType.id,
                    totalUnreadCount = unifiedFolder.unreadMessageCount,
                    totalStarredCount = unifiedFolder.starredMessageCount,
                ),
            ),
        )
        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should create tree from unified folders and regular folders`() {
        // arrange
        val unifiedFolder = createDisplayUnifiedFolder(
            unifiedFolderType = DisplayUnifiedFolderType.INBOX,
            unreadMessageCount = 3,
            starredMessageCount = 1,
        )
        val regularFolder = createDisplayAccountFolder(
            folderId = 1L,
            folderName = "folder",
            unreadMessageCount = 3,
            starredMessageCount = 1,
        )
        val folders = listOf(
            unifiedFolder,
            regularFolder,
        )
        val testSubject = GetDisplayTreeFolder()

        // act
        val result = testSubject(folders, 1)

        // assert
        val expected = createDisplayTreeFolder(
            totalUnreadCount = regularFolder.unreadMessageCount,
            totalStarredCount = regularFolder.starredMessageCount,
            children = persistentListOf(
                createDisplayTreeFolder(
                    displayFolder = unifiedFolder,
                    displayName = unifiedFolder.unifiedType.id,
                    totalUnreadCount = unifiedFolder.unreadMessageCount,
                    totalStarredCount = unifiedFolder.starredMessageCount,
                ),
                createDisplayTreeFolder(
                    displayFolder = regularFolder,
                    displayName = regularFolder.folder.name,
                    totalUnreadCount = regularFolder.unreadMessageCount,
                    totalStarredCount = regularFolder.starredMessageCount,
                ),
            ),
        )
        assertThat(result).isEqualTo(expected)
    }

    @Suppress("LongMethod")
    @Test
    fun `should handle folders with malformed names in hierarchy`() {
        // arrange
        val flatFolder = createDisplayAccountFolder(
            folderId = 1,
            folderName = "Inbox",
            unreadMessageCount = 2,
            starredMessageCount = 1,
        )
        val noNameFolder = createDisplayAccountFolder(
            folderId = 2,
            folderName = "", // malformed
            unreadMessageCount = 4,
            starredMessageCount = 2,
        )
        val weirdFolder = createDisplayAccountFolder(
            folderId = 3,
            folderName = "///", // malformed
            unreadMessageCount = 1,
            starredMessageCount = 2,
        )
        val nestedWeird = createDisplayAccountFolder(
            folderId = 4,
            folderName = "valid1///valid2",
            unreadMessageCount = 6,
            starredMessageCount = 3,
        )
        val folders = listOf(flatFolder, noNameFolder, weirdFolder, nestedWeird)
        val testSubject = GetDisplayTreeFolder()

        // act
        val result = testSubject(folders, maxDepth = 2)

        // assert
        val expectedOld = createDisplayTreeFolder(
            totalUnreadCount = 13, // 2 + 4 + 1 + 6
            totalStarredCount = 8, // 1 + 2 + 2 + 3
            children = persistentListOf(
                // Inbox
                createDisplayTreeFolder(
                    displayFolder = flatFolder,
                    displayName = "Inbox",
                    totalUnreadCount = flatFolder.unreadMessageCount,
                    totalStarredCount = flatFolder.starredMessageCount,
                ),
                // valid1 → (Unnamed) → (Unnamed)/valid2
                createDisplayTreeFolder(
                    displayFolder = createDisplayAccountFolder(
                        folderId = 0,
                        folderName = "valid1",
                        unreadMessageCount = 0,
                        starredMessageCount = 0,
                    ),
                    displayName = "valid1",
                    totalUnreadCount = 6,
                    totalStarredCount = 3,
                    children = persistentListOf(
                        createDisplayTreeFolder(
                            displayFolder = createDisplayAccountFolder(
                                folderId = 0,
                                folderName = "valid1/(Unnamed)",
                                unreadMessageCount = 0,
                                starredMessageCount = 0,
                            ),
                            displayName = "(Unnamed)",
                            totalUnreadCount = 6,
                            totalStarredCount = 3,
                            children = persistentListOf(
                                createDisplayTreeFolder(
                                    displayFolder = nestedWeird,
                                    displayName = "(Unnamed)/valid2",
                                    totalUnreadCount = 6,
                                    totalStarredCount = 3,
                                ),
                            ),
                        ),
                    ),
                ),
                // (Unnamed) parent for both "" and "///"
                createDisplayTreeFolder(
                    displayFolder = noNameFolder, // promoted as node
                    displayName = "(Unnamed)",
                    totalUnreadCount = 5, // 4 (noName) + 1 (weird)
                    totalStarredCount = 4, // 2 (noName) + 2 (weird)
                    children = persistentListOf(
                        createDisplayTreeFolder(
                            displayFolder = createDisplayAccountFolder(
                                folderId = 0,
                                folderName = "(Unnamed)/(Unnamed)",
                                unreadMessageCount = 0,
                                starredMessageCount = 0,
                            ),
                            displayName = "(Unnamed)",
                            totalUnreadCount = 1,
                            totalStarredCount = 2,
                            children = persistentListOf(
                                createDisplayTreeFolder(
                                    displayFolder = weirdFolder,
                                    displayName = "(Unnamed)/(Unnamed)",
                                    totalUnreadCount = 1,
                                    totalStarredCount = 2,
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val expected =  DisplayTreeFolder(
            displayFolder = null,
            displayName = null,
            totalUnreadCount = flatFolder.unreadMessageCount +
                noNameFolder.unreadMessageCount +
                weirdFolder.unreadMessageCount +
                nestedWeird.unreadMessageCount,
            totalStarredCount = flatFolder.starredMessageCount +
                noNameFolder.starredMessageCount +
                weirdFolder.starredMessageCount +
                nestedWeird.starredMessageCount,
            children = persistentListOf(
                DisplayTreeFolder(
                    displayFolder = DisplayAccountFolder(
                        accountId = "accountId",
                        folder = Folder(id = 1, name = "Inbox", type = FolderType.REGULAR, isLocalOnly = false),
                        isInTopGroup = true,
                        unreadMessageCount = 2,
                        starredMessageCount = 1
                    ),
                    displayName = "Inbox",
                    totalUnreadCount = 2,
                    totalStarredCount = 1,
                    children = persistentListOf()
                ),
                DisplayTreeFolder(
                    displayFolder = DisplayAccountFolder(
                        accountId = "accountId",
                        folder = Folder(id = 2, name = "", type = FolderType.REGULAR, isLocalOnly = false),
                        isInTopGroup = true,
                        unreadMessageCount = 4,
                        starredMessageCount = 2
                    ),
                    displayName = "(Unnamed)",
                    totalUnreadCount = 5,
                    totalStarredCount = 4,
                    children = persistentListOf(
                        DisplayTreeFolder(
                            displayFolder = DisplayAccountFolder(
                                accountId = "accountId",
                                folder = Folder(id = 0, name = "(Unnamed)/(Unnamed)", type = FolderType.REGULAR, isLocalOnly = false),
                                isInTopGroup = true,
                                unreadMessageCount = 0,
                                starredMessageCount = 0
                            ),
                            displayName = "(Unnamed)",
                            totalUnreadCount = 1,
                            totalStarredCount = 2,
                            children = persistentListOf(
                                DisplayTreeFolder(
                                    displayFolder = DisplayAccountFolder(
                                        accountId = "accountId",
                                        folder = Folder(id = 3, name = "///", type = FolderType.REGULAR, isLocalOnly = false),
                                        isInTopGroup = true,
                                        unreadMessageCount = 1,
                                        starredMessageCount = 2
                                    ),
                                    displayName = "(Unnamed)/(Unnamed)",
                                    totalUnreadCount = 1,
                                    totalStarredCount = 2,
                                    children = persistentListOf()
                                )
                            )
                        )
                    )
                ),
                DisplayTreeFolder(
                    displayFolder = DisplayAccountFolder(
                        accountId = "accountId",
                        folder = Folder(id = 0, name = "valid1", type = FolderType.REGULAR, isLocalOnly = false),
                        isInTopGroup = true,
                        unreadMessageCount = 0,
                        starredMessageCount = 0
                    ),
                    displayName = "valid1",
                    totalUnreadCount = 6,
                    totalStarredCount = 3,
                    children = persistentListOf(
                        DisplayTreeFolder(
                            displayFolder = DisplayAccountFolder(
                                accountId = "accountId",
                                folder = Folder(id = 0, name = "valid1/(Unnamed)", type = FolderType.REGULAR, isLocalOnly = false),
                                isInTopGroup = true,
                                unreadMessageCount = 0,
                                starredMessageCount = 0
                            ),
                            displayName = "(Unnamed)",
                            totalUnreadCount = 6,
                            totalStarredCount = 3,
                            children = persistentListOf(
                                DisplayTreeFolder(
                                    displayFolder = DisplayAccountFolder(
                                        accountId = "accountId",
                                        folder = Folder(id = 4, name = "valid1///valid2", type = FolderType.REGULAR, isLocalOnly = false),
                                        isInTopGroup = true,
                                        unreadMessageCount = 6,
                                        starredMessageCount = 3
                                    ),
                                    displayName = "(Unnamed)/valid2",
                                    totalUnreadCount = 6,
                                    totalStarredCount = 3,
                                    children = persistentListOf()
                                )
                            )
                        )
                    )
                )
            )
        )

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `should create tree from nested folders`() {
        // arrange
        val folder1 = createDisplayAccountFolder(
            folderId = 1L,
            folderName = "folderLevel1/folderLevel2_1",
            unreadMessageCount = 5,
            starredMessageCount = 2,
        )
        val folder2 = createDisplayAccountFolder(
            folderId = 2L,
            folderName = "folderLevel1/folderLevel2_2",
            unreadMessageCount = 3,
            starredMessageCount = 1,
        )
        val folders = listOf(folder1, folder2)
        val testSubject = GetDisplayTreeFolder()

        // act
        val result = testSubject(folders, maxDepth = 3)

        // assert
        val expected = createDisplayTreeFolder(
            totalUnreadCount = 8,
            totalStarredCount = 3,
            children = persistentListOf(
                createDisplayTreeFolder(
                    displayFolder = createDisplayAccountFolder(
                        folderId = 0L,
                        folderName = "folderLevel1",
                        unreadMessageCount = 0,
                        starredMessageCount = 0,
                    ),
                    displayName = "folderLevel1",
                    totalUnreadCount = 8,
                    totalStarredCount = 3,
                    children = persistentListOf(
                        createDisplayTreeFolder(
                            displayFolder = folder1,
                            displayName = "folderLevel2_1",
                            totalUnreadCount = 5,
                            totalStarredCount = 2,
                        ),
                        createDisplayTreeFolder(
                            displayFolder = folder2,
                            displayName = "folderLevel2_2",
                            totalUnreadCount = 3,
                            totalStarredCount = 1,
                        ),
                    ),
                ),
            ),
        )

        assertThat(result).isEqualTo(expected)
    }

    @Suppress("LongMethod")
    @Test
    fun `should flatten folders beyond max depth`() {
        // arrange
        val deepFolder = createDisplayAccountFolder(
            folderId = 1L,
            folderName = "level1/level2/level3",
            unreadMessageCount = 10,
            starredMessageCount = 4,
        )
        val deeperFolder = createDisplayAccountFolder(
            folderId = 2L,
            folderName = "level1/level2/level3/level4",
            unreadMessageCount = 2,
            starredMessageCount = 1,
        )
        val folders = listOf(deepFolder, deeperFolder)
        val testSubject = GetDisplayTreeFolder()

        // act
        val result = testSubject(folders, maxDepth = 2)

        // assert
        val expected = createDisplayTreeFolder(
            totalUnreadCount = 12,
            totalStarredCount = 5,
            children = persistentListOf(
                createDisplayTreeFolder(
                    displayFolder = createDisplayAccountFolder(
                        folderId = 0,
                        folderName = "level1",
                        unreadMessageCount = 0,
                        starredMessageCount = 0,
                    ),
                    displayName = "level1",
                    totalUnreadCount = 12,
                    totalStarredCount = 5,
                    children = persistentListOf(
                        createDisplayTreeFolder(
                            displayFolder = createDisplayAccountFolder(
                                folderId = 0,
                                folderName = "level1/level2",
                                unreadMessageCount = 0,
                                starredMessageCount = 0,
                            ),
                            displayName = "level2",
                            totalUnreadCount = 12,
                            totalStarredCount = 5,
                            children = persistentListOf(
                                createDisplayTreeFolder(
                                    displayFolder = deepFolder,
                                    totalUnreadCount = 10,
                                    totalStarredCount = 4,
                                    displayName = "level3",
                                ),
                                createDisplayTreeFolder(
                                    displayFolder = deeperFolder,
                                    totalUnreadCount = 2,
                                    totalStarredCount = 1,
                                    displayName = "level3/level4",
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )

        assertThat(result).isEqualTo(expected)
    }

    private companion object {
        fun createDisplayAccountFolder(
            folderId: Long,
            folderName: String,
            unreadMessageCount: Int,
            starredMessageCount: Int,
        ): DisplayAccountFolder {
            return DisplayAccountFolder(
                accountId = "accountId",
                folder = Folder(
                    id = folderId,
                    name = folderName,
                    type = FolderType.REGULAR,
                    isLocalOnly = false,
                ),
                isInTopGroup = true,
                unreadMessageCount = unreadMessageCount,
                starredMessageCount = starredMessageCount,
            )
        }

        fun createDisplayUnifiedFolder(
            unifiedFolderType: DisplayUnifiedFolderType,
            unreadMessageCount: Int,
            starredMessageCount: Int,
        ): DisplayUnifiedFolder {
            return DisplayUnifiedFolder(
                id = unifiedFolderType.name.lowercase(),
                unifiedType = unifiedFolderType,
                unreadMessageCount = unreadMessageCount,
                starredMessageCount = starredMessageCount,
            )
        }

        fun createDisplayTreeFolder(
            displayFolder: DisplayFolder? = null,
            displayName: String? = null,
            totalUnreadCount: Int,
            totalStarredCount: Int,
            children: ImmutableList<DisplayTreeFolder> = persistentListOf(),
        ): DisplayTreeFolder {
            return DisplayTreeFolder(
                displayFolder = displayFolder,
                displayName = displayName,
                totalUnreadCount = totalUnreadCount,
                totalStarredCount = totalStarredCount,
                children = children,
            )
        }
    }
}
