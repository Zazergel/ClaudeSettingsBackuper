package ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Shape
import app.Project
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
        GradientButton("+ Добавить", onClick = onAdd)
    }
}

@Composable
fun ProjectList(
    projects: List<Project>,
    selected: Project?,
    scanning: Boolean,
    onSelect: (Project) -> Unit,
    onHide: (Project) -> Unit = {},
    hiddenCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    Card(modifier, shape = Theme.cardShape, colors = CardDefaults.cardColors(containerColor = Theme.card)) {
        Column(Modifier.fillMaxSize().padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Проекты (${projects.size})", color = Theme.text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (scanning) CircularProgressIndicator(Modifier.size(16.dp), color = Theme.accent, strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(8.dp))
            if (hiddenCount > 0) {
                Text(
                    "Скрыто проектов: " + hiddenCount + " (Файл -> Скрытые проекты)",
                    color = Theme.textDim, fontSize = 11.sp,
                )
                Spacer(Modifier.height(4.dp))
            }
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
    onBackup: () -> Unit,
    onSelectBackup: (Path?) -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Text(project.settings.toString(), color = Theme.textDim, fontSize = 12.sp)
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
            GradientButton("Создать бекап", enabled = current?.exists == true, onClick = onBackup)
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
                ) {
                    Text(
                        backup.fileName.toString().removePrefix("settings.json."),
                        color = if (isSelected) Theme.accent2 else Theme.textDim,
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            GradientButton("Восстановить", enabled = selectedBackup != null, onClick = onRestore)
            }
            VerticalScrollbar(
                rememberScrollbarAdapter(scrollState),
                Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
            )
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
    Dialog(onCloseRequest = onClose, title = "О программе", resizable = false) {
        Box(Modifier.fillMaxSize().background(Theme.bg1)) {
            Card(shape = Theme.cardShape, colors = CardDefaults.cardColors(containerColor = Theme.card)) {
                Column(Modifier.padding(24.dp).width(360.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Claude Settings Backuper",
                        style = TextStyle(brush = Theme.accentGradient, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Локальная утилита: находит проекты Claude Code, делает бекапы их settings.json и восстанавливает их.",
                        color = Theme.textDim, fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Версия 0.1.0", color = Theme.textDim, fontSize = 12.sp)
                    Spacer(Modifier.height(16.dp))
                    GradientButton("OK", onClick = onClose)
                }
            }
        }
    }
}
