package net.thunderbird.feature.account.settings.impl.ui.sendingMail

import assertk.all
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.prop
import kotlin.test.Test
import net.thunderbird.core.common.resources.StringsResourceManager
import net.thunderbird.core.ui.setting.Setting
import net.thunderbird.core.ui.setting.SettingValue
import net.thunderbird.core.ui.setting.SettingValue.Select.SelectOption

internal class SendingMailSettingsBuilderTest {

    private val resources = object : StringsResourceManager {
        override fun stringResource(resourceId: Int): String =
            "String for $resourceId"

        override fun stringResource(resourceId: Int, vararg formatArgs: Any?): String =
            stringResource(resourceId)
    }

    private val optionMapper = SendingMailSettingsOptionsMapper(
        resources = resources,
    )

    private val builder = SendingMailSettingsBuilder(
        resources = resources,
        optionMapper = optionMapper,
    )

    @Test
    fun `should build settings in correct order`() {
        val settings = builder.buildSettings(createState()) {}

        assertThat(settings).hasSize(12)

        assertThat(settings[0]).all {
            isInstanceOf<SettingValue.ActionText>()
            prop(Setting::id).isEqualTo(SendingMailSettingsId.COMPOSITION_DEFAULT)
        }

        assertThat(settings[1]).all {
            isInstanceOf<SettingValue.ActionText>()
            prop(Setting::id).isEqualTo(SendingMailSettingsId.MANAGE_IDENTITIES)
        }

        assertThat(settings[2]).all {
            isInstanceOf<SettingValue.Select>()
            prop(Setting::id).isEqualTo(SendingMailSettingsId.MESSAGE_FORMAT)
        }

        assertThat(settings[3]).all {
            isInstanceOf<SettingValue.Switch>()
            prop(Setting::id).isEqualTo(SendingMailSettingsId.ALWAYS_SHOW_CC_BCC)
        }

        assertThat(settings[4]).all {
            isInstanceOf<SettingValue.Switch>()
            prop(Setting::id).isEqualTo(SendingMailSettingsId.READ_RECEIPT)
        }

        assertThat(settings[5]).all {
            isInstanceOf<SettingValue.Select>()
            prop(Setting::id).isEqualTo(SendingMailSettingsId.REPLY_QUOTING_STYLE)
        }

        assertThat(settings[6]).all {
            isInstanceOf<SettingValue.Switch>()
            prop(Setting::id).isEqualTo(SendingMailSettingsId.QUOTE_MESSAGE_WHEN_REPLYING)
        }

        assertThat(settings[7]).all {
            isInstanceOf<SettingValue.Switch>()
            prop(Setting::id).isEqualTo(SendingMailSettingsId.REPLY_AFTER_QUOTED_TEXT)
        }

        assertThat(settings[8]).all {
            isInstanceOf<SettingValue.Switch>()
            prop(Setting::id).isEqualTo(SendingMailSettingsId.STRIP_SIGNATURE_ON_REPLY)
        }

        assertThat(settings[9]).all {
            isInstanceOf<SettingValue.Text>()
            prop(Setting::id).isEqualTo(SendingMailSettingsId.QUOTED_TEXT_PREFIX)
        }

        assertThat(settings[10]).all {
            isInstanceOf<SettingValue.Switch>()
            prop(Setting::id).isEqualTo(SendingMailSettingsId.UPLOAD_SENT_MESSAGES)
        }

        assertThat(settings[11]).all {
            isInstanceOf<SettingValue.ActionText>()
            prop(Setting::id).isEqualTo(SendingMailSettingsId.OUTGOING_SERVER)
        }
    }

    @Test
    fun `message format select should preserve selected value`() {
        val selected = SelectOption("HTML") { "HTML" }

        val settings = builder.buildSettings(
            createState(messageFormat = selected),
        ) {}

        val select = settings[2] as SettingValue.Select

        assertThat(select.value.id).isEqualTo(selected.id)
    }

    @Test
    fun `message format select should contain expected options`() {
        val settings = builder.buildSettings(createState()) {}
        val select = settings[2] as SettingValue.Select

        assertThat(select.options.map { it.id }).isEqualTo(
            listOf(
                "TEXT",
                "HTML",
                "AUTO",
            ),
        )
    }

    @Test
    fun `reply quoting style should preserve selected value`() {
        val selected = SelectOption("HEADER") { "Header" }

        val settings = builder.buildSettings(
            createState(replyQuotingStyle = selected),
        ) {}

        val select = settings[5] as SettingValue.Select

        assertThat(select.value.id).isEqualTo(selected.id)
    }

