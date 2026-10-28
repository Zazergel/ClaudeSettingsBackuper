package app

/**
 * Каталог ключей settings.json по официальной документации Claude Code
 * (https://code.claude.com/docs/ru/settings-reference, индекс «Все параметры»).
 * Источник правды — docs-индекс (~190 ключей), НЕ schemastore-схема (~90, официально отстаёт).
 * Тип ANY = тип не описан в схеме — проверка типа пропускается (ложное срабатывание хуже пропуска).
 * Каталог также обслуживает UI-форму редактора.
 */

enum class ValType { BOOLEAN, STRING, NUMBER, STRING_LIST, OBJECT, ANY }

enum class KeyScope { ANY, USER_OR_MANAGED, USER_ONLY, MANAGED_ONLY, GLOBAL_CONFIG }

data class KeyDef(
    val name: String,
    val type: ValType = ValType.ANY,
    val scope: KeyScope = KeyScope.ANY,
    val enumValues: List<String>? = null,
    val deprecatedFor: String? = null,
)

object SettingsCatalog {

    private fun k(
        name: String,
        type: ValType = ValType.ANY,
        scope: KeyScope = KeyScope.ANY,
        enum: List<String>? = null,
        deprecated: String? = null,
    ) = KeyDef(name, type, scope, enum, deprecated)

