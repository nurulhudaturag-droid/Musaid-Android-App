# AGENTS.md — Musaid Android App

## Project overview

**Musaid** — offline-first Android app (100% Bengali/Bangla UI) for daily routines with
spoken voice alarms, habits/checklists, goals with milestone steps, daily reports, and
monthly summaries.

- **applicationId:** `qiubzen.musaid` · **namespace/package root:** `qiubzen.musaid`
- App name: `musaid` (`strings.xml`, **lowercase**); rootProject name `Musaid`
- Launcher icon: final PNG pack installed from `musaid-icon-pack/` (folder removed from
  the project after install — backup lives outside the repo) —
  22 files across `mipmap-*` densities + `mipmap-anydpi-v26` adaptive XMLs,
  sha256-verified byte-identical to the pack. Do not regenerate/edit these files.
- Single Gradle module: `:app`. Git repo (push to GitHub → CI builds).
- minSdk 24, targetSdk/compileSdk 36 (with `minorApiLevel = 1`), versionName 1.0
- **100% offline:** no `INTERNET` permission, no network libraries, no telemetry, no
  cloud/SDK services. Data lives only in local Room DB + user-triggered JSON backup.
- Template leftovers (external-service metadata, API-key secrets plumbing, cloud plugins,
  promotional links) have been fully removed: metadata.json, .env.example, public/,
  secrets & cloud-service Gradle plugins — the project stands alone.

## Tech stack

| Area | Choice |
|---|---|
| Language / UI | Kotlin 2.2.10, Jetpack Compose + Material3 (BOM 2024.09.00) |
| Build | AGP 9.1.1, Gradle 9.3.1 (wrapper jar committed), KSP `2.3.5`, Java 11 target (JDK 21 runs Gradle + tests — Robolectric SDK 36 requires Java 21) |
| Data | Room 2.7.0 (`daily_routine_db`, DB version 3, `Migration(2,3)` for the completion-records unique index, `fallbackToDestructiveMigration` kept for unregistered jumps), Moshi (codegen) for JSON backup |
| Navigation | Navigation-Compose 2.8.9 (routes in `MainScreen.kt`) |
| Widget | Glance 1.1.0 app widget |
| Alarms | `AlarmManager` exact alarms + `BroadcastReceiver` + TextToSpeech (`bn_BD` locale, fallback to default) |
| Release | R8 minify **ON** + resource shrinking (`isMinifyEnabled`/`isShrinkResources`), default proguard + line-number keeps |
| Tests | JUnit4, Robolectric 4.16.1, Roborazzi 1.59.0 (screenshot), Compose UI test, Espresso (androidTest) |

**Removed on purpose** (do not re-add without a real feature need): Retrofit, OkHttp,
logging-interceptor, converter-moshi, Coil, DataStore, Accompanist, Camera, location
services, all cloud/telemetry/auth SDKs, cloud-service and secrets plugins (.env handling).
There is no network code path — keep it that way (fully offline requirement).

**Unavoidable "google" strings in the build** (technical coordinates, not branding/links —
leave them): `google()` Maven repo + `includeGroupByRegex("com\\.google.*")` in
`settings.gradle.kts` (AndroidX/AGP resolve from there) and plugin id
`com.google.devtools.ksp` (Room/Moshi codegen; alias is `libs.plugins.ksp`).

## Build & test

Wrapper jar is committed, so `./gradlew` works — **but a local Android SDK is required**
(none installed on this machine; no `local.properties`). Primary build/verify path is
GitHub Actions.

```bash
./gradlew :app:assembleDebug        # debug APK (needs debug.keystore — gitignored)
./gradlew :app:assembleRelease      # release APK: env ANDROID_KEYSTORE_PATH/ANDROID_KEYSTORE_PASSWORD/ANDROID_KEY_ALIAS/ANDROID_KEY_PASSWORD
./gradlew :app:testDebugUnitTest    # Robolectric + Roborazzi unit/screenshot tests
./gradlew :app:lintDebug
```

Debug keystore (local, gitignored) if missing:

```bash
keytool -genkeypair -keystore debug.keystore -storepass android -keypass android \
  -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10950 \
  -dname "CN=Android Debug,O=Android,C=US"
```

### CI (`.github/workflows/build.yml`)

