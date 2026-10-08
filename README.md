# musaid

**musaid** is a free, 100% offline **Bangla** Android app for everyday life —
daily routines with spoken voice alarms, habits, goals, and monthly reports.

- **No internet needed** — works fully offline
- **No account, no ads, no tracking** — your data never leaves your phone
- **Bangla voice alarms** — the alarm reads your message out loud in Bangla

---

## Download the app

**Latest version: v1.0**

[Download musaid-v1.0.apk directly](https://github.com/nurulhudaturag-droid/Musaid-Android-App/releases/download/v1.0/musaid-v1.0.apk)

Or copy this link and paste it into your browser:

```
https://github.com/nurulhudaturag-droid/Musaid-Android-App/releases/download/v1.0/musaid-v1.0.apk
```

**All releases (every version, old and new):**

[Open the Releases page](https://github.com/nurulhudaturag-droid/Musaid-Android-App/releases) or copy this link:

```
https://github.com/nurulhudaturag-droid/Musaid-Android-App/releases
```

New updates will be published on the Releases page, so check it from time to time.

---

## Install the app (about 1 minute)

1. Download the APK using one of the links above.
2. Open the downloaded file and tap **Install**.
3. If Android shows **"For your security, your phone is not allowed to install unknown apps from this source"**:
   - Tap **Settings**
   - Turn on **Allow from this source**
   - Go back and tap **Install** again
4. Open **musaid**.
5. Allow **Notifications** and **Alarms/Reminders** when asked — without these, voice alarms will not ring.

> Already have an older or test version installed? Uninstall it first, then install this release — Android blocks updates when two versions are signed with different keys.

---

## How to use musaid

The app has 5 tabs at the bottom of the screen:

| Tab (as shown in the app) | What it is for |
|---|---|
| **রুটিন (Routine)** | Daily timed tasks with voice alarms |
| **টার্গেট (Target)** | Goals broken into steps, with reminders |
| **চেকলিস্ট (Checklist)** | Daily habits — tick them off every day |
| **রিপোর্ট (Report)** | Daily progress report |
| **সারাংশ (Summary)** | Monthly progress overview |

### Daily routine with a voice alarm

1. Open the **রুটিন (Routine)** tab.
2. Tap the **+** button.
3. Type a **title** (for example "Fajr prayer") and a **message** — this is exactly what the voice will say.
4. Set the **time** and choose which **days** it should ring.
5. Tap **Save**.
6. At the set time you get a notification, and the app **speaks your message in Bangla**.

- To edit or remove a routine, tap the **⋮** menu on its card.
- Tip: grant the alarm permission Android asks for, otherwise the alarm will not ring.

### Goals (টার্গেট)

1. Open the **টার্গেট (Target)** tab and tap **+**.
2. Enter the goal name, start date, and target date (plus an optional motivating message).
3. Open the goal and add **steps** — each step can have its own date and reminder.
4. Tick a step when it is done. When **all steps are done**, the goal is marked complete automatically.
5. Use the filter chips at the top to show only running, completed, or paused goals.

### Daily habits (চেকলিস্ট)

1. Open the **চেকলিস্ট (Checklist)** tab and tap **+**.
2. Type the habit name and tap **Save**.
3. Every day, tick the box to keep your streak going.

### Daily report (রিপোর্ট)

1. Open the **রিপোর্ট (Report)** tab.
2. Tick which routines and habits you finished **today**.
3. Tap the date at the top to view or correct **past days** — the last 30 days are always stored.

### Monthly summary (সারাংশ)

Open the **সারাংশ (Summary)** tab and move between months to see your success rate for routines and goals.

### Home screen widget

1. Long-press an empty spot on your home screen.
2. Choose **Widgets**, find **musaid**, and place it.
3. The widget always shows your **next routine**.

### Settings, backup and report time (gear icon, top-right)

- **Report time** — change when the daily report notification appears (default 21:00).
- **Export backup** — saves all your data as a JSON file on your phone.
- **Import backup** — restores that file (use it after buying a new phone).
- **Reschedule alarms** — fixes alarms if they stopped ringing, for example after a reboot.

---

## Important tips

- **Backup regularly:** all data lives only on this phone. Before resetting or changing your phone, use **Export backup**.
- **Battery optimization:** if alarms are late or silent, remove musaid from battery optimization (Settings → Apps → musaid → Battery → Unrestricted). On Xiaomi/MIUI-style phones also enable **Auto-start** if your phone offers it.
- **Permissions:** notifications and the alarm permission are required for voice alarms; the app asks for them on first launch.

---

## For developers

- Kotlin 2.2 + Jetpack Compose + Room, minSdk 24 / targetSdk 36. **No internet permission** — the app is fully offline by design.
- Build: `./gradlew :app:assembleDebug` · Test: `./gradlew :app:testDebugUnitTest`
- Every push runs CI (tests + debug APK). A `v*` tag builds a **signed release** and attaches the APK to the GitHub Release.
