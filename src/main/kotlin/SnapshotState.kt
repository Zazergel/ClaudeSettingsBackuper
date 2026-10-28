package app

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.Base64

/** Отслеживание состояния конфигов: снапшот «путь → sha256», сравнение с последним принятым состоянием. */

fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

fun hashFile(path: Path): String? = runCatching { sha256Hex(Files.readAllBytes(path)) }.getOrNull()

/** Строка снапшота в Prefs: «base64(путь) хэш»; декодер пропускает битые строки. */
fun encodeSnapshot(snapshot: Map<String, String>): List<String> =
    snapshot.map { (path, hash) -> Base64.getEncoder().encodeToString(path.toByteArray(Charsets.UTF_8)) + " " + hash }

fun decodeSnapshot(lines: List<String>): Map<String, String> = buildMap {
    for (line in lines) {
        val space = line.lastIndexOf(' ')
        if (space <= 0) continue
        val hash = line.substring(space + 1)
        val path = runCatching { String(Base64.getDecoder().decode(line.substring(0, space)), Charsets.UTF_8) }.getOrNull() ?: continue
        if (hash.length == 64) put(path, hash)
    }
}

data class SnapshotDiff(val changed: List<String>, val added: List<String>, val removed: List<String>)

fun diff(current: Map<String, String>, stored: Map<String, String>): SnapshotDiff = SnapshotDiff(
    changed = current.keys.filter { stored[it] != null && stored[it] != current[it] },
    added = current.keys.filter { stored[it] == null },
    removed = stored.keys.filter { current[it] == null },
)

/** Строка построчного диффа: номер строки на своей стороне (1-based); у удалённых нет newLine, у вставленных oldLine. */
sealed interface DiffRow {
    data class Unchanged(val text: String, val oldLine: Int, val newLine: Int) : DiffRow
    data class Removed(val text: String, val oldLine: Int) : DiffRow
    data class Added(val text: String, val newLine: Int) : DiffRow
}

/** Построчный LCS-дифф. O(n·m) на сотнях строк конфига — мгновенно; ponytail: Myers, если конфиги вырастут за ~5k строк. */
fun lineDiff(old: List<String>, new: List<String>): List<DiffRow> {
    val n = old.size
    val m = new.size
    val w = m + 1
    val dp = IntArray((n + 1) * w)
    for (i in n - 1 downTo 0) for (j in m - 1 downTo 0)
        dp[i * w + j] = if (old[i] == new[j]) dp[(i + 1) * w + j + 1] + 1
        else maxOf(dp[(i + 1) * w + j], dp[i * w + j + 1])
    val rows = ArrayList<DiffRow>(n + m)
    var i = 0
    var j = 0
    while (i < n && j < m) when {
        old[i] == new[j] -> { rows += DiffRow.Unchanged(old[i], i + 1, j + 1); i++; j++ }
        dp[(i + 1) * w + j] >= dp[i * w + j + 1] -> { rows += DiffRow.Removed(old[i], i + 1); i++ }
        else -> { rows += DiffRow.Added(new[j], j + 1); j++ }
    }
    while (i < n) { rows += DiffRow.Removed(old[i], i + 1); i++ }
    while (j < m) { rows += DiffRow.Added(new[j], j + 1); j++ }
    return rows
}

/**
 * Теневые копии принятых конфигов — источник «было» для диффа (в Preferences контент не влезает, ~8КБ на значение).
 * Один файл на путь: sha256(абс. путь).json, перезапись при каждом принятии; удаление — когда конфиг исчез из снапшота.
 */
private val SHADOW_DIR: Path get() = Path.of(System.getProperty("user.home"), ".claude-settings-backuper", "shadow")

fun writeShadow(f: Path, base: Path = SHADOW_DIR) {
    runCatching {
        Files.createDirectories(base)
        Files.writeString(base.resolve(sha256Hex(f.toAbsolutePath().toString().toByteArray()) + ".json"), Files.readString(f))
    }
}

fun readShadow(f: Path, base: Path = SHADOW_DIR): String? =
    runCatching { Files.readString(base.resolve(sha256Hex(f.toAbsolutePath().toString().toByteArray()) + ".json")) }.getOrNull()

fun deleteShadow(f: Path, base: Path = SHADOW_DIR) {
    runCatching { Files.deleteIfExists(base.resolve(sha256Hex(f.toAbsolutePath().toString().toByteArray()) + ".json")) }
}
