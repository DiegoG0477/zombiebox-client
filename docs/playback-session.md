# Service-owned playback (dev.14)

`features/playback/platform/PlaybackService` is the Android composition root for
one decoder, audio focus, notifications and gateway repositories. Pure
`PlaybackSessionViewModel` owns immutable semantic session/queue/progress state.
`PlaybackConnection` is the Activity binding adapter. Activities do not own the
service's transport or decoder, and ViewModels contain no Android objects/DTOs.

Opening another screen, pressing Home or recreating the Activity leaves the
session alive. A new Activity reattaches its surface and renders the same session,
position, explicit pause, next-content queue and chosen subtitle ID. Subtitles
are fetched again by the screen repository; bitmaps/Views are never retained.
Fullscreen/miniplayer state uses the Activity instance-state bundle.

Playback starts a foreground media service with play/pause, Next and Stop actions.
Notification channels (API26) and permission requests (API33) are verifier-isolated.
Denied notifications do not force playback to stop. Audio focus loss pauses;
background continuation never overrides explicit user pause. A partial wake lock
is held while playing/buffering with a six-hour bound and released on pause/stop.
Foreground video waits for its new surface; background playback can continue audio.
The firmware's actual no-surface behavior still needs physical validation.

Next uses the originating Home row or current provider page. Playable leaf items
are retained in order (max200); catalog cursors preserve provider/parent/query and
fetch at most three empty pages per action. Ended VOD auto-advances once. Live
channels require manual Next; external receiver sessions do not use this queue.
New selections, Stop and manual recovery invalidate late queue results. Errors
leave Next available for explicit retry. The service owns progress writes and
revocation; incoming reception uses the receiver stop operation.

The service is START_NOT_STICKY: process death does not silently restart media or
restore a queue. A subsequent explicit Play uses gateway history. This is not a
claim of process-death restoration, OEM health fallback, fully restored navigation
screens or complete Spotify/AirPlay recovery. The four dev.14 items are implemented;
product M2/M3 acceptance and broader track/language policy remain open.

Validation: JVM session/queue/cancellation/intent tests, APK build/lint and static
legacy audit. Physical USB tests must cover recreation during playing/paused media,
Home/return, another Activity, screen off, focus interruption, notification actions,
completion/page boundaries, subtitle restoration and conversion on a real decoder.
