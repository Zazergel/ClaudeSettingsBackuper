package app

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * The only place in the app that creates or overwrites files.
 * Backups are additive copies; deletion happens only on explicit user request.
 */
object BackupEngine {
    val TS: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS")
    private val LEGACY_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    private val SUFFIX = ".backup-"

    /** Optional [label] goes into the filename; filesystem-unsafe chars become '-'. */
    fun backup(settings: Path, stamp: ZonedDateTime = ZonedDateTime.now(), label: String? = null): Path {
        val name = settings.fileName.toString() + SUFFIX + stamp.format(TS) +
            label?.trim()?.takeIf { it.isNotEmpty() }?.let { "_" + it.replace(Regex("[\\\\/:*?\"<>|]"), "-") }.orEmpty()
        return Files.copy(settings, settings.resolveSibling(name), StandardCopyOption.REPLACE_EXISTING)
    }

    /** Backups of [settings], newest first. Reads both millisecond and legacy second-precision stamps; labels ignored. */
    fun listBackups(settings: Path): List<Path> {
        val prefix = settings.fileName.toString() + SUFFIX
        val dir = settings.parent ?: return emptyList()
        if (!Files.isDirectory(dir)) return emptyList()
        fun stampOf(remainder: String): LocalDateTime? =
            runCatching { LocalDateTime.parse(remainder.take(23), TS) }.getOrNull()
                ?: runCatching { LocalDateTime.parse(remainder.take(19), LEGACY_TS) }.getOrNull()
        return Files.list(dir).use { stream ->
            stream.map { p: Path -> p to stampOf(p.fileName.toString().removePrefix(prefix)) }
                .filter { it.second != null }
                .sorted(compareByDescending { it.second })
                .map { it.first }
                .toList()
        }
    }

    fun restore(backup: Path, target: Path) {
        Files.copy(backup, target, StandardCopyOption.REPLACE_EXISTING)
    }

    fun delete(backup: Path) {
        Files.deleteIfExists(backup)
    }
}

/** User's answer to the R6 consent dialog (CANCEL aborts). */
enum class RestoreChoice { PROCEED, BACKUP_THEN_RESTORE, CANCEL }

/**
 * null means: current file exists but has no backups yet, so the app must
 * ask for consent before overwriting it (requirement 6). Otherwise restore
 * is safe to run directly.
 */
fun decideRestore(current: java.nio.file.Path): RestoreChoice? = when {
    !Files.exists(current) -> RestoreChoice.PROCEED
    BackupEngine.listBackups(current).isEmpty() -> null
    else -> RestoreChoice.PROCEED
}
