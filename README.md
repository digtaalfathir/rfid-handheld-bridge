# Stechoq RFID Suite

Native Android UHF RFID scanning app for warehouse handhelds. Supports **Chainway C72** and
**Zebra** (MC33/MC3300x) devices from a single APK — a one-time device-picker screen on first
launch decides which vendor SDK backend to use, so the same build can be installed on either
type of hardware.

## Features

- Continuous multi-tag inventory scanning with live tag table (EPC, read count, RSSI, antenna,
  new/existing status, last-seen time)
- Physical hardware trigger button support (hold-to-scan)
- Two operating modes, **WO** and **Register**, each posting to a fixed endpoint and payload
  shape (see [API payload](#api-payload) below) — Base URL stays editable, endpoint is derived
  automatically from the selected mode
- Adjustable power (1–30 dBm, shared range across both vendors), adjustable beep volume,
  English/Indonesian language switch (default English)
- Self-update: checks GitHub Releases on launch and offers an in-app download + install prompt,
  so deployed handhelds don't need to be reconnected to a laptop for every new build

## Project layout

```
app/src/main/java/com/example/chainwayrfidbridge/
├── MainActivity.kt              # Nav host, language CompositionLocal, physical trigger key events
├── ScanViewModel.kt             # Scan session state, tag buffer, beep, send, self-update
├── Payload.kt                   # WO vs Register JSON body shape
├── data/
│   ├── ScanConfig.kt            # Persisted scan settings + per-mode endpoint + validation
│   ├── ConfigRepository.kt      # SharedPreferences (resettable config vs. device-level prefs)
│   ├── DeviceType.kt            # Chainway | Zebra
│   ├── AppLanguage.kt           # EN | ID
│   └── TagRecord.kt
├── rfid/
│   ├── RfidReaderManager.kt     # Vendor-agnostic interface both backends implement
│   ├── ChainwayReaderManager.kt # RFIDWithUHFUART push-callback backend
│   └── ZebraReaderManager.kt    # RFID API3 backend
├── network/
│   ├── ApiClient.kt             # POSTs scanned tags to the configured endpoint
│   └── UpdateClient.kt          # GitHub Releases version check + APK download
└── ui/
    ├── DevicePickerScreen.kt    # First-launch vendor choice
    ├── ScanScreen.kt            # Main screen
    ├── SettingsScreen.kt        # Mode, API config, power, sound, language
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

## Releasing an update

Deployed handhelds self-check GitHub Releases on every launch (see `GITHUB_REPO` in
`ScanViewModel.kt`) and prompt an in-app update if a newer tag is found. To ship one:

1. Bump **both** `versionCode` and `versionName` in `app/build.gradle` — Android's installer
   refuses to install over an equal-or-lower `versionCode`, independent of the app's own check.
2. Build and sign the release APK as above.
3. Tag the commit to match `versionName` (e.g. `versionName "1.2"` → tag `v1.2`) and push the tag.
4. Create a GitHub Release on that tag with the signed APK attached as an asset.

Any handheld running an older version picks it up automatically next time it's opened; the user
taps through one standard Android "install from this app" prompt (a one-time approval per
device, not per update).

## API payload

Base URL is configurable in Settings; the endpoint path is fixed per mode and cannot be edited
directly:

**WO** — `POST {baseUrl}/api/v1/warehouse-management/jmp/log-rfids`
```json
{
  "reader_id": "MC33-12f3da",
  "antenna": "1",
  "idHex": ["E28011...", "E28022..."],
  "timestamp": "2026-08-05T10:00:00Z"
}
```

**Register** — `POST {baseUrl}/api/v1/warehouse-management/jmp/log-rfids/components/handheld`
```json
{
  "rr_type": "T1B",
  "maker_name": "...",
  "rfid_numbers": ["E28011...", "E28022..."],
  "initial_year": "2026",
  "reader_id": "MC33-12f3da",
  "antenna": "1"
}
```

`reader_id` is derived automatically from the device (`<model>-<short Android ID>`), never typed
by hand — every handheld in a fleet gets a unique, readable ID with no per-device setup.
