# Provider navigation (dev.11)

The catalog feature follows MVVM. `GatewayCatalogRepository` translates the shared
wire transport into semantic items/pages; `CatalogViewModel` owns a bounded
24-snapshot navigation history, locations, stable item IDs and scroll offsets.
The UI owns native controls, dialogs and list geometry, with no JSON/HTTP calls.
Cancelled/superseded requests cannot replace the current page; failed navigation
preserves its previous snapshot. Back returns without another provider request.

Plex/Jellyfin/Stremio “View all” uses hierarchical browsing; folders navigate and
playable entries open details. Next follows the gateway's returned offset, including
short Stremio boundary pages. Search stays within the current provider/node and
shows its scope. Root/season filtering and catalog search capabilities vary by
provider. Loading, cancel, empty results and return paths are explicit.

`CatalogListView` uses API-9 native ListView recycling with a bounded viewport
(roughly five rows at the current height). It never inflates all page entries or
allocates hidden artwork. Folder and playable rows share semantic rendering;
unknown kinds use the same text fallback. Provider accents come from shared color
resources and selected rows retain an outline. Returning restores the selected
item by ID and its relative vertical position; touch selection is recorded too.

This addresses spec sections 17, 20, 22, 42–44 and 69 for the provider browser.
It does not complete the horizontal Home-row virtualizer, low/normal-memory tier
budgets, a reusable bitmap cache, all semantic screen primitives, debounce/global
search switching, a unified player/back stack or handheld refinement. Those remain
in the workspace gap audit. Full-screen poster/episode layouts and visual matching
to the concept remain unfinished; the browser currently uses a native dialog host.

JVM tests cover history, scope, server offsets, stale callbacks and failure
preservation. Build/lint is not a physical D-pad, Dalvik or scroll benchmark.

## dev.12 receiver integration

Settings → Receive Spotify / AirPlay explicitly selects one shared foreground
output. The receiving repository reads Cast first, then the media receiver. The
same ReceiverViewModel owns interruption snapshots, metadata updates and up to
three consecutive reconnect attempts with backoff. It preserves a plan across
network errors and restores the interrupted playback on confirmed end. Local stop
suppresses the incoming item until sender idle or explicit re-arm. The audio panel
shows title/artist/state; Play/Pause controls the selected Spotify provider.

Reception stops renewing when the Activity leaves the foreground and expires on
the gateway. Re-select the provider after lease expiry. Cross-Activity/background
music ownership, artwork and the full Home/catalog/player stack are still pending.

## dev.39 search return path

Global search now retains one result set (at most six sections of twenty items)
for sixty seconds. Returning from details, playback or a provider root restores the
query, selected semantic item and list geometry without issuing another request
inside that window. Older results are fetched again; explicit Search always
refreshes. A query edit resets its viewport. Gateway/profile or provider
configuration changes discard the retained results and pending callbacks.

Provider roots opened from search expose Back to search; nested catalogs still
use their existing bounded history. Loading cancellation/failure also returns to
search if no provider page has been loaded. Close explicitly exits the route.
An old dialog's delayed dismissal cannot detach a replacement search observer.

Activity saved state contains only the query/focus/scroll bookmark, alongside the
existing catalog path. Pages, credentials, bitmaps and provider DTOs are not saved.
Recreation fetches results again; a search-owned details dialog falls back to its
search results instead of serializing the selected media object. This is a bounded
return path, not durable multi-query history or a unified browser/guide stack.
Those remaining navigation boundaries stay open in the workspace gap audit.

Host fixtures cover freshness expiry, stale requests, bookmark restoration and
profile reset. Android dialog/focus behavior still requires the deferred physical
acceptance phase. No device or visual-match claim follows from those fixtures.
