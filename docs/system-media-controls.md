# Native system media controls

Dev.32 publishes a framework MediaSession on API21+ while playback owns a plan.
The service owns its lifetime across Activity recreation/background playback.
The API21 class is loaded reflectively through a guarded factory; API9/13 retain
existing app/notification controls with no reference to the new session class.
No JNI, AndroidX, broadcast receiver, restart-on-media-button or new permission is
introduced. The session ends when playback stops or the service is destroyed.

The domain derives semantic state/actions without Android types or provider DTOs.
Play/pause are explicit and idempotent; next is exposed only for available queue
continuation. Seek is exposed only for a seekable non-live plan with known duration
and is clamped/subtracted against the current conversion timeline offset. Mirroring
and live AirPlay expose Stop; Spotify routes explicit pause/resume through its
existing gateway adapter. The API callback and service both recheck availability.

API21+ notifications use MediaStyle with the service-owned session token, while
API26+ retains the low-importance channel. Metadata contains bounded title/subtitle
and timeline values, never a gateway stream URL or credentials. Existing controls
remain available if session construction/update throws. Two operation errors open
a five-minute cooldown persisted by firmware/app/backend revision. API operation
success clears that error circuit; it is not a functional Bluetooth/CEC/playback
probe. Stopping/disabling releases the session; late callbacks are ignored.
Advanced > System media controls offers Auto/Disabled, applied on the next state
update. It cannot enable the API on an older OS or grant OEM/system permissions.

Historical Serenity's VideoPlayerKeyCodeHandler and current
VideoKeyCodeHandlerDelegate separate play/pause/next/seek intent; this implementation
uses those behavioral distinctions through our service and pure action policy.
No upstream code or modern playback dependency was copied.

Host tests cover action masks, incoming/live limitations, timeline bounds, unknown
commands, failure cooldown and release lifecycle. System UI rendering, headset/
Bluetooth/HDMI events and Android verifier behavior remain physically untested.
DIAL/CEC/root/vendor-specific backends are separate unfinished work.

Primary references: [MediaSession lifecycle/callbacks](https://developer.android.com/reference/android/media/session/MediaSession),
[PlaybackState actions and timeline](https://developer.android.com/reference/android/media/session/PlaybackState.Builder).
