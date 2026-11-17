# Mobilive Arcade

**Mobilive Arcade** is an Android social gaming, live streaming, multiplayer LAN world-hosting, and community rewards application built with **Kotlin**, **Jetpack Compose (Material 3)**, **Firebase Authentication (Google Sign-In via Credential Manager)**, and **Cloud Firestore**.

---

## Features

- **Authentication & Gamer Profile**
  - Google Sign-In & Sign-Up onboarding via Jetpack `CredentialManager` (`GetSignInWithGoogleOption` + startup auto sign-in).
  - Real-time `UserProfile` details in `ProfileScreen` fetched from Cloud Firestore displaying display name, avatar, join date (`createdAt`), bio, and level/XP progress.
  - Customizable **Avatar (Profile Face)** presets or personal photo via the zero-permission Android Photo Picker (`PickVisualMedia`).
  - **Verified Creator Badge**, bio, favorite game, follower/following counts, and live status ring.
- **Home, Feed, Stories & Shorts**
  - **Arcade Stories** tray, featured live stream banner, global **Search** (players, streams, LAN worlds, posts), and **Notifications** inbox.
  - Publish **Posts**, **Stories**, and **Shorts** with optional screenshots/videos.
  - Real-time **Like**, **Comment**, **Share** (native Android share sheet), **Delete** (own posts), **Report User**, and **Block User** actions.
- **Live Streaming, Stream Manager, Chat & Replay**
  - **Go Live** setup with optional automatic **Stream Replay** recording.
  - **Stream Manager** for host controls: mic toggle, chat slow mode (`0s`–`30s`), title editing, and ending streams cleanly to Replay.
  - Real-time **Stream Chat** with host message moderation and **Gift Coins** tipping (`+25`, `+100`, `+250`).
- **Multiplayer Lobby, LAN Worlds, Party Voice & Matchmaking**
  - **LAN World Hosting & Joining** (built for Minecraft & co-op LAN play):
    - *Hosting: open your world, then turn on Multiplayer Game and Visible to LAN Players.*
    - *Joining: press Join here, then open Minecraft > Play > Worlds and tap the world.*
  - Auto-detected local IPv4 LAN address, one-tap copy, and direct **Open Minecraft** launcher integration.
  - **Party System & Voice Chat** with runtime `RECORD_AUDIO` permission handling and **Matchmaking** queue (`idle` / `searching` / `matched`).
  - **Game Communities** and **Tournaments** with prize pools and team registration.
- **Social, Friends & Messaging**
  - **Friends** & **Friend Requests** (send, accept, decline), **Followers / Following** discovery, 1-on-1 **Messages**, and **Group Chats**.
- **Progression, Rewards, Gallery, Analytics & Privacy**
  - **Level System** with XP progress bar, **Level Up Buff Rewards** (XP boosts, coin bonuses, LAN priority, creator aura), **Achievements & Badges**, and **7-Day Gift Coins Claim**.
  - **Gallery** for uploading Videos, Screenshots, and Highlights via Android Photo Picker.
  - **Creator Analytics** rendered with Compose `Canvas` charts.
  - **Privacy Controls & Moderation**: DM policy (`Everyone` / `Friends` / `Nobody`), online visibility, LAN visibility, replay auto-save, blocked user management, and user reporting.

---

## Android SDK Setup

### Prerequisites

| Component | Version / Requirement |
| --- | --- |
| **JDK** | Java 11 or newer (JDK 17 recommended for Android Gradle Plugin 9.x) |
| **Android Gradle Plugin (AGP)** | `9.1.1` |
| **Gradle** | `9.3.1` |
| **Kotlin** | `2.2.10` |
| **compileSdk** | `36` (`minorApiLevel = 1`) |
| **targetSdk** | `36` |
| **minSdk** | `24` (Android 7.0 Nougat) |

### 1. Configure `ANDROID_SDK_ROOT` (Recommended)

Do **not** commit `local.properties` to version control. Configure your Android SDK path using the `ANDROID_SDK_ROOT` environment variable:

#### Linux / macOS
```bash
export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
```

#### Windows (PowerShell)
```powershell
$env:ANDROID_SDK_ROOT = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path += ";$env:ANDROID_SDK_ROOT\cmdline-tools\latest\bin;$env:ANDROID_SDK_ROOT\platform-tools"
```

### 2. Install Required SDK Packages