- Every push/PR: unit tests + debug APK artifact (`musaid-debug-apk`).
- Tag `v*` (or manual `workflow_dispatch`): signed **release** APK (R8), attached to the
  GitHub Release. Requires repo secrets (user-created, never stored in the repo):
  - `ANDROID_KEYSTORE_BASE64` = `base64 -w0 my-upload-key.jks`
  - `ANDROID_KEYSTORE_PASSWORD`
  - `ANDROID_KEY_ALIAS`
  - `ANDROID_KEY_PASSWORD`
- Release keystore creation (never commit it — `*.jks` is gitignored; alias must match
  the `ANDROID_KEY_ALIAS` secret):

```bash
keytool -genkeypair -keystore my-upload-key.jks -storepass CHANGEME \
  -keypass CHANGEME -alias upload -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=Musaid,O=Musaid,C=BD"
```

## Architecture

Unidirectional flow: **Screen (Compose) → ViewModel (AndroidViewModel) → AppRepository → AppDao → Room**.
No DI framework — each ViewModel builds its own `AppRepository` + `AlarmScheduler` from the
`AppDatabase.getDatabase()` singleton in `init {}`.

```
app/src/main/java/qiubzen/musaid/
├── MainActivity.kt          # POST_NOTIFICATIONS + SCHEDULE_EXACT_ALARM requests;
│                            # intent extra "navigate_to" deep-links into a tab
├── data/                    # Room layer
│   ├── AppDatabase.kt       # entities: RoutineItem, ChecklistItem, CompletionRecord, Goal, GoalStep (v3)
│   ├── AppDao.kt            # all queries
│   ├── AppRepository.kt     # thin Flow/suspend wrapper over DAO
│   ├── BackupManager.kt     # SAF JSON export/import (BackupData via Moshi)
│   ├── Converters.kt        # List<Int> etc. <-> String
│   └── {RoutineItem,ChecklistItem,CompletionRecord,Goal,GoalStep}.kt  # @Entity + @JsonClass
├── viewmodel/
│   ├── AppViewModel.kt      # routines, checklists, today's due goal steps; widget refresh
│   ├── TodayFlow.kt         # todayEpochDayFlow(): today's epoch day, re-emits at local midnight
│   ├── GoalViewModel.kt     # goals + per-goal step stats (GoalWithStepStats), status filter
│   ├── ReportViewModel.kt   # daily report (mark completion per date)
│   └── SummaryViewModel.kt  # monthly aggregate/progress
├── ui/
│   ├── screens/MainScreen.kt          # Scaffold + bottom nav + NavHost (all routes)
│   ├── screens/RoutinesScreen.kt      # "রুটিন"
│   ├── screens/GoalsScreen.kt         # "টার্গেট" (list)
│   ├── screens/GoalDetailScreen.kt    # "টার্গেট" detail — LARGEST FILE (~973 lines)
│   ├── screens/ChecklistScreen.kt     # "চেকলিস্ট" (habits)
│   ├── screens/ReportScreen.kt        # "রিপোর্ট" (daily)
│   ├── screens/SummaryScreen.kt       # "সারাংশ" (monthly)
│   ├── screens/SettingsDialog.kt      # report time + JSON backup export/import + alarm reschedule
│   └── theme/{Color,Type,Theme}.kt    # MusaidTheme
├── alarm/
│   ├── AlarmScheduler.kt    # schedule/cancel routine, goal-step, and daily report alarms
│   ├── AlarmReceiver.kt     # notification + Bangla TTS read-aloud; auto-reschedules next occurrence
│   └── BootReceiver.kt      # re-schedules alarms after BOOT_COMPLETED / package update
└── widget/
    ├── RoutineWidget.kt     # Glance widget showing next routine
    └── RoutineWidgetReceiver.kt
```

Navigation routes (`MainScreen.kt`): `routines` (default) | `target` | `target_detail/{goalId}`
| `checklist` | `report` | `summary`. Detail screen hides top/bottom bars.

Manifest permissions: `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`
(**no INTERNET — keep it that way**). Receivers: `alarm.AlarmReceiver`, `alarm.BootReceiver`,
`widget.RoutineWidgetReceiver`. Theme: `Theme.Musaid`.

## Conventions

- **UI strings are Bangla**; code identifiers/comments in English. TTS uses `Locale("bn","BD")`
  with fallback to default.
