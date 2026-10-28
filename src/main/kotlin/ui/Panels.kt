package ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import app.Diagnostic
import app.JsonNode
import app.Prefs
import app.Project
import app.Severity
import app.parseJson
import app.validateConfig
import app.writeJson
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Current state of a project's settings.json as shown in the details panel. */
data class CurrentInfo(val exists: Boolean, val size: Long, val mtime: LocalDateTime)

private val MTIME_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")

@Composable
fun RootsBar(roots: List<Path>, onAdd: () -> Unit, onRemove: (Path) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Папки сканирования:", color = Theme.textDim, fontSize = 13.sp)
        Row(
            Modifier.weight(1f).horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            roots.forEach { root ->
                Row(
                    Modifier
                        .background(Theme.card, Theme.chipShape)
                        .padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(root.toString(), color = Theme.text, fontSize = 12.sp, maxLines = 1)
                    Text(
                        "  ✕",
                        color = Theme.textDim,
                        fontSize = 12.sp,
                        modifier = Modifier.hoverClick(Theme.chipShape) { onRemove(root) }.padding(2.dp),
                    )
                }
            }
        }
        Text(
            "+ Добавить",
            color = Theme.accent, fontSize = 13.sp,
            modifier = Modifier.hoverClick(Theme.chipShape) { onAdd() }.padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}

/** Конфликт снапшота: конфиг изменён извне; hasBackup — доступен ли последний бекап для восстановления. */
data class SnapshotConflict(val project: Project, val hasBackup: Boolean)

/** Состояние конфигов после запуска: изменённые извне (с действиями) и невалидные (без действий). */
@Composable
fun SnapshotBanner(
    conflicts: List<SnapshotConflict>,
    invalid: List<Project>,
    onShowDiff: (Project) -> Unit,
    onAccept: (Project) -> Unit,
    onRestore: (Project) -> Unit,
) {
    if (conflicts.isEmpty() && invalid.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        conflicts.forEach { c ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Theme.card, Theme.chipShape)
                    .padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "⚠ «${c.project.root.fileName}»: конфиг изменён извне",
                    color = Theme.warn, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                GradientButton("Изменения", onClick = { onShowDiff(c.project) })
                Spacer(Modifier.width(6.dp))
                GradientButton("Принять", onClick = { onAccept(c.project) })
                Spacer(Modifier.width(6.dp))
                GradientButton("Восстановить", enabled = c.hasBackup, onClick = { onRestore(c.project) })
            }
        }
        invalid.forEach { p ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Theme.card, Theme.chipShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "✕ «${p.root.fileName}»: settings.json — невалидный JSON, исправьте в редакторе",
                    color = Theme.danger, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun ProjectList(
    projects: List<Project>,
    selected: Project?,
    scanning: Boolean,
    onSelect: (Project) -> Unit,
    onHide: (Project) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Card(modifier, shape = Theme.cardShape, colors = CardDefaults.cardColors(containerColor = Theme.card)) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Проекты (${projects.size})", color = Theme.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (scanning) CircularProgressIndicator(Modifier.size(16.dp), color = Theme.accent, strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(8.dp))
            if (projects.isEmpty()) {
                Text(
                    if (scanning) "Сканирую" else "Проекты не найдены. Добавьте папку сканирования.",
                    color = Theme.textDim, fontSize = 13.sp,
                )
            } else {
                val listState = rememberLazyListState()
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                    items(projects, key = { it.root.toString() }) { project ->
                        val isSelected = project == selected
                        val rowSource = remember { MutableInteractionSource() }
                        val rowHovered by rowSource.collectIsHoveredAsState()
                        val rowAlpha by animateFloatAsState(if (rowHovered) 0.10f else 0f, tween(120))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(Theme.chipShape)
                                .background(if (isSelected) Theme.selectedGradient else Theme.clearGradient)
                                .hoverable(rowSource)
                                .clickable(interactionSource = rowSource, indication = null) { onSelect(project) }
                                .drawWithContent {
                                    drawContent()
                                    if (rowAlpha > 0f) drawRect(Color.White.copy(alpha = rowAlpha))
                                }
                                .padding(8.dp),
                        ) {
                            Column(Modifier.widthIn(max = 240.dp)) {
                                Text(
                                    project.root.fileName.toString(),
                                    color = if (isSelected) Theme.accent2 else Theme.text,
                                    fontSize = 13.sp, fontWeight = FontWeight.Medium,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    project.root.parent?.toString() ?: "",
                                    color = Theme.textDim, fontSize = 11.sp,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (rowHovered) {
                                val hideSource = remember { MutableInteractionSource() }
                                Text(
                                    "Скрыть",
                                    color = Theme.textDim, fontSize = 11.sp,
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .clip(Theme.chipShape)
                                        .clickable(interactionSource = hideSource, indication = null) { onHide(project) }
                                        .padding(4.dp),
                                )
                            }
                        }
                    }
                    }
                    VerticalScrollbar(
                        rememberScrollbarAdapter(listState),
                        Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                        style = AppScrollbarStyle(),
                    )
                }
            }
        }
    }
}