Using `sdkmanager` (from Android Command-Line Tools) or **Android Studio > Settings > Languages & Frameworks > Android SDK**, install:

```bash
yes | sdkmanager --licenses
sdkmanager \
  "platform-tools" \
  "platforms;android-36" \
  "build-tools;36.0.0"
```

> **Note:** In cloud build environments where `ANDROID_SDK_ROOT` is already set, leave `local.properties` absent so Gradle resolves `ANDROID_SDK_ROOT` automatically.

### 3. Build & Run

Using the system `gradle` command (or Android Studio):

```bash
# Compile debug Kotlin sources
gradle :app:compileDebugKotlin

# Build debug APK
gradle :app:assembleDebug

# Run local JVM / Robolectric unit tests
gradle :app:testDebugUnitTest
# Or with Gradle Wrapper (in CI / local wrapper setups):
./gradlew app:testDebugUnitTest --stacktrace
```

If opening locally in **Android Studio**, select **File > Open**, choose the repository root, let Gradle sync, and run the `app` configuration on an emulator or physical Android device (API 24+).

### 4. Automated Debug APK & GitHub Releases (GitHub Actions)

The workflow at `.github/workflows/android-debug-release.yml` automates testing, debug APK building, and GitHub Release publishing:

- **Triggers**: Pushes to `main` / `master`, version tags (`v*`), pull requests, and manual `workflow_dispatch`.
- **Steps**:
  1. Verifies `agp = "9.1.1"` in `gradle/libs.versions.toml` and sets up **JDK 17** and **Gradle 9.3.1** (bootstrapping `./gradlew` if absent).
  2. Accepts Android SDK licenses and decodes `debug.keystore.base64` into `debug.keystore` so the APK signing matches Firebase OAuth fingerprints.
  3. Runs `./gradlew app:testDebugUnitTest --stacktrace` and `gradle :app:assembleDebug`.
  4. Uploads `app/build/outputs/apk/debug/app-debug.apk` as a workflow artifact and publishes a **GitHub Release** with the debug APK attached (`v*` tags publish standard releases; branch/manual runs publish `debug-build-<run_number>` prereleases).

---

## Firebase & Security Rules Setup

1. **Configuration Files**
   - `app/google-services.json` — Firebase project & OAuth client configuration (SHA-1 / SHA-256 registered for Google Sign-In).
   - `app/src/main/res/values/firebase_applet_config.xml` — Defines `firestore_database_id` for the named Cloud Firestore database.
2. **Firestore Security Rules & Local Emulator Tests**
   - `firebase-blueprint.json` defines the entity schemas and collection paths.
   - `firestore.rules` enforces zero-trust schema validation, PII isolation (`/users/{userId}` vs `/public_profiles/{userId}`), ownership checks, and bounded fields.
   - Run local security rules tests against the Firestore emulator (port `8085`):

```bash
firebase emulators:start --only firestore --project demo-no-project
FIRESTORE_EMULATOR_HOST="127.0.0.1:8085" node --test firestore.test.js
```

---

## Project Structure

```text
.
├── app/
│   ├── build.gradle.kts                  # App config, compileSdk 36, Firebase & Compose deps
│   └── src/main/
│       ├── AndroidManifest.xml           # Permissions (INTERNET, ACCESS_NETWORK_STATE, RECORD_AUDIO)
│       ├── java/com/example/
│       │   ├── MainActivity.kt           # Edge-to-edge, Auth gating, Adaptive NavHost
│       │   ├── data/
│       │   │   ├── ArcadeModels.kt       # Firestore models & progression catalogs
│       │   │   ├── ArcadeRepository.kt   # Firestore real-time flows & mutations
│       │   │   └── FirestoreErrorHandler.kt
│       │   └── ui/
│       │       ├── auth/AuthScreen.kt    # Credential Manager Google Sign-In & Sign-Up
│       │       ├── components/           # Shared arcade UI components
│       │       ├── screens/              # Home, Watch, Play, Social, Profile screens
│       │       ├── theme/                # Material 3 Neon Obsidian theme & local fonts
│       │       └── viewmodel/            # ArcadeViewModel state flows
│       └── res/                          # Adaptive launcher icon, drawables, fonts, strings
├── firebase-blueprint.json               # Firestore schema blueprint
├── firebase.json                         # Single named database emulator config
├── firestore.rules                       # Hardened Firestore security rules
└── firestore.test.js                     # Automated security rules test suite
```
