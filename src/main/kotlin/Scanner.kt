import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/** A Claude Code project: a directory containing a real .claude/settings.json file. */
data class Project(val root: Path, val settings: Path)

object Scanner {
    /** Directory names never descended into. */
    private val SKIP_NAMES = setOf("node_modules", "build", "target", "AppData", "Library")

    /** How deep [Files.walkFileTree] goes below the scan root. */
    private const val MAX_DEPTH = 6

    fun scan(root: Path, maxDepth: Int = MAX_DEPTH): List<Project> {
        if (!Files.isDirectory(root)) return emptyList()
        val found = mutableListOf<Project>()
        Files.walkFileTree(root, emptySet(), maxDepth, object : SimpleFileVisitor<Path>() {
            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                val name = dir.fileName?.toString()
                // Descend into ".claude" itself but skip every other dot-dir and known heavy dirs.
                if (dir != root && name != null && skip(name)) return FileVisitResult.SKIP_SUBTREE
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                val parent = file.parent?.fileName?.toString()
                if (attrs.isRegularFile && file.fileName.toString() == "settings.json" && parent == ".claude") {
                    found += Project(root = file.parent.parent, settings = file)
                }
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException?) = FileVisitResult.SKIP_SUBTREE
        })
        return found
    }

    private fun skip(name: String): Boolean = (name.startsWith(".") && name != ".claude") || name in SKIP_NAMES
}
