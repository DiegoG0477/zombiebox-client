# Recovery, automatic reception and details

The service-scoped playback ViewModel retries ordinary failed playback at most
three times, with 2/4/8-second minimum delays checked by the service clock. The
first attempt resolves a fresh AUTO plan, the second requests compatible conversion,
and the third requests LOW quality. Stop and newer playback invalidate late plans.
The previous conversion is revoked before replacement to preserve the one-job
budget. Queue, position and subtitle intent remain; live recovery starts at zero.
Only one minute of stable playback restores the retry budget. Incoming receivers
keep their existing separate recovery policy.

Advanced → Automatic recovery applies to the next requested playback. Forced
playback-mode overrides disable automatic recovery. This is reactive failure
recovery, not measured throughput/decoder certification. A failed final attempt
returns to the existing manual retry/external-player UI.

Receive Spotify / AirPlay → Automatic arms both workers. New confirmed sender
activity can take over; metadata changes alone cannot. Existing Cast/YouTube
transport exclusion remains. Physical A/V, interruption/resume and account
acceptance are still required.

Details use gateway-provided artwork/semantic metadata, bounded text/programme
summaries and explicit Resume / Play from beginning actions. They keep provider
accent colors and native D-pad scroll/buttons. No provider JSON enters the UI.