@Composable
fun DetailsPanel(
    project: Project?,
    current: CurrentInfo?,
    backups: List<Path>,
    selectedBackup: Path?,
    onBackup: (String) -> Unit,
    onSelectBackup: (Path?) -> Unit,
    onRestore: () -> Unit,
    onEdit: (Path) -> Unit,
    onDelete: (Path) -> Unit,
    modifier: Modifier = Modifier,
) {
    var label by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Path?>(null) }
    Card(modifier, shape = Theme.cardShape, colors = CardDefaults.cardColors(containerColor = Theme.card)) {
        val scrollState = rememberScrollState()
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp)) {
                if (project == null) {
                    Text("Выберите проект слева", color = Theme.textDim, fontSize = 14.sp)
                    return@Column
                }
                Text(
                    project.root.fileName.toString(),
                    color = Theme.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        project.settings.toString(),
                        color = Theme.textDim, fontSize = 12.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    GradientButton("Изменить конфиг") { onEdit(project.settings) }
                }
                Spacer(Modifier.height(12.dp))

                if (current?.exists == true) {
                    Text(
                        "Текущий файл: " + "%,d".format(current.size) + " Б, изменён " + current.mtime.format(MTIME_FMT),
                        color = Theme.ok, fontSize = 13.sp,
                    )
                } else {
                    Text("settings.json отсутствует", color = Theme.warn, fontSize = 13.sp)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        placeholder = { Text("Пометка (необязательно)", color = Theme.textDim, fontSize = 12.sp) },
                        singleLine = true,
                        textStyle = TextStyle(color = Theme.text, fontSize = 13.sp),
                        modifier = Modifier.weight(1f),
                    )
                    GradientButton("Создать бекап", enabled = current?.exists == true) { onBackup(label); label = "" }
                }
                Spacer(Modifier.height(16.dp))
                Text("Бекапы (" + backups.size + ")", color = Theme.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                if (backups.isEmpty()) {
                    Text("Бекапов пока нет", color = Theme.textDim, fontSize = 13.sp)
                }
                backups.forEach { backup ->
                    val isSelected = backup == selectedBackup
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) Theme.selectedGradient else Theme.clearGradient,
                                Theme.chipShape,
                            )
                            .hoverClick(Theme.chipShape) { onSelectBackup(if (isSelected) null else backup) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            backup.fileName.toString().removePrefix("settings.json."),
                            color = if (isSelected) Theme.accent2 else Theme.textDim,
                            fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (isSelected) {
                            GradientButton("Восстановить", onClick = onRestore)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Изменить",
                                color = Theme.accent, fontSize = 12.sp,
                                modifier = Modifier.hoverClick(Theme.chipShape) { onEdit(backup) }
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Удалить",
                                color = Theme.danger, fontSize = 12.sp,
                                modifier = Modifier.hoverClick(Theme.chipShape) { pendingDelete = backup }
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
            VerticalScrollbar(
                rememberScrollbarAdapter(scrollState),
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
                style = AppScrollbarStyle(),
            )
        }
    }
    pendingDelete?.let { backup ->
        DeleteConfirmDialog(
            backup = backup,
            onConfirm = { onDelete(backup); pendingDelete = null },
            onClose = { pendingDelete = null },
        )
    }
}

