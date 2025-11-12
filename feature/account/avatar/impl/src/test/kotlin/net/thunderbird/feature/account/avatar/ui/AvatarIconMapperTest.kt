package net.thunderbird.feature.account.avatar.ui

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.core.ui.compose.designsystem.atom.icon.Icons

class AvatarIconMapperTest {

    @Test
    fun `known names map to expected icons`() {
        assertThat(AvatarIconMapper.toIcon("star")).isEqualTo(Icons.Outlined.Star)
        assertThat(AvatarIconMapper.toIcon("person")).isEqualTo(Icons.Outlined.Person)
        assertThat(AvatarIconMapper.toIcon("folder")).isEqualTo(Icons.Outlined.Folder)
        assertThat(AvatarIconMapper.toIcon("pets")).isEqualTo(Icons.Outlined.Pets)
        assertThat(AvatarIconMapper.toIcon("rocket")).isEqualTo(Icons.Outlined.Rocket)
        assertThat(AvatarIconMapper.toIcon("spa")).isEqualTo(Icons.Outlined.Spa)
    }

    @Test
    fun `known names are case insensitive`() {
        assertThat(AvatarIconMapper.toIcon("STAR")).isEqualTo(Icons.Outlined.Star)
        assertThat(AvatarIconMapper.toIcon("Person")).isEqualTo(Icons.Outlined.Person)
        assertThat(AvatarIconMapper.toIcon("FoLdEr")).isEqualTo(Icons.Outlined.Folder)
        assertThat(AvatarIconMapper.toIcon("PeTs")).isEqualTo(Icons.Outlined.Pets)
        assertThat(AvatarIconMapper.toIcon("ROCKET")).isEqualTo(Icons.Outlined.Rocket)
        assertThat(AvatarIconMapper.toIcon("sPa")).isEqualTo(Icons.Outlined.Spa)
    }

    @Test
    fun `unknown name falls back to default icon`() {
        assertThat(AvatarIconMapper.toIcon("unknown")).isEqualTo(Icons.Outlined.Person)
        assertThat(AvatarIconMapper.toIcon("")).isEqualTo(Icons.Outlined.Person)
    }
}