    val top: Map<String, KeyDef> = listOf(
        // Модель и ответы
        k("advisorModel", ValType.STRING),
        k("alwaysThinkingEnabled", ValType.BOOLEAN),
        k("availableModels", ValType.STRING_LIST),
        k("availableModelsMatch", scope = KeyScope.MANAGED_ONLY, enum = listOf("prefix", "exact")),
        k("deniedModels", ValType.STRING_LIST, KeyScope.MANAGED_ONLY),
        k("effortLevel", ValType.STRING, enum = listOf("low", "medium", "high", "xhigh")),
        k("enforceAvailableModels"),
        k("fallbackModel"),
        k("fastMode", ValType.BOOLEAN),
        k("fastModePerSessionOptIn", ValType.BOOLEAN),
        k("language", ValType.STRING),
        k("maxEffortLevel"),
        k("model", ValType.STRING),
        k("modelOverrides", ValType.OBJECT),
        k("modelPicker", scope = KeyScope.USER_OR_MANAGED),
        k("modelPricing", scope = KeyScope.MANAGED_ONLY),
        k("modelSettings", ValType.OBJECT),
        k("outputStyle", ValType.STRING),
        k("promptCacheTtl"),
        k("showThinkingSummaries"),
        k("subagentPromptCacheTtl"),
        k("switchModelsOnFlag"),
        k("ultracode"),
        // Разрешения
        k("allowManagedPermissionRulesOnly", ValType.BOOLEAN, KeyScope.MANAGED_ONLY),
        k("autoMode", ValType.OBJECT, KeyScope.USER_OR_MANAGED),
        k("disableAutoMode", ValType.STRING, enum = listOf("disable")),
        k("permissions", ValType.OBJECT),
        k("skipAutoPermissionPrompt", scope = KeyScope.USER_OR_MANAGED),
        k("skipDangerousModePermissionPrompt", scope = KeyScope.USER_OR_MANAGED),
        k("useAutoModeDuringPlan", scope = KeyScope.USER_OR_MANAGED),
        // Песочница
        k("sandbox", ValType.OBJECT),
        // Память и контекст
        k("autoCompactEnabled"),
        k("autoCompactWindow"),
        k("autoMemoryDirectory"),
        k("autoMemoryEnabled", ValType.BOOLEAN),
        k("bashOutputMaxChars"),
        k("claudeMd", ValType.STRING, KeyScope.MANAGED_ONLY),
        k("claudeMdExcludes", ValType.STRING_LIST),
        k("env", ValType.OBJECT),
        k("fileCheckpointingEnabled"),
        k("idleCompaction"),
        k("plansDirectory", ValType.STRING),
        k("skillListingBudgetFraction"),
        k("skillListingMaxDescChars"),
        k("taskOutputMaxChars", deprecated = "удалён в v2.1.277 — удалите ключ"),
        // Интерфейс и терминал
        k("askUserQuestionTimeout", ValType.STRING, KeyScope.USER_OR_MANAGED, listOf("60s", "5m", "10m", "never")),
        k("autoContinueAtUsageLimit", scope = KeyScope.USER_OR_MANAGED),
        k("autoScrollEnabled", ValType.BOOLEAN),
        k("axScreenReader", ValType.BOOLEAN, KeyScope.USER_OR_MANAGED),
        k("bashEditDiffEnabled", scope = KeyScope.USER_OR_MANAGED),
        k("companyAnnouncements", ValType.STRING_LIST),
        k("defaultShell"),
        k("dialogExpiry", scope = KeyScope.USER_OR_MANAGED),
        k("editorMode"),
        k("emojiCompletionEnabled", ValType.BOOLEAN),
        k("fileSuggestion", ValType.OBJECT),
        k("footerLinksRegexes", scope = KeyScope.USER_OR_MANAGED),
        k("keybindingFlavor", deprecated = "не имеет эффекта — удалите ключ"),
        k("maxProseWidth"),
        k("prefersReducedMotion", ValType.BOOLEAN),
        k("promptSuggestionEnabled"),
        k("respectGitignore", ValType.BOOLEAN),
        k("respondToBashCommands"),
        k("showClearContextOnPlanAccept"),
        k("showTurnDuration", ValType.BOOLEAN),
        k("spellcheck", scope = KeyScope.USER_OR_MANAGED),
        k("spinnerTipsEnabled", ValType.BOOLEAN),
        k("spinnerTipsOverride", ValType.OBJECT),
        k("spinnerVerbs", ValType.OBJECT),
        k("statusLine", ValType.OBJECT),
        k("subagentStatusLine", ValType.OBJECT),
        k("syntaxHighlightingDisabled", ValType.BOOLEAN),
        k("terminalProgressBarEnabled", ValType.BOOLEAN),
        k("terminalTitleFromRename"),
        k("theme", ValType.STRING),
        k("timeFormat"),
        k("timeZone"),
        k("tui", enum = listOf("fullscreen", "classic")),
        k("verbose", ValType.BOOLEAN),
        k("viewMode", enum = listOf("default", "verbose", "focused")),
        k("vimInsertModeRemaps", ValType.OBJECT, KeyScope.USER_OR_MANAGED),
        k("voice"),
        k("voiceEnabled", ValType.BOOLEAN),
        k("wheelScrollAccelerationEnabled"),
        // Git и атрибуция
        k("attribution", ValType.OBJECT),
        k("includeCoAuthoredBy", ValType.BOOLEAN, deprecated = "attribution"),
        k("includeGitInstructions", ValType.BOOLEAN),
        k("prUrlTemplate", ValType.STRING),
        // Хуки и автоматизация
        k("allowedHttpHookUrls", ValType.STRING_LIST),
        k("allowManagedHooksOnly", ValType.BOOLEAN, KeyScope.MANAGED_ONLY),
        k("disableAllHooks", ValType.BOOLEAN),
        k("disableWorkflows", ValType.BOOLEAN),
        k("enableWorkflows"),
        k("hooks", ValType.OBJECT),
        k("httpHookAllowedEnvVars", ValType.STRING_LIST),
        k("workflowKeywordTriggerEnabled", ValType.BOOLEAN),
        k("workflowSizeGuideline", enum = listOf("unrestricted", "small", "medium", "large")),
        // Плагины и скиллы
        k("disableBundledSkills"),
        k("disableSkillShellExecution"),
        k("skillOverrides", ValType.OBJECT),
        k("syncClaudeAiSkills", scope = KeyScope.USER_OR_MANAGED),
        k("syncClaudeAiPlugins", scope = KeyScope.USER_OR_MANAGED),
        k("allowedChannelPlugins", scope = KeyScope.MANAGED_ONLY),
        k("blockedMarketplaces", scope = KeyScope.MANAGED_ONLY),
        k("channelsEnabled", scope = KeyScope.MANAGED_ONLY),
        k("disableCommandPluginSources", scope = KeyScope.MANAGED_ONLY),
        k("pluginSuggestionMarketplaces", scope = KeyScope.MANAGED_ONLY),
        k("pluginTrustMessage", ValType.STRING, KeyScope.MANAGED_ONLY),
        k("strictKnownMarketplaces", scope = KeyScope.MANAGED_ONLY),
        k("strictPluginOnlyCustomization", ValType.OBJECT, KeyScope.MANAGED_ONLY),
        k("enabledPlugins"), // bool или map plugin@marketplace → bool/list
        k("extraKnownMarketplaces", ValType.OBJECT),
        k("pluginConfigs", ValType.OBJECT, KeyScope.USER_OR_MANAGED),
        k("prependPlugins", scope = KeyScope.USER_OR_MANAGED),
        k("appendPlugins", scope = KeyScope.USER_OR_MANAGED),
        k("skippedMarketplaces", ValType.STRING_LIST),
        k("skippedPlugins", ValType.STRING_LIST),
        // MCP
        k("allowAllClaudeAiMcps", ValType.BOOLEAN, KeyScope.MANAGED_ONLY),
        k("allowClaudeInChromeWithManagedMcp", scope = KeyScope.MANAGED_ONLY),
        k("allowedMcpServers"),
        k("allowManagedMcpServersOnly", ValType.BOOLEAN, KeyScope.MANAGED_ONLY),
        k("deniedMcpServers"),
        k("disableClaudeAiConnectors", ValType.BOOLEAN),
        k("disabledMcpjsonServers", ValType.STRING_LIST),
        k("enableAllProjectMcpServers", ValType.BOOLEAN),
        k("enabledMcpjsonServers", ValType.STRING_LIST),
        k("managedMcpServers", scope = KeyScope.MANAGED_ONLY),
        // Агенты, сессии, worktrees
        k("agent"),
        k("crossSessionInbound"),
        k("disableAgentView"),
        k("isolatePeerMachines"),
        k("processWrapper", ValType.STRING, KeyScope.USER_OR_MANAGED),
        k("teammateMode", enum = listOf("auto", "in-process", "tmux", "iterm2")),
        k("worktree", ValType.OBJECT),
        // Remote, desktop, уведомления
        k("agentPushNotifEnabled", ValType.BOOLEAN),
        k("awaySummaryEnabled", ValType.BOOLEAN),
        k("disableArtifact", ValType.BOOLEAN, deprecated = "enableArtifact"),
        k("disableDeepLinkRegistration"),
        k("disableDesktopLocalSessions", scope = KeyScope.MANAGED_ONLY),
        k("disableRemoteControl", ValType.BOOLEAN),
        k("enableArtifact", ValType.BOOLEAN, KeyScope.USER_OR_MANAGED), // project/local не могут
        k("inputNeededNotifEnabled", ValType.BOOLEAN),
        k("preferredNotifChannel"),
        k("remote", ValType.OBJECT),
        k("remoteControlAtStartup", ValType.BOOLEAN),
        k("sshConfigs", scope = KeyScope.USER_OR_MANAGED),
        k("sshHostAllowlist", scope = KeyScope.MANAGED_ONLY),
        // Аутентификация и провайдеры
        k("allowedProviders", scope = KeyScope.MANAGED_ONLY),
        k("apiKeyHelper", ValType.STRING),
        k("awsAuthRefresh", ValType.STRING),
        k("awsCredentialExport", ValType.STRING),
        k("forceLoginMethod", ValType.STRING, enum = listOf("claudeai", "console", "gateway")),
        k("forceLoginGatewayUrl", ValType.STRING, KeyScope.USER_OR_MANAGED),
        k("forceLoginOrgUUID"), // string или string[]
        k("gatewayInternalNetworks", scope = KeyScope.MANAGED_ONLY),
        k("gcpAuthRefresh"),
        k("otelHeadersHelper", ValType.STRING),
        // Обновления
        k("autoUpdatesChannel", enum = listOf("stable", "latest")),
        k("minimumVersion"),
        k("requiredMaximumVersion", scope = KeyScope.MANAGED_ONLY),
        k("requiredMinimumVersion", scope = KeyScope.MANAGED_ONLY),
        // Инструменты desktop
        k("browserExternalPageTools", scope = KeyScope.MANAGED_ONLY, enum = listOf("disabled")),
        k("disableBrowserExternalNavigation", ValType.BOOLEAN, KeyScope.MANAGED_ONLY),
        k("disableMobileSimulatorTools", ValType.BOOLEAN, KeyScope.MANAGED_ONLY),
        // Приватность и телеметрия
        k("cleanupPeriodDays", ValType.NUMBER),
        k("desktopSessionCleanupPeriodDays", scope = KeyScope.USER_OR_MANAGED),
        k("feedbackDrafts", scope = KeyScope.USER_OR_MANAGED),
        k("feedbackSurveyRate", ValType.NUMBER),
        k("skipWebFetchPreflight", ValType.BOOLEAN),
        // Enterprise/managed
        k("disableSideloadFlags", ValType.BOOLEAN, KeyScope.MANAGED_ONLY),
        k("forceRemoteSettingsRefresh", scope = KeyScope.MANAGED_ONLY),
        k("managedSourcesBehavior", scope = KeyScope.MANAGED_ONLY),
        k("parentSettingsBehavior", scope = KeyScope.MANAGED_ONLY, enum = listOf("first-wins", "merge")),
        k("policyHelper", ValType.OBJECT, KeyScope.MANAGED_ONLY),
        k("wslInheritsWindowsSettings", scope = KeyScope.MANAGED_ONLY),
        k("requireCoworkFullVmSandbox", ValType.BOOLEAN, KeyScope.MANAGED_ONLY),
        // Global config — живут в ~/.claude.json, в settings.json игнорируются
        k("autoConnectIde", scope = KeyScope.GLOBAL_CONFIG),
        k("autoInstallIdeExtension", scope = KeyScope.GLOBAL_CONFIG),
        k("claudeInChromeDefaultEnabled", scope = KeyScope.GLOBAL_CONFIG),
        k("copyFullResponse", scope = KeyScope.GLOBAL_CONFIG),
        k("copyOnSelect", scope = KeyScope.GLOBAL_CONFIG),
        k("defaultToAgentsView", scope = KeyScope.GLOBAL_CONFIG),
        k("diffTool", scope = KeyScope.GLOBAL_CONFIG, enum = listOf("auto", "terminal")),
        k("externalEditorContext", scope = KeyScope.GLOBAL_CONFIG),
        k("leftArrowOpensAgentsView", scope = KeyScope.GLOBAL_CONFIG),
        k("permissionExplainerEnabled", scope = KeyScope.GLOBAL_CONFIG, deprecated = "удалён в v2.1.257 — удалите ключ"),
        k("prStatusFooterEnabled", scope = KeyScope.GLOBAL_CONFIG),
        k("teammateDefaultModel", scope = KeyScope.GLOBAL_CONFIG, deprecated = "удалён в v2.1.234 — удалите ключ"),
        // Мета
        k("\$schema", ValType.STRING),
    ).associateBy { it.name }

