# Roam Launcher — S6 prototype

Native Android launcher intended for the **Hisense RoamView S6, not S6S**. Version 0.1. Uses Android framework Views and Java, with no runtime library dependencies. The S6's firmware, Android version, input mapping, and ability to select a third-party home app have not been verified. Requires Android 6 / API 23 or later.

## What is implemented

- Dark, spacious Apple TV-inspired home, rounded cards, visible focus/selection state.
- Portrait (3 columns) and landscape (5 columns). `fullUser` follows user rotation settings; enable auto-rotate on the device. Rotation recreates the screen and restores the selected item by identity.
- Vertically scrollable app rows with **no fixed row limit**; GridView recycles tile views. Rows grow with installed apps, subject to normal device memory limits. This prototype has an alphabetical app grid, not editable/custom named shelves.
- Discovers standard and Leanback/TV launchers, preferring TV launch entries per package. Refreshes when returning home. Touch, pointer clicks, and explicit D-pad selection work through the same items.
- Up/Down move between rows; Left/Right move within a row without wrapping. Moving down to a partial final row clamps to its last tile. OK/Enter/A launches. Back on home selects the first control.
- Remote laboratory: six focus targets; raw key action, code, scan code, repeat, device ID and input source; pointer/touch/scroll events; device model/API report; clipboard export and `RoamKeys` logcat output.
- Muted local-video aerial screensaver playlist with preview, 2-minute launcher idle trigger, and idle-off option. Playlist grants persist. Any delivered key, touch, pointer movement, or scroll exits. Video aspect ratio is preserved with black space where necessary.

The screensaver works **while this launcher is visible**. It is not a system DreamService and does not interrupt other apps. It keeps the display awake only during playback. Use videos you own or have permission to use. No aerial footage or Apple assets are included. H.264 MP4 at 1080p is a useful starting format; actual decoder support requires device testing. No internet or broad storage permissions are requested. Selecting videos replaces the playlist.

## Diagnose the remote first

The report that Left/Right seek in YouTube TV suggests usable input in that app. It does **not** prove that these are standard D-pad events. Nova's focus layout, pointer mode, media keys, or vendor routing could explain the menu behavior. No remapping is applied before observing events.

1. Open Roam Launcher as an ordinary app; do not make it your default yet.
2. Open **Remote test** with touch if needed. Turn the remote's cursor/pointer mode **off**.
3. Press Up, Down, Left, Right, OK and Back individually. Reopen the test and hold arrows briefly to inspect repeats.
4. Standard expected codes: Up=19, Down=20, Left=21, Right=22, Center=23. Enter=66 is another possible select event. The report displays symbolic names as well.
5. Test the six tiles. White borders must move both horizontally and vertically; OK must produce an `ACTIVATE` line. Copy the report, or capture it using ADB below.
6. Repeat with pointer mode **on**. Motion events instead of key events indicate pointer input. Test pointer clicks and touch separately.
7. If Left/Right log as `KEYCODE_MEDIA_REWIND`/`FAST_FORWARD` or another code, retain the report for a deliberate device-specific mapping. If no event appears, this activity cannot remap an event that firmware does not deliver.
8. Compare a USB/Bluetooth keyboard or injected D-pad events with the physical remote. If those work but the remote doesn't, investigate remote mode/firmware routing. If standard remote keys reach the test but a focus target fails, that is a launcher issue.

Android normally routes Home at the system level; do not expect a Home key log. Logs capture only this app's diagnostic activity, not YouTube or Nova.

## Build an APK

**No APK is included:** this workspace has no Java runtime, Android SDK, Gradle, or connected device. The source has not been compiled, linted, or run on hardware here. A debug APK is installable and automatically debug-signed when built successfully.

### Android Studio

1. Install Android Studio with JDK 17, Android SDK Platform 35, and Build Tools 35.0.0.
2. Open this `RoamLauncher` directory. The build uses Android Gradle Plugin 8.7.3 and Gradle 8.9.
3. This source package does not include the Gradle wrapper binary. If Studio asks for a Gradle distribution, install Gradle 8.9 and select it, or run `gradle wrapper --gradle-version 8.9` once from this directory with that Gradle installation.
4. Set the SDK path in Studio or `local.properties` (`sdk.dir=/absolute/path/to/Android/sdk`).
5. Run `gradle :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`, or use `./gradlew` after generating the wrapper.
6. APK: `app/build/outputs/apk/debug/app-debug.apk`.

### GitHub build (no local Android setup)

