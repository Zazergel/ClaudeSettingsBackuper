package app

import java.util.Base64
import java.util.prefs.Preferences

/** Scan-roots persistence via the JDK Preferences store (base64, one path per line). */
object Prefs {
    private val NODE = Preferences.userNodeForPackage(Prefs::class.java)
    private const val KEY = "scan.roots"

    fun loadRoots(): List<String> = runCatching {
        val raw = NODE.get(KEY, "")
        if (raw.isBlank()) emptyList()
        else String(Base64.getDecoder().decode(raw), Charsets.UTF_8).lines().filter { it.isNotBlank() }
    }.getOrDefault(emptyList())

    fun saveRoots(roots: List<String>) {
        runCatching {
            NODE.put(KEY, Base64.getEncoder().encodeToString(roots.joinToString("\n").toByteArray(Charsets.UTF_8)))
        }
    }
}
