package ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.JsonNode
import app.SettingsCatalog
import app.removeValue
import app.setValue
import app.valueAt

enum class EditorMode { JSON, FORM }

enum class FieldKind { SWITCH, TEXT, ENUM, STRING_LIST, MAP }

data class FormField(val path: List<String>, val label: String, val kind: FieldKind, val enumSource: List<String>? = null)

/** Секретные значения env: маскируются, показываются по кнопке. Расширение — новая строка. */
internal fun isSecretEnvKey(name: String): Boolean = name == "ANTHROPIC_AUTH_TOKEN"

/** Набор полей UI-режима v1; расширение — новая строка здесь, код формы не меняется. Порядок = порядок в UI. */
val formFields: List<FormField> = listOf(
    FormField(listOf("env"), "Переменные окружения", FieldKind.MAP),
    FormField(listOf("model"), "Модель", FieldKind.TEXT),
    FormField(listOf("effortLevel"), "Уровень усилий", FieldKind.ENUM, SettingsCatalog.top["effortLevel"]?.enumValues),
    FormField(listOf("permissions", "defaultMode"), "Режим разрешений", FieldKind.ENUM, SettingsCatalog.defaultModeValues),
    FormField(listOf("permissions", "allow"), "Разрешения: всегда разрешать", FieldKind.STRING_LIST),
    FormField(listOf("permissions", "deny"), "Разрешения: запрещать", FieldKind.STRING_LIST),
    FormField(listOf("permissions", "ask"), "Разрешения: спрашивать", FieldKind.STRING_LIST),
    FormField(listOf("claudeMdExcludes"), "Исключения CLAUDE.md", FieldKind.STRING_LIST),
    FormField(listOf("verbose"), "Подробный вывод", FieldKind.SWITCH),
    FormField(listOf("includeGitInstructions"), "Git-инструкции в контексте", FieldKind.SWITCH),
    FormField(listOf("respectGitignore"), "Учитывать .gitignore", FieldKind.SWITCH),
    FormField(listOf("autoScrollEnabled"), "Автопрокрутка", FieldKind.SWITCH),
    FormField(listOf("\$schema"), "\$schema (схема JSON)", FieldKind.TEXT),
)

/**
 * Форма поверх дерева конфига: правит только известные поля через ConfigTree,
 * unknown-ключи не трогает; сериализацией в текст занимается вызывающий (onChange).
 */
@Composable
fun ConfigForm(tree: JsonNode.Obj, onChange: (JsonNode.Obj) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        formFields.forEach { field ->
            Field(field, tree, onChange)
        }
    }
}