- **Dates/times:**
  - `RoutineItem.timeMillis` = milliseconds **since midnight** (not absolute).
  - `Goal.startDateEpochDay` / `GoalStep.targetDateEpochDay` / `CompletionRecord.dateEpochDay`
    = `LocalDate.toEpochDay()`.
  - `daysOfWeek`: `0 = Sunday … 6 = Saturday` (`Calendar.DAY_OF_WEEK - 1`).
- **IDs:** alarm requestCode = routine id; goal-step alarms base `500_000 + stepId`;
  notification ids: routine `1000 + id`, report `999`, goal step `500_000 + id`;
  intent actions `qiubzen.musaid.action.*`.
- Settings: SharedPreferences `app_prefs`, key `report_time` (default 21:00, ms since midnight).
- Project-wide opt-ins (`kotlin { compilerOptions { optIn.add(...) } }` in
  `app/build.gradle.kts`): `kotlinx.coroutines.ExperimentalCoroutinesApi`
  (`flatMapLatest` in Report/Summary/AppViewModel) and `androidx.glance.ExperimentalGlanceApi`
  (`RoutineWidget().updateAll`) — both are ERROR-level; do not remove these flags.
- Status enums = string constants on companions (`Goal.STATUS_IN_PROGRESS/COMPLETED/PAUSED`).
- Room schema changes → bump `@Database` version (currently `fallbackToDestructiveMigration`
  **wipes user data** — add a real `Migration` before shipping any schema change).
- Secrets: none in the project. No `.env`, no secrets plugin. Release signing comes only
  from CI environment variables.

## Icons — INSTALLED (final, do not touch)

Icon pack source folder (`musaid-icon-pack/`) was **removed from the project** after
install; `app/src/main/res/` is now the single source of truth. Masters/Play Store PNG
backup lives outside the repo (`~/Desktop/musaid-icon-pack-backup/`).

- Installed **as supplied**: `android-res/` → `app/src/main/res/` (byte-for-byte `cp`,
  sha256 22/22 identical). **Never** edit, re-compress, regenerate (no Image Asset,
  no `flutter_launcher_icons`) — replacement requires the same copy-only discipline.
- 5 densities × 4 PNGs (`ic_launcher`, `ic_launcher_round`, `ic_launcher_foreground`,
  `ic_launcher_background`) + adaptive `mipmap-anydpi-v26/ic_launcher{,_round}.xml`
  referencing `@mipmap/ic_launcher_{background,foreground}`.
- Manifest uses `@mipmap/ic_launcher` / `@mipmap/ic_launcher_round` / `@string/app_name`
  (label = `musaid`).
- Notification/status-bar icon is separate and untouched: system
  `android.R.drawable.ic_lock_idle_alarm` in `AlarmReceiver` (intentional — do not swap
  for the launcher PNG).

## Known gaps / likely next tasks

1. First GitHub push must prove the build: package move to `qiubzen.musaid`, new
   applicationId, R8 release, and SDK setup are all verified only in CI.
2. `GoalDetailScreen.kt` (~973 lines) is the main refactor candidate.
3. Future Room schema changes still need a hand-written `Migration` (pattern exists:
   `MIGRATION_2_3` in `AppDatabase.kt`); destructive fallback remains for unregistered
   jumps and `exportSchema` stays off.

## Recent bug fixes (audit)

- `ExampleRobolectricTest` expected `"Musaid"` vs `app_name` `musaid` — test would fail in CI.
- `BootReceiver` registered 3 actions but only handled `BOOT_COMPLETED` — package-update and
  quickboot never rescheduled alarms (now handles all 3).
- Daily report alarm was never scheduled on a fresh install (only via settings/boot) —
  now scheduled idempotently in `MainActivity.onCreate`.
- Exact-alarm permission denial silently dropped alarms — `AlarmScheduler.scheduleWakeUp`
  now falls back to `setAndAllowWhileIdle` (inexact, still fires, no permission needed).
- Routine with empty `daysOfWeek` scheduled an alarm for a non-selected day — guarded.
- Backup import left stale PendingIntents (phantom alarms) — old routine/step alarms are
  cancelled before `replaceAllData`.
- Import kept old `completion_records` whose ids collide with imported items —
  `replaceAllData` now clears completion records too.
