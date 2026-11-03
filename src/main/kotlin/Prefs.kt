package app

import java.util.*
import java.util.prefs.Preferences

/** Persistence of scan roots and hidden projects via the JDK Preferences store (base64, one path per line). */
object Prefs {
    private val NODE = Preferences.userNodeForPackage(Prefs::class.java)

    fun loadRoots(): List<String> = loadList("scan.roots")
    fun saveRoots(roots: List<String>) = saveList("scan.roots", roots)

    fun loadHidden(): List<String> = loadList("hidden.projects")
    fun saveHidden(paths: List<String>) = saveList("hidden.projects", paths)

    fun loadSnapshot(): Map<String, String> = decodeSnapshot(loadList("config.snapshot"))
    fun saveSnapshot(snapshot: Map<String, String>) = saveList("config.snapshot", encodeSnapshot(snapshot))

    fun loadTrustedWarnings(): Set<String> = loadList("trusted.warnings").toSet()
    fun saveTrustedWarnings(trusted: Set<String>) = saveList("trusted.warnings", trusted.toList())

    private fun loadList(key: String): List<String> = runCatching {
        val raw = NODE.get(key, "")
        if (raw.isBlank()) emptyList()
        else String(Base64.getDecoder().decode(raw), Charsets.UTF_8).lines().filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    private fun saveList(key: String, values: List<String>) {
        runCatching {
            NODE.put(key, Base64.getEncoder().encodeToString(values.joinToString("\n").toByteArray(Charsets.UTF_8)))
        }
    }
}
