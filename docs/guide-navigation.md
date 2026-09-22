# Paged IPTV guide

Dev.41 keeps the guide on a bounded catalog page and adds Previous channels / More
channels controls beside the existing shared time navigation. It uses the same
catalog ViewModel, server-owned next offset, scoped history and repository as the
provider browser. Rows remain recycled; opening the guide never downloads the whole
playlist. Existing history is bounded to 25 catalog bookmarks.

The selected time survives page transitions within the rolling 48-hour window.
Previous channels follows visited pages in the same provider, folder and query;
it never backs into a parent folder or unrelated search. Channel focus is retained
when returning to a visited page and when a refresh reorders channels. The catalog
underneath follows the selected guide page, so closing the guide returns there.

A pending page request disables page controls. Failure retains the current page
and exposes an in-place retry message. Restored previous pages are removed from
history only after loading succeeds. Closing or replacing the guide fences pending
loads, and an old dialog's delayed dismissal cannot cancel its replacement.
Background guide refresh and explicit paging do not start competing requests.

The owned guide dialog now participates in scoped Cast Remote D-pad routing. It
adds no remote typing field and changes neither pairing nor consent boundaries.
English/Spanish strings and IPTV accent remain shared resources.

Host JVM tests cover scoped page return/focus, restored-page failure/retry and
closing during a request. Build/lint checks are separate from deferred physical
focus/touch acceptance. Unified guide/player/browser history and remaining provider
specific actions are still open; this increment does not claim those complete.

## Playback return (dev.42)

Play captures the selected guide channel and time before the window is dismissed.
Explicit Back or minimize consumes that return point and reopens the guide on its
catalog page while the service keeps playing. Catalog details return to the same
item; direct search results return to the saved search/focus. Home details remain
Home-owned. These bookmarks contain IDs and bounded semantic fields only.

The pending point is saved even while all catalog windows are hidden. A recreated
Activity retains the complete page path while reloading, including on failure;
Back can supersede that load without accepting a stale response. A failed return
reload retains intent for retry. New provider/Home navigation, independent Home
playback and profile/configuration reset discard old intent. Receiver handoff and
lifecycle restoration do not independently open catalog windows.

JVM coverage checks pending restoration, full-path failure/retry, single consumption
and provider/profile invalidation. Actual dialog focus and background playback are
not physically validated. This implements catalog/player return, not a universal
stack across Browser, settings and every provider action.