    @Test
    fun `reply quoting style should contain expected options`() {
        val settings = builder.buildSettings(createState()) {}
        val select = settings[5] as SettingValue.Select

        assertThat(select.options.map { it.id }).isEqualTo(
            listOf(
                "PREFIX",
                "HEADER",
            ),
        )
    }

    @Test
    fun `switch settings should preserve values`() {
        val settings = builder.buildSettings(
            createState(
                alwaysShowCcBcc = true,
                readReceipt = true,
                quoteMessageWhenReplying = true,
                replyAfterQuotedText = true,
                stripSignatureOnReply = true,
                uploadSentMessages = true,
            ),
        ) {}

        assertThat(
            (
                settings.first {
                    it.id == SendingMailSettingsId.ALWAYS_SHOW_CC_BCC
                } as SettingValue.Switch
                ).value,
        ).isTrue()

        assertThat(
            (
                settings.first {
                    it.id == SendingMailSettingsId.READ_RECEIPT
                } as SettingValue.Switch
                ).value,
        ).isTrue()

        assertThat(
            (
                settings.first {
                    it.id == SendingMailSettingsId.QUOTE_MESSAGE_WHEN_REPLYING
                } as SettingValue.Switch
                ).value,
        ).isTrue()

        assertThat(
            (
                settings.first {
                    it.id == SendingMailSettingsId.REPLY_AFTER_QUOTED_TEXT
                } as SettingValue.Switch
                ).value,
        ).isTrue()

        assertThat(
            (
                settings.first {
                    it.id == SendingMailSettingsId.STRIP_SIGNATURE_ON_REPLY
                } as SettingValue.Switch
                ).value,
        ).isTrue()

        assertThat(
            (
                settings.first {
                    it.id == SendingMailSettingsId.UPLOAD_SENT_MESSAGES
                } as SettingValue.Switch
                ).value,
        ).isTrue()
    }

    @Test
    fun `quote prefix should preserve value`() {
        val settings = builder.buildSettings(
            createState(quotedTextPrefix = "> "),
        ) {}

        val text = settings[9] as SettingValue.Text

        assertThat(text.value).isEqualTo("> ")
    }

    @Test
    fun `composition defaults should emit click event`() {
        var event: SendingMailSettingContract.Event? = null

        val settings = builder.buildSettings(createState()) {
            event = it
        }

        val setting = settings[0] as SettingValue.ActionText
        setting.onClick()

        assertThat(event).isEqualTo(
            SendingMailSettingContract.Event.OnCompositionDefaultsClick,
        )
    }

    @Test
    fun `manage identities should emit click event`() {
        var event: SendingMailSettingContract.Event? = null

        val settings = builder.buildSettings(createState()) {
            event = it
        }

        val setting = settings[1] as SettingValue.ActionText
        setting.onClick()

        assertThat(event).isEqualTo(
            SendingMailSettingContract.Event.OnManageIdentitiesClick,
        )
    }

    @Test
    fun `outgoing server should emit click event`() {
        var event: SendingMailSettingContract.Event? = null

        val settings = builder.buildSettings(createState()) {
            event = it
        }

        val setting = settings[11] as SettingValue.ActionText
        setting.onClick()

        assertThat(event).isEqualTo(
            SendingMailSettingContract.Event.OnOutgoingServerClick,
        )
    }

    private fun createState(
        messageFormat: SelectOption = SelectOption("TEXT") { "Plain Text" },
        alwaysShowCcBcc: Boolean = false,
        readReceipt: Boolean = false,
        replyQuotingStyle: SelectOption = SelectOption("PREFIX") { "Prefix" },
        quoteMessageWhenReplying: Boolean = false,
        replyAfterQuotedText: Boolean = false,
        stripSignatureOnReply: Boolean = false,
        quotedTextPrefix: String = "",
        uploadSentMessages: Boolean = false,
    ) = SendingMailSettingContract.State(
        subtitle = "",
        messageFormat = messageFormat,
        alwaysShowCcBcc = alwaysShowCcBcc,
        readReceipt = readReceipt,
        replyQuotingStyle = replyQuotingStyle,
        quoteMessageWhenReplying = quoteMessageWhenReplying,
        replyAfterQuotedText = replyAfterQuotedText,
        stripSignatureOnReply = stripSignatureOnReply,
        quotedTextPrefix = quotedTextPrefix,
        uploadSentMessages = uploadSentMessages,
    )
}
