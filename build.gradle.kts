import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.4.21"
    kotlin("plugin.compose") version "2.4.21"
    id("org.jetbrains.compose") version "1.12.1"
}

group = "local.claude"
version = "0.2.0"

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    testImplementation(kotlin("test"))
}

tasks.withType<Test> {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "app.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi)

            packageName = "ClaudeSettingsBackuper"
            packageVersion = project.version.toString()
            vendor = "Zazergel"

            linux {
                iconFile.set(rootProject.file("icon.png"))
            }
            windows {
                iconFile.set(rootProject.file("icon.ico"))
                upgradeUuid = "e2f4a1c7-9b3d-4e68-a57c-0d82f61b93a4"
                perUserInstall = true
                dirChooser = true
                menu = true
                shortcut = true
            }
        }
    }
}
