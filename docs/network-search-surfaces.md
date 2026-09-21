# Adaptive output and navigation increment

New Auto playback and bounded recovery optionally measure the paired gateway link.
The shared transport uses one fixed-size streamed sample, without retaining its
body or following redirects. Missing support, cooldown and network errors do not
block ordinary resolution. Settings → Advanced can disable measured adaptation;
forced playback modes retain their explicit behavior.

Global Search has a pure ViewModel with a 400-ms debounce, one request in flight
and only the latest pending draft. Obsolete replies never replace current results.
A recycled ListView shows contextual provider labels, partial-provider failures,
empty/loading/error states and actions to continue into provider browsing. The
existing query overlay survives Activity recreation. Full cross-feature history
and all provider-specific account/actions remain separate work.

## Video output selection

SurfaceView remains the API9 baseline. The isolated `Api14TextureOutput` is loaded
by class name only on API14+. Auto uses it on handheld layouts or after a measured
SurfaceView reattach failure, and only after a successful Texture output probe.
Advanced allows Auto/SurfaceView/verified TextureView; overriding cannot bypass
API availability, failed/unknown evidence or a recent failure circuit.

Diagnostics uses the signed baseline fixture for an additional `texture-output`
probe. PASS requires completion, position advancement and at least three actual
TextureView update callbacks. Preparation alone is not evidence. Old APIs, missing
hardware acceleration, cancelled or uncertain runs remain UNKNOWN. Probe evidence
and operation health are invalidated by firmware/app/surface-suite identity.
Settings apply when the screen is reconstructed; no probe has been run physically
by this development checkpoint.

UI and decoder own separate retained references to Texture output. A destroyed
View gives up its reference, but Surface/SurfaceTexture are released only after
asynchronous player detachment. Attachment errors record backend health and switch
the UI to the baseline output; session recovery retains its existing finite policy.
This follows the platform's [SurfaceTexture ownership contract](https://developer.android.com/reference/android/view/TextureView.SurfaceTextureListener).
Both factories and media calls remain outside domain/ViewModels.

This is an alternative platform output backend, not an OEM DIAL/CEC driver.
Android's [CEC service](https://source.android.com/docs/devices/tv/hdmi-cec) is a
system integration; detecting a firmware service does not give a sideloaded app
its permissions or a compatible OEM control contract. Those adapters remain open.
