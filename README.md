# Android client

From the root: `make android-build && make android-audit`. Output: `app/build/outputs/apk/debug/app-debug.apk`.

One Kotlin APK uses native Views, MediaPlayer/SurfaceView and HttpURLConnection. No AndroidX, Compose or native libraries. The initial Home follows the near-black/green reference with navigation, hero, media rows, service state and a bottom player. It supports pairing, service credential submission, basic diagnostics, local EN/ES preferences, catalog pages, IPTV programme details, fullscreen/miniplayer, playback controls and an external-player intent.

IntelliJ can edit the project; Gradle Wrapper is the build reference. JDK 21 and the Android SDK are selected by the repository helper. Android Studio and emulators are not required. Enter a real gateway LAN address in Settings; there is no emulator-specific default address.

Provider credentials are transient form input, sent only to the gateway and never persisted in Android preferences. Only gateway/device credentials and presentation preferences are stored locally; backups are disabled. See [configuration](../docs/development/services-and-credentials.md).

Build/lint/DEX success is not API 9/10/13 runtime validation. Codec probes, subtitles/audio selection, image delivery, refined handheld layout, interruption/device lifecycle validation and physical focus/surface behavior still need implementation or device verification. No real provider/device playback has been claimed.