/** Scrollbar visible on the dark background: dim thumb at rest, orange on hover. */
@Composable
fun AppScrollbarStyle(): ScrollbarStyle = ScrollbarStyle(
    unhoverColor = Theme.textDim.copy(alpha = 0.45f),
    hoverColor = Theme.accent,
    thickness = 8.dp,
    minimalHeight = 24.dp,
    shape = Theme.buttonShape,
    hoverDurationMillis = 300,
)

/** Irreversible action guard: deleting a backup loses that copy forever. */
@Composable
fun DeleteConfirmDialog(backup: Path, onConfirm: () -> Unit, onClose: () -> Unit) {
    DialogWindow(onCloseRequest = onClose, title = "Удалить бекап", resizable = false) {
        Box(Modifier.fillMaxSize().background(Theme.bg1)) {
            Card(shape = Theme.cardShape, colors = CardDefaults.cardColors(containerColor = Theme.card)) {
                Column(Modifier.padding(20.dp).width(320.dp)) {
                    Text("Удалить бекап?", color = Theme.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        backup.fileName.toString(),
                        color = Theme.warn, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(14.dp))
                    Row {
                        GradientButton("Удалить", onClick = onConfirm)
                        Spacer(Modifier.width(8.dp))
                        GradientButton("Отмена", onClick = onClose)
                    }
                }
            }
        }
    }
}

