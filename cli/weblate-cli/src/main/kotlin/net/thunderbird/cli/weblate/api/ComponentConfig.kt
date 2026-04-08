package net.thunderbird.cli.weblate.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the configuration of a Weblate component.
 *
 * This maps the shape of `golden-component-config.json` used by the CLI. Defaults are
 * provided so decoding remains tolerant when the server omits optional keys.
 *
 * @property license SPDX license identifier for the component
 * @property licenseUrl URL to the license text
 * @property agreement Any agreement text associated with the component
 * @property reportSourceBugs URL where users should report issues
 * @property priority Component priority (numeric)
 * @property isGlossary Whether this component is a glossary
 * @property glossaryColor Color used to render glossary items in the UI
 * @property enableSuggestions Whether suggestions are enabled
 * @property suggestionVoting Whether suggestion voting is enabled
 * @property suggestionAutoaccept Number of votes required for auto-accept (or 0)
 * @property allowTranslationPropagation Whether translation propagation is allowed
 * @property manageUnits Controls unit management behavior
 * @property checkFlags Miscellaneous check flags
 * @property variantRegex Regex used to identify language variants
 * @property enforcedChecks List of enforced check identifiers (e.g. "plurals")
 * @property secondaryLanguage Optional secondary language code
 * @property vcs Version control system identifier (e.g. "github")
 * @property repo Repository URL
 * @property branch Default branch for the component's repo
 * @property push Push URL or configuration
 * @property pushBranch Branch used for pushes
 * @property repoweb Web UI link pattern for sources
 * @property pushOnCommit Whether pushes should be triggered on commit
 * @property commitPendingAge Age (hours) before committing pending changes
 * @property mergeStyle Merge strategy for commits (e.g. "rebase")
 * @property autoLockError Whether to auto-lock component on errors
 * @property commitMessage Template used for commit messages
 * @property addMessage Template used when adding translations
 * @property deleteMessage Template used when deleting translations
 * @property mergeMessage Template used when merging
 * @property addonMessage Template used by add-ons
 * @property pullMessage Template used for pull updates
 * @property languageRegex Regex to validate language codes
 * @property keyFilter Optional key filter applied to strings
 * @property fileFormatParams Nested file-format specific parameters
 * @property editTemplate Whether edit templates are enabled
 * @property intermediate Intermediate file path or marker
 * @property newBase New base branch/file marker
 * @property newLang Default new language handling value
 * @property languageCodeStyle Style applied to language codes
 * @property screenshotFilemask File mask for screenshots
 */
@Serializable
data class ComponentConfig(
    @SerialName("license")
    val license: String = "",

    @SerialName("license_url")
    val licenseUrl: String = "",

    @SerialName("agreement")
    val agreement: String = "",

    @SerialName("report_source_bugs")
    val reportSourceBugs: String = "",

    @SerialName("priority")
    val priority: Int = 0,

    @SerialName("is_glossary")
    val isGlossary: Boolean = false,

    @SerialName("glossary_color")
    val glossaryColor: String = "",

    @SerialName("enable_suggestions")
    val enableSuggestions: Boolean = false,

    @SerialName("suggestion_voting")
    val suggestionVoting: Boolean = false,

    @SerialName("suggestion_autoaccept")
    val suggestionAutoaccept: Int = 0,

    @SerialName("allow_translation_propagation")
    val allowTranslationPropagation: Boolean = false,

    @SerialName("check_flags")
    val checkFlags: String = "",

    @SerialName("variant_regex")
    val variantRegex: String = "",

    @SerialName("enforced_checks")
    val enforcedChecks: List<String> = emptyList(),

    @SerialName("secondary_language")
    val secondaryLanguage: String? = null,

    @SerialName("vcs")
    val vcs: String = "",

    @SerialName("repo")
    val repo: String = "",

    @SerialName("branch")
    val branch: String = "",

    @SerialName("push")
    val push: String = "",

    @SerialName("push_branch")
    val pushBranch: String = "",

    @SerialName("repoweb")
    val repoweb: String = "",

    @SerialName("commit_pending_age")
    val commitPendingAge: Int = 0,

    @SerialName("merge_style")
    val mergeStyle: String = "",

    @SerialName("auto_lock_error")
    val autoLockError: Boolean = false,

    @SerialName("commit_message")
    val commitMessage: String = "",

    @SerialName("add_message")
    val addMessage: String = "",

    @SerialName("delete_message")
    val deleteMessage: String = "",

    @SerialName("merge_message")
    val mergeMessage: String = "",

    @SerialName("addon_message")
    val addonMessage: String = "",

    @SerialName("pull_message")
    val pullMessage: String = "",

    @SerialName("language_regex")
    val languageRegex: String = "",

    @SerialName("key_filter")
    val keyFilter: String = "",

    @SerialName("file_format_params")
    val fileFormatParams: FileFormatParams = FileFormatParams(),

    @SerialName("edit_template")
    val editTemplate: Boolean = false,

    @SerialName("intermediate")
    val intermediate: String = "",

    @SerialName("new_base")
    val newBase: String = "",

    @SerialName("new_lang")
    val newLang: String = "",

    @SerialName("language_code_style")
    val languageCodeStyle: String = "",

    @SerialName("screenshot_filemask")
    val screenshotFilemask: String = "",
)

@Serializable
data class FileFormatParams(
    @SerialName("xml_closing_tags")
    val xmlClosingTags: Boolean = false,
)
