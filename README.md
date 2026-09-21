# zombiebox-client

Legacy-first Android TV/handheld client; one stable client APK.

This is an independent repository in the Zombie Box workspace. Remotes and hosted
releases are not configured yet; local commits/tags and dependency pins are real.

Application ID: `io.github.diegog0477.zombiebox.client`; experimental minSdk9.

```sh
make deps-check  # ../zombiebox-protocol or ZOMBIE_PROTOCOL_DIR
make build test
```

Android SDK35/build-tools35.0.0 and JDK21 are the current candidate toolchain.
Android Studio is optional. The independent Gradle build includes `:app` and the
shared library from the pinned protocol repository; it does not include Cast.
`make deps` can restore `.deps/zombiebox-protocol` after a remote is configured.

Features use `domain/model`, `domain/repository`, `data`, `presentation/viewmodel`,
`presentation/ui` and isolated `platform` classes where needed. ViewModels consume
semantic values and injected repositories; UI never decodes JSON/calls HTTP.
Views/XML, MediaPlayer/SurfaceView and manual DI preserve the legacy contract.
No modern AndroidX/Compose/coroutines/JNI. Guard modern APIs through factories;
verify Dalvik on physical hardware separately. Home stays green; providers retain
contextual accents. Keep English defaults and Spanish variants in string resources.

Output: `app/build/outputs/apk/debug/app-debug.apk`. Tracks/subtitles, nested provider
navigation, virtualization and adaptive health/OEM selection remain unfinished.

## Development rules

Run `make format` and `make format-check`. Formatters are pinned and downloaded
on first use. See [AGENTS.md](AGENTS.md), [history provenance](docs/history.md),
[component work](docs/PLANNING.md) and [local milestone registry](docs/milestones.json).
The central workspace owns product-wide ADRs, the original specification, the UI
reference, M0–M11 exit gates and the complete development/validation gap audit.
Physical devices over USB/ADB are the default; automated checks do not establish
legacy runtime or end-to-end account/media compatibility.

Dev.10 adds [playback tracks and refined Home navigation](docs/playback-tracks.md).
Local audio/text-subtitle controls use feature MVVM and the shared gateway transport;
active section selection is independent of remote focus. Physical visual and media
validation remains pending.
