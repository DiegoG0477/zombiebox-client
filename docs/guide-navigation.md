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