    /** Объекты со строгим набором вложенных ключей (additionalProperties: false в схеме). */
    val strictObjects: Map<String, Set<String>> = mapOf(
        "permissions" to setOf(
            "allow", "ask", "deny", "additionalDirectories", "defaultMode",
            "disableBypassPermissionsMode", "disableAutoMode", "blockReadsOutsideWorkingDirectories",
        ),
        "statusLine" to setOf("type", "command", "padding", "refreshInterval", "hideVimModeIndicator"),
        "subagentStatusLine" to setOf("type", "command", "padding", "refreshInterval", "hideVimModeIndicator"),
        "fileSuggestion" to setOf("type", "command"),
        "attribution" to setOf("commit", "pr", "sessionUrl"),
        "worktree" to setOf("baseRef", "bgIsolation", "location", "sparsePaths", "symlinkDirectories"),
        "spinnerVerbs" to setOf("mode", "verbs"),
        "spinnerTipsOverride" to setOf("tips", "excludeDefault"),
        "sandbox" to setOf(
            "enabled", "failIfUnavailable", "autoAllowBashIfSandboxed", "excludedCommands",
            "allowUnsandboxedCommands", "allowAppleEvents", "allowNestedUserNamespaces",
            "enableWeakerNestedSandbox", "enableWeakerNetworkIsolation", "ripgrep", "bwrapPath",
            "socatPath", "enabledPlatforms", "ignoreViolations", "filesystem", "network", "credentials",
        ),
        "hooks" to setOf(
            "PreToolUse", "PostToolUse", "PostToolUseFailure", "PostToolBatch", "PermissionRequest",
            "PermissionDenied", "Notification", "UserPromptSubmit", "UserPromptExpansion", "Stop",
            "StopFailure", "SubagentStart", "SubagentStop", "PreCompact", "PostCompact", "SessionStart",
            "SessionEnd", "Setup", "InstructionsLoaded", "CwdChanged", "FileChanged", "ConfigChange",
            "WorktreeCreate", "WorktreeRemove", "Elicitation", "ElicitationResult", "TeammateIdle",
            "TaskCompleted", "TaskCreated", "DirectoryAdded", "MessageDisplay",
        ),
    )

    val defaultModeValues = listOf(
        "default", "manual", "acceptEdits", "plan", "auto", "dontAsk", "bypassPermissions", "delegate",
    )
}
