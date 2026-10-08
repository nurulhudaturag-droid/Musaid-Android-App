# musaid

musaid — একটি সম্পূর্ণ অফলাইন বাংলা অ্যান্ড্রয়েড অ্যাপ ডেইলি রুটিন, ভয়েস অ্যালার্ম,
অভ্যাস/চেকলিস্ট, টার্গেট ও লক্ষ্য, দৈনিক রিপোর্ট ও মাসিক সারাংশ ব্যবস্থাপনার জন্য।

- **100% offline** — ইন্টারনেট পারমিশন নেই, কোনো সার্ভার/অ্যাপি কল নেই
- **১০০% বাংলা UI** — বাংলা টিটস (TTS) ভয়েসে অ্যালার্ম পড়ে শোনায়
- Local Room database + JSON backup (export/import)

## Features

- ডেইলি রুটিন — নির্দিষ্ট সময়ে সঠিক অ্যালার্ম, বাংলা ভয়েস উচ্চারণ, সাপ্তাহিক দিন নির্বাচন
- টার্গেট ও লক্ষ্য — ধাপে ধাপে স্টেপ, রিমাইন্ডার, অগ্রগতি ট্র্যাকিং
- অভ্যাস ও চেকলিস্ট — দৈনিক টিক দেওয়া
- দৈনিক রিপোর্ট ও মাসিক সারাংশ
- হোম স্ক্রিন উইজেট (Glance)
- JSON ব্যাকআপ এক্সপোর্ট/ইমপোর্ট

## Requirements

- JDK 21+ (CI uses 21 — Robolectric SDK 36 tests require Java 21)
- Android SDK (compileSdk 36)
- Android Studio (বা যেকোনো Gradle-supported IDE) — প্রথমবার ওপেন করলে IDE নিজে `local.properties` বানিয়ে দেবে

## Build

```bash
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:assembleRelease      # release APK (signing env vars লাগবে)
./gradlew :app:testDebugUnitTest    # unit + screenshot tests
```

Release build signing: `ANDROID_KEYSTORE_PATH`, `ANDROID_KEYSTORE_PASSWORD`,
`ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD` environment variable দিতে হবে (path না
দিলে `my-upload-key.jks` ব্যবহার হয়, alias না দিলে `upload`)।

## CI / GitHub Actions

- Push-e debug build + unit test চলে (`.github/workflows/build.yml`)
- Tag push (`v*`) বা manual dispatch-e signed release APK তৈরি হয়
- Repo **Settings → Secrets** এ এইগুলো রাখতে হবে (কোনো secret file-এ commit হবে না):
  - `ANDROID_KEYSTORE_BASE64` — keystore file-এর base64 (`base64 -w0 my-upload-key.jks`)
  - `ANDROID_KEYSTORE_PASSWORD`
  - `ANDROID_KEY_ALIAS`
  - `ANDROID_KEY_PASSWORD`

Release keystore না থাকলে (secret-এর `ANDROID_KEY_ALIAS` alias-এর সাথে মিলতে হবে):

```bash
keytool -genkeypair -keystore my-upload-key.jks -storepass CHANGE_ME \
  -keypass CHANGE_ME -alias upload -keyalg RSA -keysize 2048 \
  -validity 10000 -dname "CN=Musaid, OU=Musaid, O=Musaid, C=BD"
```

## Project structure

```
app/src/main/java/qiubzen/musaid/
├── MainActivity.kt
├── data/        # Room entities, DAO, repository, backup
├── viewmodel/   # AppViewModel, GoalViewModel, ReportViewModel, SummaryViewModel
├── ui/          # Compose screens + theme
├── alarm/       # AlarmScheduler, AlarmReceiver (TTS), BootReceiver
└── widget/      # Glance home-screen widget
```
