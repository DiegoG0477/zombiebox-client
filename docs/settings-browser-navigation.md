# Settings and Browser navigation

Dev.43 retains Settings and Advanced/provider list parents while opening child
surfaces. Back/Close returns to the existing parent and its selected row. Gateway
Services can return through Android's Activity stack to the same settings list.
Recreation saves only up to two allowlisted menu IDs and bounded row positions.
Provider credential drafts, operator codes and consent dialogs are never saved or
replayed. Children reopen at their parent menu after recreation. Pending provider
loads are fenced when the menu closes or another action is selected.

Provider/pairing forms clear sensitive fields and close their discovery resources.
Cancelled/closed forms cannot activate a late pairing result or render a late save.
Resume playback explicitly closes the menu stack so it does not cover the player.
Settings remain excluded from companion remote typing/control consent surfaces.

Browser has stable semantic keys across its address bar, page, control row and text
entry. Control focus is restored after recreation. Page arrows still go to the
remote website; Back exits page control to the Page button, and Back outside page
control or the visible Close button returns to Home. A visible EN/ES hint explains
this distinction. Native text cursor movement is preserved. Expired sessions clear
stale displayed frames; no submitted page text is saved. Phone insets use the same
guarded policy as the main client.

Host navigation-state tests and build/lint cover wiring and bounded state. Physical
D-pad, keyboard, screen-reader and touch acceptance remain deferred. This is not a
persisted universal stack of every settings form or an offline browser session.
