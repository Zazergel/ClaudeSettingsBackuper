<p align="center">
<img src='https://allwebs.ru/images/2026/10/16/125a352ec07b009a3ae8c5da6b22e445.png' border='0' weight=300 height=300 alt='NotifyPay-logo'/></a>
</p>
  <p align="center">
     
   Локальное desktop-приложение: находит проекты Claude Code, делает бекапы их
`settings.json`. Также позволяет просматривать и править конфиги, а также безопасно
восстанавливать их.
    <br/>
    <p align="center">
    <a href="https://github.com/Zazergel/ClaudeSettingsBackuper/issues">Report Bug</a> *
    <a href="https://github.com/Zazergel/ClaudeSettingsBackuper/issues">Request Feature</a>
  </p>
  </p>
 <div class="header" markdown="1" align="center">

  ![Downloads](https://img.shields.io/github/downloads/Zazergel/ClaudeSettingsBackuper/total) 
  ![Contributors](https://img.shields.io/github/contributors/Zazergel/ClaudeSettingsBackuper?color=dark-green) 
  ![Forks](https://img.shields.io/github/forks/Zazergel/ClaudeSettingsBackuper?style=social) 
  ![Issues](https://img.shields.io/github/issues/Zazergel/ClaudeSettingsBackuper) 
</div>

# Claude Settings Backuper


## Возможности

- Сканирует выбранные директории в поисках проектов Claude Code (внутри которых есть папка `.claude`).
   Папки сканирования сохраняются между запусками.
- Находит в каждом проекте файл настроек `.claude/settings.json`
- Создаёт бекапы: копия файла с миллисекундной меткой —
   `settings.json.backup-<ГГГГ-ММ-ДД_ЧЧ-ММ-СС-мсс>`, можно добавить свою
   пометку: `...-мсс_до-эксперимента`. 
- Позволяет просматривать и редактировать `settings.json` (в том числе и бекап) в
   отдельном окне с подсветкой синтаксиса и индикатором несохранённых
   изменений.
- Восстанавливать ранее сохраненные конфиги: перезаписывает текущий `settings.json` выбранным бекапом
- Предупреждает, когда текущий бекап не сохранен: если у текущего файла ещё нет бекапов, перед перезаписью показывается предупреждение.
- Проекты из списка можно скрыть или вернуть —
   «Файл → Скрытые проекты».

## Запуск

Linux/macOS:

```bash
./gradlew run
```

Windows:

```bat
gradlew.bat run
```

Требуется JDK 21+. Тесты: `./gradlew test`.

## Разработка в IDEA

`File → Open` → папка проекта → Gradle sync → задача `run` (или `application → run`).
Стек: Kotlin 2.4 + Compose Multiplatform Desktop 1.12, Gradle 9.8, JVM 21.

## Структура

```
src/main/kotlin/
  Main.kt          окно, меню, состояние, связка панелей
  Scanner.kt       поиск проектов (.claude/settings.json) по корням сканирования
  BackupEngine.kt  бекап / список / восстановление / удаление + решение о предупреждении (R6)
  Prefs.kt         сохранение папок сканирования и скрытых проектов (java.util.prefs)
  ui/Theme.kt      тёмная тема в оранжевой гамме Claude, градиенты
  ui/Panels.kt     панели: корни, список проектов, детали, окно редактирования, диалоги
  ui/HiddenDialog.kt   возврат скрытых проектов в список
  ui/JsonEditor.kt     редактор JSON с подсветкой (VisualTransformation)
  ui/ConsentDialog.kt  предупреждение перед перезаписью
```

Сборка самодостаточного дистрибутива (когда понадобится):
задача `packageDistributionForCurrentOS` из плагина Compose — добавляется одной настройкой,
сейчас не включена намеренно.
