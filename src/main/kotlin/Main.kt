package app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.AboutDialog
import ui.AppTheme
import ui.ConsentDialog
import ui.CurrentInfo
import ui.DetailsPanel
import ui.HiddenDialog
import ui.ProjectList
import ui.RootsBar
import ui.Theme
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.JFileChooser
import javax.swing.UIManager

fun main() = application {
    runCatching { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) }

    val scope = rememberCoroutineScope()
    val roots = remember { mutableStateListOf<Path>() }
    val projects = remember { mutableStateListOf<Project>() }
    val backups = remember { mutableStateListOf<Path>() }
    val hidden = remember { mutableStateListOf<Path>() }
    var selected by remember { mutableStateOf<Project?>(null) }
    var selectedBackup by remember { mutableStateOf<Path?>(null) }
    var current by remember { mutableStateOf<CurrentInfo?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showHidden by remember { mutableStateOf(false) }
    var pendingRestore by remember { mutableStateOf<Path?>(null) }

    fun reloadDetails(p: Project?) {
        selected = p
        selectedBackup = null
        backups.clear()
        if (p == null) {
            current = null
            return
        }
        current = runCatching {
            if (Files.exists(p.settings)) {
                CurrentInfo(
                    exists = true,
                    size = Files.size(p.settings),
                    mtime = LocalDateTime.ofInstant(Files.getLastModifiedTime(p.settings).toInstant(), ZoneId.systemDefault()),
                )
            } else CurrentInfo(false, 0, LocalDateTime.now())
        }.getOrDefault(CurrentInfo(false, 0, LocalDateTime.now()))
        scope.launch(Dispatchers.IO) {
            val list = runCatching { BackupEngine.listBackups(p.settings) }.getOrDefault(emptyList())
            // Compose snapshot state is thread-safe; IO thread may write it directly.
            if (selected == p) backups.addAll(list)
        }
    }

    fun rescan() {
        scanning = true
        scope.launch(Dispatchers.IO) {
            val found = roots.distinct()
                .flatMap { runCatching { Scanner.scan(it) }.getOrDefault(emptyList()) }
                .distinctBy { it.root }
                .sortedBy { it.root.toString().lowercase() }
            projects.clear()
            projects.addAll(found)
            scanning = false
            if (selected != null && selected !in found) reloadDetails(null)
        }
    }

    fun persistRoots() = Prefs.saveRoots(roots.map { it.toString() })

    fun persistHidden() = Prefs.saveHidden(hidden.map { it.toString() })

    fun hideProject(p: Project) {
        if (p.root !in hidden) {
            hidden.add(p.root)
            persistHidden()
        }
        if (selected == p) reloadDetails(null)
    }

    fun unhideProject(root: Path) {
        hidden.remove(root)
        persistHidden()
    }

    fun addRoot(parent: java.awt.Window?) {
        val chooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            dialogTitle = "Добавить папку сканирования"
        }
        val chosen = if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFile.toPath()
        } else null
        if (chosen != null && chosen !in roots) {
            roots.add(chosen)
            persistRoots()
            rescan()
        }
    }

    fun doBackup() {
        val p = selected ?: return
        scope.launch(Dispatchers.IO) {
            runCatching { BackupEngine.backup(p.settings) }
            reloadDetails(p)
        }
    }

    fun doRestore(b: Path) {
        val p = selected ?: return
        scope.launch(Dispatchers.IO) {
            runCatching { BackupEngine.restore(b, p.settings) }
            reloadDetails(p)
        }
    }

    /** R6 gate: direct restore only when safe (no current file, or backups exist). */
    fun requestRestore(b: Path) {
        val p = selected ?: return
        when (decideRestore(p.settings)) {
            RestoreChoice.PROCEED -> doRestore(b)
            RestoreChoice.BACKUP_THEN_RESTORE, RestoreChoice.CANCEL -> {}
            null -> pendingRestore = b
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Claude Settings Backuper",
        state = rememberWindowState(size = DpSize(1000.dp, 640.dp)),
    ) {
        MenuBar {
            Menu("Файл") {
                Item("Добавить папку сканирования") { addRoot(window) }
                Item("Обновить") { rescan() }
                Item("Скрытые проекты...") { showHidden = true }
                Separator()
                Item("Выход") { exitApplication() }
            }
            Menu("Справка") {
                Item("О программе") { showAbout = true }
            }
        }
        AppTheme {
            Column(Modifier.fillMaxSize().background(Theme.background)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Claude Settings Backuper",
                        style = TextStyle(brush = Theme.accentGradient, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                    )
                    Spacer(Modifier.weight(1f))
                    if (scanning) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Theme.accent)
                    }
                }
                RootsBar(
                    roots = roots.toList(),
                    onAdd = { addRoot(window) },
                    onRemove = { r -> roots.remove(r); persistRoots(); rescan() },
                )
                Row(
                    Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ProjectList(
                        projects = projects.toList().filter { it.root !in hidden },
                        selected = selected,
                        scanning = scanning,
                        onSelect = ::reloadDetails,
                        onHide = ::hideProject,
                        hiddenCount = hidden.size,
                        modifier = Modifier.width(340.dp).fillMaxHeight(),
                    )
                    DetailsPanel(
                        project = selected,
                        current = current,
                        backups = backups.toList(),
                        selectedBackup = selectedBackup,
                        onBackup = ::doBackup,
                        onSelectBackup = { selectedBackup = it },
                        onRestore = { selectedBackup?.let(::requestRestore) },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
        if (showAbout) AboutDialog { showAbout = false }
        if (showHidden) HiddenDialog(
            paths = hidden.toList(),
            onRestore = ::unhideProject,
            onClose = { showHidden = false },
        )
        pendingRestore?.let { b ->
            ConsentDialog(onChoice = { choice ->
                pendingRestore = null
                when (choice) {
                    RestoreChoice.PROCEED -> doRestore(b)
                    RestoreChoice.BACKUP_THEN_RESTORE -> {
                        val p = selected
                        if (p != null) {
                            scope.launch(Dispatchers.IO) {
                                runCatching { BackupEngine.backup(p.settings) }
                                runCatching { BackupEngine.restore(b, p.settings) }
                                reloadDetails(p)
                            }
                        }
                    }
                    RestoreChoice.CANCEL -> {}
                }
            })
        }
    }

    LaunchedEffect(Unit) {
        val saved = Prefs.loadRoots().map { runCatching { Path.of(it) }.getOrNull() }.filterNotNull()
        if (saved.isEmpty()) roots.add(Path.of(System.getProperty("user.home"))) else roots.addAll(saved)
        hidden.addAll(Prefs.loadHidden().map { runCatching { Path.of(it) }.getOrNull() }.filterNotNull())
        rescan()
    }
}