/** Editor window for one file: Save needs a dirty document and zero ERROR diagnostics. */
@Composable
fun EditDialog(path: Path, onSave: (Path, String) -> Unit, onClose: () -> Unit) {
    var text by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf("") }
    var revealLine by remember { mutableStateOf<Int?>(null) }
    var revealTick by remember { mutableStateOf(0) }
    var mode by remember { mutableStateOf(EditorMode.FORM) } // UI-режим по умолчанию
    // «Скрыть» — на сессию диалога, «Доверять» — навсегда (Prefs). ID доверия — точный текст предупреждения:
    // у валидатора нет стабильных ключей. ponytail: смена формулировок сбрасывает доверие, одинаковые
    // дубликаты доверятся вместе, бюджет Prefs ~8КБ — переезд в файл, если перерастём
    val hidden = remember { mutableStateSetOf<String>() }
    val trusted = remember { mutableStateSetOf<String>().apply { addAll(Prefs.loadTrustedWarnings()) } }
    var showHidden by remember { mutableStateOf(false) }
    var showTrusted by remember { mutableStateOf(false) }
    LaunchedEffect(path) {
        text = runCatching { Files.readString(path) }.getOrDefault("")
        saved = text
    }
    val diagnostics = remember(text) { validateConfig(text) }
    val errors = diagnostics.filter { it.severity == Severity.ERROR }
    val warnings = diagnostics.filter { it.severity == Severity.WARNING }
    // счёт по живым совпадениям, не по размеру множеств: устаревшие записи в hidden/trusted не считаются
    val hiddenWarnings = warnings.filter { it.message in hidden }
    val trustedWarnings = warnings.filter { it.message in trusted }
    val visible = warnings.filter { it.message !in hidden && it.message !in trusted }
    val canSave = text != saved && errors.isEmpty()
    fun lineOf(d: Diagnostic) = text.take(d.offset).count { it == '\n' } + 1
    DialogWindow(
        onCloseRequest = onClose,
        title = "Редактирование: " + path.fileName,
        state = rememberDialogState(size = DpSize(760.dp, 660.dp)),
        resizable = false, // #E215: у DialogWindow нет minimumSize — фиксированный размер вместо ломающегося ресайза
    ) {
        Box(Modifier.fillMaxSize().background(Theme.bg1)) {
            Card(
                Modifier.fillMaxSize().padding(16.dp),
                shape = Theme.cardShape,
                colors = CardDefaults.cardColors(containerColor = Theme.card),
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    // ponytail: parseJson здесь и внутри validateConfig — двойной разбор KB-файла на keystroke, дешево
                    val parsed = remember(text) { parseJson(text) }
                    val tree = parsed.root as? JsonNode.Obj
                    val canForm = parsed.ok && tree != null
                    // при невалидном JSON форма недоступна — показываем JSON-режим независимо от выбора
                    val activeMode = if (mode == EditorMode.FORM && !canForm) EditorMode.JSON else mode
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            path.toString(), color = Theme.textDim, fontSize = 11.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        ModeSegment("Параметры", activeMode == EditorMode.FORM, canForm) { mode = EditorMode.FORM }
                        ModeSegment("JSON", activeMode == EditorMode.JSON) { mode = EditorMode.JSON }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (activeMode == EditorMode.JSON) {
                        JsonEditor(
                            text, { text = it }, Modifier.weight(1f).fillMaxWidth(),
                            problems = diagnostics, revealLine = revealLine, revealTick = revealTick,
                        )
                    } else if (tree != null) {
                        ConfigForm(tree, { t -> text = writeJson(t) }, Modifier.weight(1f).fillMaxWidth())
                    }
                    Spacer(Modifier.height(8.dp))
                    val statusColor = when {
                        errors.isNotEmpty() -> Theme.danger
                        warnings.isNotEmpty() -> Theme.warn
                        text.isNotBlank() -> Theme.ok
                        else -> Theme.textDim
                    }
                    Text(
                        when {
                            errors.isNotEmpty() -> "✕ " + errors.first().message
                            visible.isNotEmpty() -> "⚠ Предупреждений: ${visible.size} — подробнее см. в режиме JSON"
                            text.isNotBlank() -> "✓ JSON валиден"
                            else -> " "
                        },
                        color = statusColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (diagnostics.isNotEmpty() && activeMode == EditorMode.JSON) {
                        Spacer(Modifier.height(4.dp))
                        Column(
                            Modifier.heightIn(max = 140.dp).fillMaxWidth().verticalScroll(rememberScrollState()),
                        ) {
                            // ERROR-строки — без кнопок; скрытые/доверенные предупреждения скрыты (управление — в футерах ниже)
                            diagnostics.forEach { d ->
                                if (d.severity == Severity.ERROR) {
                                    Text(
                                        "Строка ${lineOf(d)}: ${d.message}",
                                        color = Theme.danger, fontSize = 11.sp,
                                        modifier = Modifier
                                            .hoverClick(Theme.chipShape) { revealLine = lineOf(d) - 1; revealTick++ }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                            .fillMaxWidth(),
                                    )
                                } else if (d.message !in hidden && d.message !in trusted) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "Строка ${lineOf(d)}: ${d.message}",
                                            color = Theme.warn, fontSize = 11.sp,
                                            modifier = Modifier
                                                .weight(1f)
                                                .hoverClick(Theme.chipShape) { revealLine = lineOf(d) - 1; revealTick++ },
                                        )
                                        Text(
                                            "Скрыть", color = Theme.textDim, fontSize = 11.sp,
                                            modifier = Modifier.hoverClick(Theme.chipShape) { hidden.add(d.message) }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                        )
                                        Text(
                                            "Доверять", color = Theme.accent, fontSize = 11.sp,
                                            modifier = Modifier.hoverClick(Theme.chipShape) {
                                                trusted.add(d.message)
                                                Prefs.saveTrustedWarnings(trusted)
                                            }.padding(horizontal = 4.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                            }
                            if (showHidden) hiddenWarnings.forEach { d ->
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Строка ${lineOf(d)}: ${d.message}",
                                        color = Theme.textDim, fontSize = 11.sp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .hoverClick(Theme.chipShape) { revealLine = lineOf(d) - 1; revealTick++ },
                                    )
                                    Text(
                                        "Вернуть", color = Theme.warn, fontSize = 11.sp,
                                        modifier = Modifier.hoverClick(Theme.chipShape) { hidden.remove(d.message) }
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            if (showTrusted) trustedWarnings.forEach { d ->
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "✓ Строка ${lineOf(d)}: ${d.message}",
                                        color = Theme.textDim, fontSize = 11.sp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .hoverClick(Theme.chipShape) { revealLine = lineOf(d) - 1; revealTick++ },
                                    )
                                    Text(
                                        "Передумать", color = Theme.danger, fontSize = 11.sp,
                                        modifier = Modifier.hoverClick(Theme.chipShape) {
                                            trusted.remove(d.message)
                                            Prefs.saveTrustedWarnings(trusted)
                                        }.padding(horizontal = 4.dp, vertical = 2.dp),
                                    )
                                }
                            }
                            if (hiddenWarnings.isNotEmpty()) {
                                Text(
                                    "Скрытые: ${hiddenWarnings.size} — " + if (showHidden) "свернуть" else "показать",
                                    color = Theme.textDim, fontSize = 11.sp,
                                    modifier = Modifier.hoverClick(Theme.chipShape) { showHidden = !showHidden }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                )
                            }
                            if (trustedWarnings.isNotEmpty()) {
                                Text(
                                    "Доверенные: ${trustedWarnings.size} — " + if (showTrusted) "свернуть" else "показать",
                                    color = Theme.textDim, fontSize = 11.sp,
                                    modifier = Modifier.hoverClick(Theme.chipShape) { showTrusted = !showTrusted }
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (text != saved) "Есть несохранённые изменения" else "",
                            color = Theme.warn, fontSize = 12.sp,
                            modifier = Modifier.weight(1f),
                        )
                        GradientButton("Сохранить", enabled = canSave) { onSave(path, text); saved = text }
                        Spacer(Modifier.width(8.dp))
                        GradientButton("Закрыть", onClick = onClose)
                    }
                }
            }
        }
    }
}

/** Clickable with the default desktop rectangle indication removed; smooth brightening on hover. */
@Composable
fun Modifier.hoverClick(shape: Shape, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val alpha by animateFloatAsState(if (hovered) 0.10f else 0f, tween(120))
    return this
        .clip(shape)
        .hoverable(source)
        .clickable(interactionSource = source, indication = null, onClick = onClick)
        .drawWithContent {
            drawContent()
            if (alpha > 0f) drawRect(Color.White.copy(alpha = alpha))
        }
}

@Composable
fun GradientButton(text: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val alpha by animateFloatAsState(if (enabled && hovered) 0.18f else 0f, tween(120))
    Box(
        modifier = modifier
            .clip(Theme.buttonShape)
            .background(if (enabled) Theme.accentGradient else Theme.clearGradient)
            .hoverable(source, enabled = enabled)
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .drawWithContent {
                drawContent()
                if (alpha > 0f) drawRect(Color.White.copy(alpha = alpha))
            }
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, color = if (enabled) Theme.text else Theme.textDim)
    }
}

@Composable
fun AboutDialog(onClose: () -> Unit) {
    DialogWindow(onCloseRequest = onClose, title = "О программе", resizable = false) {
        Box(Modifier.fillMaxSize().background(Theme.bg1)) {
            Card(shape = Theme.cardShape, colors = CardDefaults.cardColors(containerColor = Theme.card)) {
                Column(Modifier.padding(24.dp).width(360.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Claude Settings Backuper",
                        style = TextStyle(brush = Theme.accentGradient, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Ищет проекты Claude Code, позволяет создавать бекапы их settings.json, редактировать и восстанавливать их.",
                        color = Theme.textDim, fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Версия 0.3.0", color = Theme.textDim, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    GradientButton("OK", onClick = onClose)
                }
            }
        }
    }
}
