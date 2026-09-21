# Media, guide and diagnostic increment

- Settings → Media languages configures ordered audio/subtitle languages and subtitle
  policy on the gateway. Preferences apply to new AUTO sessions. Track selection
  also supports remote embedded text and local sidecars through semantic cue APIs.
- Settings → Resume last playback explicitly resolves a new plan after process
  death. The checkpoint contains at most 200 remaining items, cursor, position and
  subtitle selection, expires after seven days and is bound to the active pairing.
  It contains no provider credentials, URLs or old session tickets. Stop clears it;
  receiver sessions do not overwrite prior ordinary playback. No boot autoplay.
- IPTV configuration accepts XMLTV mappings. Guide time/channel and search drafts
  survive Activity recreation; the open guide refreshes every minute through the
  gateway cache and offers up to 48 hours of programmes for the current channel page.
- Activity-owned image workers decode RGB565 derivatives outside the UI thread.
  A five-minute LRU retains up to 4 MiB on low-memory devices, 8 MiB otherwise,
  with one Hero entry. Encoded cache remains 2/4 MiB. Binding generations reject old
  callbacks; pairing changes clear caches. Eviction drops references without recycling
  bitmaps still used by views. Visible images/in-flight allocations are additional
  to the cache bound and still require physical memory/scroll acceptance.
- Diagnostics → Export previews an allowlisted report; Share launches Android's
  chooser only on explicit user action. Devices without a share target can read
  the report. No upload endpoint or automatic reporting is introduced.

MVVM remains feature-oriented; Android bitmap/persistence/UI work stays outside
ViewModels. API9 is still an unverified experimental target. No Android emulators
or physical compatibility claims are introduced by these automated checks.
