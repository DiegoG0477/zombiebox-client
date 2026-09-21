# Playback tracks and navigation

The playback feature owns semantic track/cue models, a repository port, a gateway
adapter, a session-aware ViewModel and a platform dialog. The Activity composes
these parts and displays plain subtitle text over the video surface. Provider
DTOs, JSON and network requests remain in data adapters.

Audio switches preserve the source timeline through `timelineOffsetMs`; decoder
positions are relative to each new transcode. Subtitle selection survives audio
switches. Generation guards reject stale inventory/cues and release abandoned
replacement plans. Off, stop and new content clear subtitle state. The transport
controls and D-pad focus list respect the plan's actual seekability.

Track controls currently apply to gateway-local embedded media with FFmpeg
available. Remote tracks, sidecars, automatic language policy and native track
selection remain open. Text overlays simplify ASS styling and cannot render
bitmap subtitles. JVM tests verify timing, seeking, overlaps and late replies;
physical decoder timing, SurfaceView composition and remote-control review remain
unverified.

Home navigation uses independent selection and focus states. Inactive tabs are
transparent; the active tab alone is filled with its provider accent. Hero actions,
service tiles and content cards have distinct roles, and resume bars require a
known duration. The supplied reference remains the design target; the sidebar,
handheld refinement and virtualized/cache budgets are still pending.
