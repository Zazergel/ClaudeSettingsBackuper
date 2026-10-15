package app

import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * The only place in the app that creates or overwrites files.
 * Backups are additive copies; nothing is ever deleted here.
 */
object BackupEngine {
    val TS: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    private val SUFFIX = ".backup-"

    fun backup(settings: Path, stamp: ZonedDateTime = ZonedDateTime.now()): Path =
        Files.copy(settings, settings.resolveSibling(settings.fileName.toString() + SUFFIX + stamp.format(TS)),
            StandardCopyOption.REPLACE_EXISTING)

    /** Backups of [settings], newest first. Unparseable look-alikes are ignored. */
    fun listBackups(settings: Path): List<Path> {
        val prefix = settings.fileName.toString() + SUFFIX
        val dir = settings.parent ?: return emptyList()
        if (!Files.isDirectory(dir)) return emptyList()
        return Files.list(dir).use { stream ->
            stream.filter { p: Path ->
                p.fileName.toString().startsWith(prefix) &&
                    runCatching { LocalDateTime.parse(p.fileName.toString().removePrefix(prefix), TS) }.isSuccess
            }
                .sorted(compareByDescending { p: Path -> LocalDateTime.parse(p.fileName.toString().removePrefix(prefix), TS) })
                .toList()
        }
    }

    fun restore(backup: Path, target: Path) {
        Files.copy(backup, target, StandardCopyOption.REPLACE_EXISTING)
    }
}
