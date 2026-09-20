# Android client

From the root: `make android-build && make android-audit`. Output: `app/build/outputs/apk/debug/app-debug.apk`. No AndroidX, Compose, player or native libraries. The spike uses one Activity and fetches health on a worker with timeouts and a bounded response.

IntelliJ can edit the project; Gradle Wrapper is the build reference. Use JDK 21 and the SDK at `$HOME/Android/Sdk`. The app accepts a gatewayUrl ADB extra or an editable address. Never put provider credentials/URLs here.

English strings live in res/values/strings.xml. EN/ES localization and locale settings come later. Static success does not validate API 9/10/13; read the legacy spike checklist before implementing providers.
