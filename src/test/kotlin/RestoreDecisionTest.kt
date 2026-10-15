import app.BackupEngine
import app.RestoreChoice
import app.decideRestore
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.io.TempDir

class RestoreDecisionTest {
    @TempDir lateinit var tmp: Path
    private val settings: Path get() = tmp.resolve("settings.json")

    @Test
    fun proceedsWhenCurrentMissing() {
        assertEquals(RestoreChoice.PROCEED, decideRestore(settings))
    }

    @Test
    fun asksConsentWhenCurrentUnbacked() {
        Files.writeString(settings, "current")
        assertNull(decideRestore(settings))
    }

    @Test
    fun proceedsWhenCurrentHasBackups() {
        Files.writeString(settings, "current")
        BackupEngine.backup(settings)
        assertEquals(RestoreChoice.PROCEED, decideRestore(settings))
    }
}
