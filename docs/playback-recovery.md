# Foreground playback lifecycle and recovery

Dev.13 implements part of specification sections 13–14, 44 and 61. It does not
complete the shared screen stack, background playback or OEM surface strategy.

`PlaybackIntent` is an Android-free policy used by the media adapter. Explicit
play/pause intent survives temporary foreground/surface loss. Video waits for its
surface; audio does not need one. Returning to the foreground resumes only when
the user still wants playback and audio focus is available. No service or Activity
owns a second decoder for this behavior.

`EmbeddedPlayer` serializes platform operations on its media looper. Generation
checks and player identity reject stale size/state/completion/error callbacks.
Platform exceptions become FAILED instead of escaping the looper. Preparation,
seek completion and reported network buffering have bounded waits. Failed players
retain their last known position for recovery; state does not reset history to
zero just because the decoder was released. Explicit stop still clears state.

`PlaybackViewModel` owns the finite compatible-retry policy and request generation.
The failure dialog offers Retry compatible / External player / Close. A retry
persists progress best-effort, revokes the old session, re-resolves the source and
passes the exact position in the new playback request. A newer user action or
closed ViewModel discards a late plan. The order is DIRECT_PLAY → REMUX → TRANSCODE;
mid-item recovery skips REMUX because this implementation cannot seek that output
accurately. Failed attempts advance rather than restarting the same mode forever.
External fallback obtains a fresh ticket before handing it to another app.

Live input resumes at its current edge, never at a stored VOD position. Incoming
Cast/Spotify/AirPlay keeps its dedicated receiver recovery policy. Player retry is
an explicit user action and does not manufacture new decoder capability evidence.

Remaining: Activity recreation/process-death restoration, navigation-independent
session ownership, background music, complete receiver arbitration, next-item
policy, remote tracks/language selection and measured OEM surface-recreation
fallback. Real seek timing, A/V sync, focus and API9/10/13 behavior need hardware.

Platform reference: [Android MediaPlayer state and display contract](https://developer.android.com/reference/android/media/MediaPlayer).
