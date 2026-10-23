# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

**Frieza** is a personal Android app (Kotlin + Jetpack Compose) for managing the contents of one or several freezers. It is **100% local**: no account, no server, no API key, no analytics. The app is being prepared for internationalization; the UI is currently in French (see "Internationalization").

The owner has **no Android Studio**. Everything is built by GitHub Actions and the APK is installed directly on a phone. Verify changes by pushing to a branch and checking the CI run (`gh run list`, `gh run watch`), not with a local Android toolchain.

## Hard constraints

- **Never mention any third-party app that inspired features** (names, comments, commit messages, README, test data, branch names). No such references may appear in code or git history.
- Keep the app fully offline: do not add network permissions, remote APIs, ads, accounts or tracking.
- Do not break user data: any Room schema change needs a tested `Migration` (see below).

## Tech stack

- Kotlin 1.9.24, Jetpack Compose (BOM 2024.06.00, Material 3), Compose compiler 1.5.14
- Room 2.6.1 with KSP, Navigation Compose 2.7.7, ViewModel + StateFlow
- AGP 8.3.2, Gradle 8.6 (wrapper committed), JDK 17, minSdk 26, targetSdk/compileSdk 34
- No DI framework: `FriezaApplication` lazily builds a single `FriezaRepository`, passed down to screens.

## Architecture

Package `com.frieza.freezer` (`app/src/main/java/com/frieza/freezer/`):

- `data/`
  - `Food.kt`: all entities (`Freezer`, `Drawer`, `Category`, `Food`, `ShoppingItem`) and `DefaultCategories`
  - `Daos.kt`, `Converters.kt`, `FriezaDatabase.kt` (schema version 2, `MIGRATION_1_2`, default categories seeded in `onCreate`)
  - `FriezaRepository.kt`: the only data entry point for the UI (CRUD, transactions, selected-freezer preference, backup)
  - `BackupCodec.kt`: JSON export/import (all data, `"app": "frieza"`, `"version": 2`)
- `ui/freezer/`: main screen (stacked, collapsible drawers; food chips colored by first category; search, category filter, sort; multi-select; clipboard export of an AI "recipe ideas" prompt) and its `FreezerViewModel`
- `ui/food/`: add/edit food form (also duplicate/delete)
- `ui/setup/`: first-run / "new freezer" screen and the freezer edit screen (rename, drawers add/rename/reorder/delete)
- `ui/category/`, `ui/shopping/`: category manager and shopping list
- `ui/navigation/FriezaNavGraph.kt`: routes; start destination depends on whether a freezer exists
- `util/`: date helpers (`Dates.kt`) and food text formatting (`FoodText.kt`)

## Data model notes

- Hierarchy: `Freezer` -> `Drawer` (position 0 = top) -> `Food` (via `drawerId`). No foreign keys or indices are declared; cascading deletes are done manually in the repository inside `db.withTransaction`.
- `Food.categoryIds` is a `List<Long>` stored as `"1,2,3"` through a `TypeConverter`. The first category gives the chip color.
- Quantity fields (`unitCount`, `weightGrams`, `portions`) are independent and optional.
- **Dates picked by the user (`freezeDate`, `expirationDate`) are stored as UTC midnight** (what Material's DatePicker returns) and must always be formatted/compared via `util/Dates.kt`, never with the default time zone.
- Room schema: when changing entities, bump `version`, write a `Migration` whose `CREATE TABLE` statements match Room's expected schema exactly (types, `NOT NULL`, primary keys; no defaults unless declared), and test the SQL (e.g. with `sqlite3`) on sample data. `exportSchema` is currently `false`.

## UX decisions made by the owner (keep them)

- No tabs per floor/drawer. The freezer is shown as vertically stacked drawers, each collapsible, all expanded by default so the whole content is visible at once.
- Foods are colored chips grouped by drawer, with an expiry badge (expired / under 1 month / under 2 months) and an inline "Add" chip per drawer.
- Long-press starts multi-selection across all drawers; the selection can be copied to the clipboard as a ready-to-paste prompt for any AI assistant (no API integration).

## Build, CI and releases

- Local (if a JDK 17 + Android SDK happen to be available): `./gradlew assembleDebug`
- Workflow `.github/workflows/android-build.yml` (modeled on the owner's `angerona-nota` repo):
  - Runs on push to `main`, on tags `v*`, on pull requests to `main`, and manually (`workflow_dispatch`, optional `tag` input).
  - Version comes from the tag: `v1.2.3` -> `versionName 1.2.3`; other builds are `0.0.0-dev.<run>`. `versionCode` is the run number. They are passed as `-PversionNameOverride` / `-PversionCodeOverride` (read in `app/build.gradle.kts`).
  - Builds a debug APK signed with the committed, public `app/debug.keystore` (so each build installs over the previous one). **Never use this key for a store release.**
  - On a `v*` tag, the `release` job attaches `frieza.apk` to the GitHub Release (`softprops/action-gh-release`).
- Release flow: owner creates the tag/release on GitHub; CI builds and attaches the APK. A tag only builds the code it points to, so the tag must be on a commit that already contains the workflow.

## Git and GitHub conventions

- Default branch is `main`. Work on feature branches (e.g. `feature/v2`) and open a pull request.
- Commit messages in English from now on, imperative mood, short subject.
- In Claude Code on the web, the proxy blocks tag pushes, branch deletion and GraphQL; use the REST API (`gh api`) where allowed and tell the owner what they must do in the GitHub UI.

## Internationalization (planned)

User-visible strings are currently hard-coded French literals in the Compose screens, in `FreezerViewModel.buildClipboardText()`, in `DefaultCategories`, in the default drawer names (`"Étage N"`) and in `util/` formatting. When internationalizing:

1. Move every UI string to `res/values/strings.xml` (English as default) and add `res/values-fr/strings.xml`; use `stringResource` / plurals.
2. Default category and drawer names are written to the database at creation time. Decide whether to store keys and translate at display time, or to seed names in the language active at first launch; keep the existing French data working through the migration.
3. Date and number formatting uses `Locale.FRANCE`; switch to the user's locale.
4. The clipboard prompt for the AI assistant should follow the app language.
5. Backup JSON field names stay language-neutral.

## Conventions

- Match the existing style: Compose screens take `repository` and callbacks, state lives in a ViewModel (`StateFlow`) or local `remember`/`rememberSaveable`; use `rememberSaveable` with simple types (strings, numbers) rather than collections.
- Prefer small, compiling steps: the only compiler available is CI, so re-read imports (Compose scope members such as `ExposedDropdownMenu`, `menuAnchor` need no import) and push a branch to validate.
- Code, comments and documentation in English (the README is currently French).
