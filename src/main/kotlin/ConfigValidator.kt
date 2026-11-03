package app

/**
 * Валидация текста settings.json против каталога SettingsCatalog.
 * Контракт эпика: ERROR дают только синтаксис и дубли (от парсера);
 * все семантические отклонения — WARNING (Claude Code их игнорирует или роняет точечно).
 * Контекст всегда проектный: приложение редактирует только проектные файлы и их бекапы.
 */

private val ENV_NAME = Regex("^[A-Za-z_][A-Za-z0-9_]*$")

fun validateConfig(text: String): List<Diagnostic> {
    val parsed = parseJson(text)
    if (!parsed.ok) return parsed.errors

    val root = parsed.root ?: return parsed.errors // ok=true ⇒ root не null; защита от нарушения инварианта
    if (root !is JsonNode.Obj) {
        return listOf(Diagnostic(Severity.WARNING, root.offset, root.length, "Конфиг должен быть JSON-объектом"))
    }

    val out = mutableListOf<Diagnostic>()
    fun warn(node: JsonNode, message: String) {
        out += Diagnostic(Severity.WARNING, node.offset, node.length, message)
    }

    for ((keyNode, value) in root.entries) {
        val name = keyNode.value
        val def = SettingsCatalog.top[name]
        if (def == null) {
            warn(keyNode, "Неизвестный ключ \"$name\" (отсутствует в документации Claude Code)")
            continue
        }
        def.deprecatedFor?.let {
            warn(keyNode, "Ключ \"$name\" устарел — используйте \"$it\"")
        }
        when (def.scope) {
            KeyScope.MANAGED_ONLY -> warn(keyNode, "Ключ \"$name\" действует только в managed-settings.json — здесь игнорируется")
            KeyScope.GLOBAL_CONFIG -> warn(keyNode, "Ключ \"$name\" живёт в ~/.claude.json — в settings.json игнорируется")
            KeyScope.USER_OR_MANAGED, KeyScope.USER_ONLY -> warn(keyNode, "Ключ \"$name\" не действует в проектных настройках (только пользовательский/managed файл)")
            KeyScope.ANY -> {}
        }
        typeMismatch(def, value)?.let { warn(value, it) }
        if (def.enumValues != null && value is JsonNode.Str && value.value !in def.enumValues) {
            warn(value, "Ключ \"$name\": недопустимое значение \"${value.value}\" (допустимо: ${def.enumValues.joinToString(", ")})")
        }

        val allowedNested = SettingsCatalog.strictObjects[name]
        if (value is JsonNode.Obj && allowedNested != null) {
            for ((nestedKey, nestedValue) in value.entries) {
                if (nestedKey.value !in allowedNested) {
                    warn(nestedKey, "Неизвестный параметр \"${nestedKey.value}\" в \"$name\"")
                }
                if (name == "permissions" && nestedKey.value == "defaultMode" && nestedValue is JsonNode.Str) {
                    val v = nestedValue.value
                    when {
                        v !in SettingsCatalog.defaultModeValues ->
                            warn(nestedValue, "defaultMode: недопустимое значение \"$v\" (допустимо: ${SettingsCatalog.defaultModeValues.joinToString(", ")})")
                        v == "auto" || v == "bypassPermissions" ->
                            warn(nestedValue, "defaultMode \"$v\" действует только из user/managed файлов")
                    }
                }
            }
        }

        if (name == "env" && value is JsonNode.Obj) {
            for ((envKey, envValue) in value.entries) {
                if (!ENV_NAME.matches(envKey.value)) warn(envKey, "Имя переменной окружения \"${envKey.value}\" недопустимо")
                if (envValue !is JsonNode.Str) warn(envValue, "Значение env должно быть строкой")
            }
        }
    }
    return out + parsed.warnings
}

private fun typeMismatch(def: KeyDef, value: JsonNode): String? {
    val ok = when (def.type) {
        ValType.BOOLEAN -> value is JsonNode.Bool
        ValType.STRING -> value is JsonNode.Str
        ValType.NUMBER -> value is JsonNode.Num
        ValType.OBJECT -> value is JsonNode.Obj
        ValType.STRING_LIST -> value is JsonNode.Arr && value.items.all { it is JsonNode.Str }
        ValType.ANY -> true
    }
    return if (ok) null else "Ключ \"${def.name}\": ожидался тип ${def.type}"
}
