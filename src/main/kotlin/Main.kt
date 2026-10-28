package app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ui.*
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
    var editTarget by remember { mutableStateOf<Path?>(null) }
    var diffTarget by remember { mutableStateOf<Project?>(null) }
    var pendingRestore by remember { mutableStateOf<Pair<Project, Path>?>(null) }
    var snapshot by remember { mutableStateOf(mapOf<String, String>()) }
    val conflicts = remember { mutableStateListOf<SnapshotConflict>() }
    val invalidConfigs = remember { mutableStateListOf<Project>() }

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

    fun persistSnapshot() = Prefs.saveSnapshot(snapshot)

    /** Обновить принятый хэш файла и снять с него конфликт (после save/restore/«Принять»). */
    fun acceptSnapshot(f: Path) {
        hashFile(f)?.let { h ->
            snapshot = snapshot + (f.toString() to h)
            persistSnapshot()
            writeShadow(f) // «было» для будущего диффа: копия ровно принятого содержимого
        }
        conflicts.removeAll { it.project.settings == f }
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

            // снапшот: новые файлы принимаются молча, исчезнувшие чистятся,
            // изменившиеся извне — конфликт (хэш в снапшоте остаётся прежним до «Принять»)
            val fresh = mutableMapOf<String, String>()
            val byKey = mutableMapOf<String, Project>()
            val invalid = mutableListOf<Project>()
            for (p in found) {
                val bytes = runCatching { Files.readAllBytes(p.settings) }.getOrNull() ?: continue
                val key = p.settings.toString()
                fresh[key] = sha256Hex(bytes)
                byKey[key] = p
                if (parseJson(String(bytes, Charsets.UTF_8)).errors.isNotEmpty()) invalid += p
            }
            val d = diff(fresh, snapshot)
            val changed = d.changed.mapNotNull { key ->
                byKey[key]?.let { SnapshotConflict(it, BackupEngine.listBackups(it.settings).isNotEmpty()) }
            }
            if (d.added.isNotEmpty()) snapshot = snapshot + fresh.filterKeys { it in d.added }
            if (d.removed.isNotEmpty()) {
                snapshot = snapshot - d.removed
                d.removed.forEach { deleteShadow(Path.of(it)) } // конфиг исчез — теневая копия не нужна
            }
            if (d.added.isNotEmpty() || d.removed.isNotEmpty()) persistSnapshot()
            conflicts.clear()
            conflicts.addAll(changed)
            invalidConfigs.clear()
            invalidConfigs.addAll(invalid)

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

    fun doBackup(label: String) {
        val p = selected ?: return
        scope.launch(Dispatchers.IO) {
            runCatching { BackupEngine.backup(p.settings, label = label) }
            reloadDetails(p)
        }
    }

    fun doRestore(p: Project, b: Path) {
        scope.launch(Dispatchers.IO) {
            runCatching { BackupEngine.restore(b, p.settings) }
            acceptSnapshot(p.settings)
            reloadDetails(p)
        }
    }

    fun doSave(f: Path, text: String) {
        if (!parseJson(text).ok) return // граница записи: UI уже блокирует кнопку, это вторая линия обороны
        scope.launch(Dispatchers.IO) {
            runCatching { Files.writeString(f, text) }
            acceptSnapshot(f)
            if (selected?.settings == f) reloadDetails(selected)
        }
    }

    fun doDelete(b: Path) {
        val p = selected ?: return
        scope.launch(Dispatchers.IO) {
            runCatching { BackupEngine.delete(b) }
            reloadDetails(p)
        }
    }

    /** R6 gate: direct restore only when safe (no current file, or backups exist). */
    fun requestRestore(p: Project, b: Path) {
        when (decideRestore(p.settings)) {
            RestoreChoice.PROCEED -> doRestore(p, b)
            RestoreChoice.BACKUP_THEN_RESTORE, RestoreChoice.CANCEL -> {}
            null -> pendingRestore = p to b
        }
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Claude Settings Backuper",
        state = rememberWindowState(size = DpSize(1000.dp, 640.dp)),
    ) {
        AppTheme {
            Column(Modifier.fillMaxSize().background(Theme.background)) {
                TopBar(
                    scanning = scanning,
                    onRescan = ::rescan,
                    onHidden = { showHidden = true },
                    onAddRoot = { addRoot(window) },
                    onAbout = { showAbout = true },
                    onExit = ::exitApplication,
                )
                RootsBar(
                    roots = roots.toList(),
                    onAdd = { addRoot(window) },
                    onRemove = { r -> roots.remove(r); persistRoots(); rescan() },
                )
                SnapshotBanner(
                    conflicts = conflicts.toList(),
                    invalid = invalidConfigs.toList(),
                    onShowDiff = { diffTarget = it },
                    onAccept = { p -> scope.launch(Dispatchers.IO) { acceptSnapshot(p.settings) } },
                    onRestore = { p ->
                        scope.launch(Dispatchers.IO) {
                            val last = BackupEngine.listBackups(p.settings).firstOrNull() ?: return@launch
                            requestRestore(p, last)
                        }
                    },
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
                        modifier = Modifier.width(340.dp).fillMaxHeight(),
                    )
                    DetailsPanel(
                        project = selected,
                        current = current,
                        backups = backups.toList(),
                        selectedBackup = selectedBackup,
                        onBackup = ::doBackup,
                        onSelectBackup = { selectedBackup = it },
                        onRestore = { val p = selected; val b = selectedBackup; if (p != null && b != null) requestRestore(p, b) },
                        onEdit = { editTarget = it },
                        onDelete = ::doDelete,
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
        editTarget?.let { target ->
            EditDialog(path = target, onSave = ::doSave, onClose = { editTarget = null })
        }
        diffTarget?.let { p ->
            DiffDialog(project = p, onClose = { diffTarget = null })
        }
        pendingRestore?.let { (p, b) ->
            ConsentDialog(onChoice = { choice ->
                pendingRestore = null
                when (choice) {
                    RestoreChoice.PROCEED -> doRestore(p, b)
                    RestoreChoice.BACKUP_THEN_RESTORE -> {
                        scope.launch(Dispatchers.IO) {
                            runCatching { BackupEngine.backup(p.settings) }
                            runCatching { BackupEngine.restore(b, p.settings) }
                            acceptSnapshot(p.settings)
                            reloadDetails(p)
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
        snapshot = Prefs.loadSnapshot() // до rescan: сравнение с последним принятым состоянием
        rescan()
    }
}