@Composable
private fun Field(field: FormField, tree: JsonNode.Obj, onChange: (JsonNode.Obj) -> Unit) {
    val collapsible = field.kind == FieldKind.STRING_LIST || field.kind == FieldKind.MAP
    var expanded by remember(field.path.joinToString(".")) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(field.label, color = Theme.text, fontSize = 13.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                field.path.joinToString("."),
                color = Theme.textDim, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
            if (collapsible) {
                if (!expanded) {
                    val count = when (val v = valueAt(tree, field.path)) {
                        is JsonNode.Arr -> v.items.size
                        is JsonNode.Obj -> v.entries.size
                        else -> 0
                    }
                    if (count > 0) Text("($count)", color = Theme.textDim, fontSize = 11.sp)
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    if (expanded) "−" else "+",
                    color = Theme.accent, fontSize = 13.sp,
                    modifier = Modifier.hoverClick(Theme.chipShape) { expanded = !expanded }
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
        if (!collapsible || expanded) {
        Spacer(Modifier.height(4.dp))
        val fieldTextStyle = TextStyle(color = Theme.text, fontSize = 13.sp)
        when (field.kind) {
            FieldKind.SWITCH -> {
                val checked = (valueAt(tree, field.path) as? JsonNode.Bool)?.value == true
                Switch(
                    checked = checked,
                    onCheckedChange = { on ->
                        onChange(
                            if (on) setValue(tree, field.path, JsonNode.Bool(true, -1, 4))
                            else removeValue(tree, field.path),
                        )
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Theme.accent,
                        checkedThumbColor = Theme.text,
                        uncheckedTrackColor = Theme.stroke,
                        uncheckedThumbColor = Theme.textDim,
                    ),
                )
            }
            FieldKind.TEXT -> {
                val value = (valueAt(tree, field.path) as? JsonNode.Str)?.value ?: ""
                OutlinedTextField(
                    value = value,
                    onValueChange = { onChange(setValue(tree, field.path, JsonNode.Str(it, -1, it.length + 2))) },
                    singleLine = true,
                    textStyle = fieldTextStyle,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FieldKind.ENUM -> {
                val current = (valueAt(tree, field.path) as? JsonNode.Str)?.value
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    field.enumSource.orEmpty().forEach { v ->
                        val selected = v == current
                        Box(
                            Modifier
                                .background(if (selected) Theme.selectedGradient else Theme.clearGradient, Theme.chipShape)
                                .hoverClick(Theme.chipShape) {
                                    onChange(
                                        if (selected) removeValue(tree, field.path)
                                        else setValue(tree, field.path, JsonNode.Str(v, -1, v.length + 2)),
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(v, color = if (selected) Theme.accent2 else Theme.textDim, fontSize = 12.sp)
                        }
                    }
                }
            }
            FieldKind.STRING_LIST -> {
                val items = (valueAt(tree, field.path) as? JsonNode.Arr)?.items
                    ?.map { (it as? JsonNode.Str)?.value ?: "" } ?: emptyList()
                fun commit(newItems: List<String>) = onChange(
                    if (newItems.isEmpty()) removeValue(tree, field.path)
                    else setValue(tree, field.path, JsonNode.Arr(newItems.map { JsonNode.Str(it, -1, it.length + 2) }, -1, 2)),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items.forEachIndexed { i, item ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = item,
                                onValueChange = { v -> commit(items.toMutableList().also { l -> l[i] = v }) },
                                singleLine = true,
                                textStyle = fieldTextStyle,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "✕",
                                color = Theme.textDim, fontSize = 12.sp,
                                modifier = Modifier.hoverClick(Theme.chipShape) { commit(items.filterIndexed { idx, _ -> idx != i }) }
                                    .padding(4.dp),
                            )
                        }
                    }
                    Text(
                        "+ Добавить",
                        color = Theme.accent, fontSize = 12.sp,
                        modifier = Modifier.hoverClick(Theme.chipShape) { commit(items + "") }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
            FieldKind.MAP -> {
                val entries = (valueAt(tree, field.path) as? JsonNode.Obj)?.entries
                    ?.map { it.first.value to ((it.second as? JsonNode.Str)?.value ?: (it.second as? JsonNode.Num)?.raw ?: "") }
                    ?: emptyList()
                fun commit(pairs: List<Pair<String, String>>) = onChange(
                    if (pairs.isEmpty()) removeValue(tree, field.path)
                    else setValue(
                        tree, field.path,
                        JsonNode.Obj(pairs.map { (k, v) -> JsonNode.Str(k, -1, k.length + 2) to JsonNode.Str(v, -1, v.length + 2) }, -1, 2),
                    ),
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    entries.forEachIndexed { i, pair ->
                        val (k, v) = pair
                        val secret = isSecretEnvKey(k)
                        var tokenVisible by remember(k) { mutableStateOf(false) }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = k,
                                onValueChange = { nk -> commit(entries.toMutableList().also { l -> l[i] = nk to l[i].second }) },
                                singleLine = true,
                                textStyle = fieldTextStyle,
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedTextField(
                                value = v,
                                onValueChange = { nv -> commit(entries.toMutableList().also { l -> l[i] = l[i].first to nv }) },
                                singleLine = true,
                                textStyle = fieldTextStyle,
                                visualTransformation = if (secret && !tokenVisible) PasswordVisualTransformation() else VisualTransformation.None,
                                trailingIcon = if (secret) {
                                    {
                                        Text(
                                            if (tokenVisible) "Скрыть" else "Показать",
                                            color = Theme.accent, fontSize = 11.sp,
                                            modifier = Modifier.hoverClick(Theme.chipShape) { tokenVisible = !tokenVisible }
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                } else null,
                                modifier = Modifier.weight(1.4f),
                            )
                            Text(
                                "✕",
                                color = Theme.textDim, fontSize = 12.sp,
                                modifier = Modifier.hoverClick(Theme.chipShape) { commit(entries.filterIndexed { idx, _ -> idx != i }) }
                                    .padding(4.dp),
                            )
                        }
                    }
                    Text(
                        "+ Добавить",
                        color = Theme.accent, fontSize = 12.sp,
                        modifier = Modifier.hoverClick(Theme.chipShape) { commit(entries + ("" to "")) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
        }
    }
}

/** Сегмент-переключатель режимов редактора; неактивный — приглушён и не кликается. */
@Composable
fun ModeSegment(text: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .background(if (selected) Theme.selectedGradient else Theme.clearGradient, Theme.chipShape)
            .then(if (enabled) Modifier.hoverClick(Theme.chipShape, onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            color = when {
                !enabled -> Theme.textDim.copy(alpha = 0.5f)
                selected -> Theme.accent2
                else -> Theme.textDim
            },
            fontSize = 12.sp,
        )
    }
}
