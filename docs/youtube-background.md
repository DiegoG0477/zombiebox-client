# YouTube receiver lifetime

The dev.38 implementation moves the TV-code lease, command polling, acknowledgements
and playback resolution into the existing playback service. Activities bind to
semantic `YouTubeReception` state and send enable/disable/standby actions. Leaving
or recreating a screen detaches observers; it does not revoke the selected receiver.
Provider credentials remain on the gateway and no native library is added.

Enable reception while the app is visible. An ongoing notification provides Stop
and a route back to the player. Idle listening uses the `connectedDevice` foreground
service type; playback adds `mediaPlayback`. The API29 three-argument promotion is
isolated behind a guarded factory. Existing multicast permission satisfies the
connected-device declaration prerequisite; Android34's type permission is declared.
There is no boot/background Activity launch and no automatic process-death restart.
See [Android's service-type rules](https://developer.android.com/develop/background-work/services/fgs/service-types).

Commands resolve against the active receiver lease and are fenced on disable,
standby, replacement and shutdown. Playback success is acknowledged only after
BUFFERING followed by PLAYING. Pause/seek/volume retain semantic acknowledgements.
The shared playback session restores an interrupted local queue on remote Stop or
lease expiry without requiring an Activity. Gateway/profile changes disable the
old listener before the serialized transport switches authority.

Opening retries at most three times with backoff; explicit Enable can retry after
a failure. Transient polling failures preserve the lease and retry on the next
bounded tick. An expired/revoked lease disables reception: it never automatically
reclaims a receiver that another device may now own. Notification Stop disables
listening and stops playback. A normal remote Stop leaves listening enabled.

Idle listening releases playback wake/audio-focus ownership. Android Doze/OEM
suspension, surface detach/reattach, notification restrictions and actual background
A/V still require the deferred physical acceptance. A foreground service is not a
guarantee against process killing. Automatic background discovery/handoff between
all non-YouTube receivers remains a separate work item.

Host evidence: JVM command/lifetime tests, APK/lint and no-native-library audit.
Those checks are not Android API9/13 or modern-device runtime acceptance.
