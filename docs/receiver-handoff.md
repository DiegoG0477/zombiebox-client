# Receiver handoff

Dev.22 selects media/YouTube with an explicit replacement request. The gateway
prepares ownership before retiring competing transports. Existing clients retain
first-armed exclusion unless they send the new flag.

The Cast receiver settings include **Allow receiver handoff**, disabled by default.
It is server-owned consent for an incoming Cast to replace another armed transport;
Cast receiving must also be enabled. The sender cannot change this preference.
Normal target-side service selection is already an explicit choice.

YouTube is now adopted as an incoming service-owned playback session. It preserves
the interrupted ordinary queue, position, pause and subtitle intent through further
receiver replacements. Stop, disable, confirmed lease loss and leaving the foreground
restore that ordinary session if one was saved. Receiver streams are not saved as
ordinary process-death resume bookmarks. A replaced remote sender is not re-armed.

The data adapter maps confirmed lease loss to a domain exception. Network failures
remain retryable; the ViewModel drops only confirmed expired ownership. Pending
commands and late resolved plans cannot execute after disable/lease loss. Playback
resolution carries the YouTube receiver ID so the gateway checks ownership again.

Physical A/V, D-pad and foreground transitions remain unverified. This is not
simultaneous automatic reception across every protocol or native/OEM DIAL support.
