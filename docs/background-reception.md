# Background media reception

Dev.40 extends the playback service's lifetime to idle Spotify/AirPlay and paired
Cast reception. Opening Client reads the gateway's existing receiver preferences;
only enabled reception arms the listener. Discovery alone never grants access.
The gateway remains the exclusive ownership and pairing authority.

A pure `BackgroundReceptionViewModel` owns polling and recovery policy. The service
injects its repository, serialized worker, main-thread delivery, player and clock.
The foreground Activity retains its UI coordinator; the service polls only while
that UI is absent. At most one request is in flight. Profile, visibility, shutdown
and playback-session changes fence late results. Polls do not reselect a receiver,
reclaim an expired lease or modify server preferences.

Idle reception keeps an ongoing connected-device notification with Stop. Stop
disarms local listening and stops playback; reopening Client can arm the existing
gateway preferences again. No boot listener or automatic process-death restart is
added. Idle listening releases playback wake/audio-focus ownership. Android may
still suspend or kill the process; host tests cannot establish OEM behavior.

A new incoming session preserves the original interrupted local queue, position
and pause intent across receiver replacements. A confirmed empty receiver restores
that session once; a network error preserves it and reports unavailable reception.
Live failures retry at most three times with backoff; sixty seconds of observed
PLAYING resets that budget. Finished VOD waits for the gateway's next queue item
instead of replaying the finished file. External-player plans require a visible
user action and never launch an Activity from the background.

Audio uses the existing background player. Video waits for the visible screen's
surface; this feature does not secretly open or turn on the TV screen. YouTube
retains its [separate leased service policy](youtube-background.md); incoming media
puts that listener into standby before replacing playback.

Evidence: eight JVM policy tests cover idle arming, stale/in-flight fencing,
network loss, restoration, bounded live retries, VOD completion and shutdown.
Actual background A/V, Doze/OEM restrictions and legacy runtime behavior remain
in the author's deferred physical acceptance track. Metadata support versus worker
or account failure still needs broader semantic coverage.
