# Emulator test harness

Emulator evidence only (see `docs/EMULATOR_TESTS.md`); it does not replace real-device or Orbot tests.

Prerequisites: Android SDK with an arm64 Android 15 `google_apis` AVD, `tor` on the host (SOCKS on 127.0.0.1:9050), the debug APK.

```bash
tor -f torrc   # SocksPort 127.0.0.1:9050
emulator -avd veil-test -read-only -port 5554 -no-window -no-audio &
emulator -avd veil-test -read-only -port 5556 -no-window -no-audio &
adb -s emulator-5554 reverse tcp:9050 tcp:9050
adb -s emulator-5554 install -r apps/multiplatform/android/build/outputs/apk/foss/debug/android-foss-arm64-v8a-debug.apk
python3 -I scripts/emulator/ui.py emulator-5554 texts
```

Notes: the app uses a secure window, so screenshots are black; read the UI with `ui.py texts`. Pair devices by opening the invitation link on the second device: `adb -s emulator-5556 shell am start -a android.intent.action.VIEW -d '<link>' io.github.yigitbuyukeren.veil`. Do not start more than two emulators on a 16 GB host (System UI ANRs).

Repeatable scenario (apps past onboarding, on the chat list, Tor up and reversed on both):

```bash
python3 -I scripts/emulator/scenario_group.py emulator-5554 emulator-5556
```

It creates a group on A, reads the group link from the share sheet, joins B through it, sends a message each way and checks that each app's TCP sockets go only to `127.0.0.1:9050`. Coordinates assume a 1080x2400 screen. Exit code 0 only when every step passed. It does not run in CI (needs emulators and Tor).

App data lives in the `-read-only` overlay and is lost when an emulator is stopped; after a restart onboarding data may be gone, so re-pair before running scenarios.

Other scenarios: `scenario_abuse.py <A> <B>` (deleted link, observer role), `scenario_background.py <A> <B> <chat title>` (background/Doze; needs a healthy pair, see E27). Helpers are in `emu.py`.