Upload the **contents** of this directory to the root of your own GitHub repository, including `.github/workflows/build.yml`. The workflow installs JDK 17, Android tooling and Gradle 8.9; runs navigation tests, builds and lints; and uploads `RoamLauncher-debug-apk`. Open Actions → Build sideload APK → successful run → Artifacts. Download and unzip the artifact to obtain `app-debug.apk`. This workflow is supplied but has not been run or published for you.

A fresh hosted runner can use a different debug signing key on a subsequent build. If Android rejects an update for a signature mismatch, uninstall the old prototype first (this resets its settings), or configure a stable signing key for ongoing development. Do not put a private signing key into a public repository.

## Install on the S6

### File transfer / USB drive

1. Copy the built `app-debug.apk` to a USB drive, or transfer it to the device's Downloads folder using an available file-transfer app.
2. Open the APK using the device's file manager. If prompted, allow that specific file manager to **install unknown apps**, then choose Install. Menu wording depends on the S6 firmware.
3. Open **Roam Launcher** from the installed-apps screen and test the remote and rotation.
4. Copy your aerial MP4 files to local storage. Choose **Choose aerial videos**, select one or more files, then **Start screensaver**. A compatible Android document picker is required; not all TV firmware includes one.
5. Once navigation works, open **Default home app** and select Roam Launcher if offered. Alternatively press Home and select it if Android presents a chooser. Firmware may not permit third-party default home apps.

Keep the original launcher installed. To undo, choose the original launcher in Settings → Apps → Default apps → Home app, then uninstall Roam Launcher. If no Home chooser exists, this prototype can still be opened as a regular app; it does not force or modify vendor launcher settings.

### ADB from a computer

Install Android SDK Platform Tools. Enable developer options and USB debugging on the S6 if its firmware exposes these. Connect through a USB port that supports device data/debugging and accept the device authorization prompt. Some TVs have host-only USB ports; use the device's supported Wireless debugging pairing flow if available rather than assuming a USB connection will work.

```sh
adb devices
adb shell getprop ro.product.model
adb shell getprop ro.build.version.sdk
adb install -r app-debug.apk
adb shell am start -n dev.roam.launcher/.MainActivity
```

For firmware with a Wireless debugging pairing screen, use the addresses and ports displayed there (pairing and connection ports can differ):

```sh
adb pair DEVICE_IP:PAIRING_PORT
adb connect DEVICE_IP:DEBUG_PORT
```

Open Remote test, then capture events:

```sh
adb logcat -s RoamKeys:I
```

With Roam Launcher visible, inject standard keys individually:

```sh
adb shell input keyevent 19
adb shell input keyevent 20
adb shell input keyevent 21
adb shell input keyevent 22
adb shell input keyevent 23
```

To remove the prototype:

```sh
adb uninstall dev.roam.launcher
```

## Acceptance checks on hardware

- Confirm model is S6 and Android API >=23; record build/model from Remote test.
- All six probe tiles reachable by remote; one activation per OK press; held arrows repeat; Back exits the diagnostic screen.
- Every app/control reachable by arrows in both orientations; partial final row reachable; focus remains visible when scrolling.
- Touch scroll/click and pointer scroll/click work; switching back to arrows restores a visible selection.
- Returning from an app restores selection; installing/uninstalling an app refreshes the grid on resume.
- Rotate at top and bottom of the grid; selected app survives and rows reflow.
- Choose local video, restart launcher, and confirm persistent playback access. Test a multi-video playlist and invalid/deleted/unsupported files.
- Screen saver starts only after idle on home, stays muted, exits on input, and stops when launcher loses foreground. Idle-off prevents automatic start.
- Verify Home default selection and recovery to the vendor launcher before relying on the prototype.

## Validation status and next iteration

Static XML and source/package consistency checks were run during authoring. Automated navigation tests are included for row boundaries, partial rows, invalid/empty inputs, and reachability through 1,001 tiles in both column counts. They require the build toolchain and **have not been executed here**. No visual or physical-remote validation is claimed.

Next device-driven work: use the captured remote report to add any necessary opt-in key mappings; tune tile sizing for the S6's actual density; add user-defined rows/reordering and licensed curated aerial downloads if desired. The current visual treatment is a functional prototype, not a pixel-matched Apple TV replica.

References: [Android TV navigation](https://developer.android.com/training/tv/get-started/navigation), [TV controllers](https://developer.android.com/training/tv/get-started/controllers), [Android intents](https://developer.android.com/reference/android/content/Intent).
