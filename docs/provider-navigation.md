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