- Widget showed nothing when a weekly routine's time had passed today (`<7` vs `<=7`).
- Unused template `values/colors.xml` removed (no references).
- CI used JDK 17 but Robolectric SDK 36 requires JDK 21 — tests (and the whole
  debug-build job) would fail on every push (now `java-version: "21"`; CI also runs
  `recordRoborazziDebug` and uploads screenshots).
- Missing manifest `<queries>` for `TTS_SERVICE` — on Android 11+ the TTS engine
  could not resolve, so spoken Bangla alarms silently degraded to notifications.
- `AlarmReceiver` gated both `pendingResult.finish()` and the routine's next
  occurrence on the async TTS callback — a failing engine stopped recurring alarms;
  now reschedule/finish run independently (TTS is fire-and-forget, with shutdown on
  every path). Widget refreshes when a routine alarm fires; report notification and
  channel name are now Bangla.
- JSON import was not transactional and had no try/catch — a mid-import crash or a
  constraint violation could wipe the DB or crash the app; `replaceAllData` is now a
  Room `@Transaction` DAO method and failures show "Import failed". Backups now also
  carry `completionRecords` (restore no longer destroys report/summary history),
  and alarms are rescheduled from the *persisted* rows (Room may re-key imported ids).
- `AppDatabase.getDatabase` could build two live Room instances (no re-check inside
  `synchronized`) → split invalidation tracking; now double-checked.
- Backup export could report success when nothing was written (`openOutputStream`
  null) and left a stale tail when overwriting (`"wt"` now used); cancellation is
  no longer swallowed as failure.
- `scheduleRoutineAlarm` with out-of-range day values (imported JSON) fired an
  immediate past-time alarm — now schedules only when a day actually matched.
- Past-30-days report: showed 31 days, counted *unchecked* records as done, applied
  today's schedule to every past day, and labelled empty days "missed" — now a true
  30-day window, counts only completed ticks of items applicable to that weekday,
  with a new NOT_APPLICABLE status for days with nothing to do.
- Exact-alarm permission dialog opened on every cold start and stacked on top of
  the notification dialog — now chained after the notification result, asked once
  (flag in `app_prefs`).
- Goal detail: endless spinner with no escape when the goal doesn't exist (now a
  back-enabled not-found state); goal target date could be saved before its start
  date (now clamped).
- Dead data-layer code removed (3 unused DAO queries, 3 unused repo wrappers,
  `GoalViewModel.todayDueSteps`).
- Rotation/process death discarded every open dialog (and its typed input) — dialog
  flags now use `rememberSaveable`; editing targets are stored as ids and re-derived
  from the live lists (entities aren't Bundle-storable); dialog field states
  (titles, dates, times, days, switches) are saveable too.
- Long-lived flows captured `LocalDate.now()` once — a screen kept past midnight
  showed yesterday's data; shared `todayEpochDayFlow()` (new `viewmodel/TodayFlow.kt`)
  re-emits at local midnight and now keys today's due steps, goal stats, the past-30
  report, and monthly active goals. Those flows also read goal titles/steps via
  one-shot queries (stale after edits) — they now `combine` reactive flows.
- `completion_records` allowed duplicate `(dateEpochDay, itemType, itemId)` rows
  (double-tap races) — entity now carries a unique index; DB v3 adds
  `MIGRATION_2_3` (dedupes existing rows, then creates the index).
- Deleting the last unfinished step could auto-complete the goal and fire the
  congratulations event — completion now happens only from explicit toggles
  (`checkGoalCompletion(allowComplete=false)` on delete; empty-step goals reset to
  in-progress).
- Step reorder issued two sequential `@Update`s (an interleaved write could duplicate
  an order) — now an atomic `@Transaction swapSteps` DAO method.
- Granting the exact-alarm permission in Settings never rescheduled anything —
  `MainActivity.onResume` rebuilds routine/step/report alarms once the permission is
  held; the report alarm is also re-asserted after the notification-permission dialog.
- Goals filter chips clipped on narrow screens (now horizontally scrollable); save
  buttons no-op'd silently on empty titles (now `enabled =` gated); 5 English toasts
  in SettingsDialog are now Bangla; routine/step alarm cancels use `FLAG_NO_CREATE`
  (no PendingIntent fabrication).

## Known deferred items (reported by audit, intentionally not yet fixed)

- Screenshot goldens are recorded in CI artifacts but not committed (needs one
  local `recordRoborazziDebug` run on a machine with an Android SDK).
