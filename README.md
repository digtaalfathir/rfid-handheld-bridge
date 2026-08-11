# Stechoq RFID Suite

Native Android UHF RFID scanning app for warehouse handhelds. Supports **Chainway C72** and
**Zebra** (MC33/MC3300x) devices from a single APK — a one-time device-picker screen on first
launch decides which vendor SDK backend to use, so the same build can be installed on either
type of hardware.

## Features

- Continuous multi-tag inventory scanning with live tag table (EPC, read count, RSSI, antenna,
  new/existing status, last-seen time)
- Physical hardware trigger button support (hold-to-scan), including while backgrounded — a
  floating status bubble (idle/scanning/sent/failed, live tag count) shows over other apps when
  "Background Scanning" is on, tap it to reopen the app
- WO and Register modes share one endpoint and payload shape (see [API payload](#api-payload)) —
  Base URL and endpoint are both editable dropdowns that remember custom values
- Power range is native per vendor, not abstracted: Chainway 1–30 dBm, Zebra 0–300 (matches
  Zebra's own 123RFID app); adjustable beep volume; local CSV backup per scan session
  (`rfid_stc/` folder, toggle in Settings); English/Indonesian language switch (default English)
- Self-update: checks GitHub Releases on launch and on every return to the foreground, offering
  an in-app download + install prompt — deployed handhelds never need a laptop again. Current
  version is shown at the bottom of Settings.

## Project layout

```
app/src/main/java/com/example/chainwayrfidbridge/
├── MainActivity.kt              # Nav host, language CompositionLocal, physical trigger key events
├── RfidBridgeApplication.kt     # Process-wide foreground/background watcher for the floating bubble
├── ScanViewModel.kt             # Scan session state, tag buffer, beep, CSV backup, send, self-update
├── Payload.kt                   # Unified WO/Register JSON body shape
├── data/
│   ├── ScanConfig.kt            # Persisted scan settings + validation
│   ├── ConfigRepository.kt      # SharedPreferences (resettable config vs. device-level prefs)
│   ├── DeviceType.kt            # Chainway | Zebra, incl. each vendor's native power range
│   ├── AppLanguage.kt           # EN | ID
│   └── TagRecord.kt
├── rfid/
│   ├── RfidReaderManager.kt     # Vendor-agnostic interface both backends implement
│   ├── ChainwayReaderManager.kt # RFIDWithUHFUART push-callback backend
│   └── ZebraReaderManager.kt    # RFID API3 backend
├── network/
│   ├── ApiClient.kt             # POSTs scanned tags to the configured endpoint
│   └── UpdateClient.kt          # GitHub Releases version check + APK download
├── service/
│   ├── ScanStateBus.kt          # Shared scan-state mirror (Service has no ViewModel of its own)
│   └── FloatingScanService.kt   # Foreground service showing the background scan status bubble
└── ui/
    ├── DevicePickerScreen.kt    # First-launch vendor choice
    ├── ScanScreen.kt            # Main screen
    ├── SettingsScreen.kt        # Mode, API config, power, sound, background scan, language
    └── AppStrings.kt            # All user-facing strings, EN + ID
```

## Building

Requires the vendor SDKs, which are proprietary and **not included in this repo**:

1. Chainway UHF SDK `.aar` (e.g. `DeviceAPI_ver*.aar`) → drop into `app/libs/`
   (see `app/libs/README.txt`, or get it from https://www.chainway.net/Support/Info/10)
2. Zebra RFID API3 SDK `.aar` (e.g. `API3_LIB-release-*.aar`) → also into `app/libs/`

Then a standard Gradle build:

```bash
./gradlew testReleaseUnitTest assembleRelease
```

Output lands at `app/build/outputs/apk/release/app-release-unsigned.apk` — sign it before
installing (a debug keystore is fine for internal handheld deployment):

```bash
zipalign -f -p 4 app-release-unsigned.apk app-release-aligned.apk
apksigner sign --ks ~/.android/debug.keystore --ks-pass pass:android \
  --key-pass pass:android --out app-release-signed.apk app-release-aligned.apk
adb install -r app-release-signed.apk
```

`targetSdk` is deliberately held at 32 (see the comment in `app/build.gradle`) — the Zebra SDK
registers a broadcast receiver in a way that Android turns into a hard crash once `targetSdk`
reaches 33, only on Android 13+ devices. This is a sideloaded app, never published to Play
Store, so there's no policy reason to target higher.

`versionCode`/`versionName` in `app/build.gradle` fall back to `2`/`"1.1"` for a local build like
the one above, but CI always overrides both via `-PappVersionCode=`/`-PappVersionName=` — see
below.

## Releasing an update (automated)

`.github/workflows/release.yml` builds, signs, tags, and publishes a GitHub Release on every push
to `main` — no manual build or upload step. Deployed handhelds self-check GitHub Releases on
launch and on every return to the foreground (see `GITHUB_REPO` in `ScanViewModel.kt`) and prompt
an in-app update when a newer tag is found; the operator taps through one standard Android
"install from this app" prompt (a one-time approval per device, not per update).

Each run:
1. Computes a version from the run number (`versionCode = <run number>`, `versionName =
   1.0.<run number>`) — always increasing, which Android requires for an update to install over
   the previous version, without ever needing to hand-edit `build.gradle`.
2. Builds and unit-tests the release APK with that version baked in.
3. Signs it with the same keystore every already-deployed handheld was signed with (see setup
   below) — using a different key would make every existing install reject the update.
4. Tags the commit `vX.Y.Z` and publishes a GitHub Release with the signed APK attached.

### One-time CI setup

Vendor SDKs are proprietary and too large for a GitHub Actions secret (64 KB limit), so they're
restored from a dedicated release instead of committed to the repo:

```bash
gh release create vendor-sdks \
  app/libs/DeviceAPI_ver20251103_release.aar \
  app/libs/API3_LIB-release-2.0.1.29.aar \
  --title "Vendor SDKs (CI only)" \
  --notes "Restored into app/libs/ by release.yml — not part of the app's own release history."
```

The signing keystore *is* small enough for a secret — add it as `RELEASE_KEYSTORE_BASE64`
(Settings → Secrets and variables → Actions):

```bash
gh secret set RELEASE_KEYSTORE_BASE64 < <(base64 -w0 ~/.android/debug.keystore)
```

## API payload

WO and Register both post the same shape to the same endpoint — `mode` is what tells them apart
server-side. Base URL and endpoint are both editable in Settings (`endpoint` below is just the
shared default):

`POST {baseUrl}/api/v1/warehouse-management/jmp/log-rfids/components/handheld`
```json
{
  "rr_type": "T1B",
  "maker_name": "...",
  "idHex": ["E28011...", "E28022..."],
  "initial_year": "2026",
  "reader_id": "MC33-12f3da",
  "antenna": "1",
  "timestamp": "2026-08-05T10:00:00Z",
  "opname": false,
  "mode": "wo",
  "factory_code": "..."
}
```

`reader_id` is derived automatically from the device (`<model>-<short Android ID>`), never typed
by hand — every handheld in a fleet gets a unique, readable ID with no per-device setup.
`opname` is Register-only in the UI (a checkbox that only appears in that mode, meaning "also
post straight to current stock") but is always present in the payload, `false` outside Register.
