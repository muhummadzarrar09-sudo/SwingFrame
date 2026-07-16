# Phase 3 — Local project library and bookmarks

Version: `0.4.0-projects`

## Local project index

- Completed analyses are added to an app-private recent-project index.
- Home shows project name, update date, frame count, last-opened frame, and bookmark count.
- Opening a recent project reuses the persisted document URI, rebuilds the exact timestamp index, restores annotations, and resumes the last frame automatically.
- Project metadata is refreshed when the source is reopened.
- Projects can be removed locally from Home.

## Source re-linking

If Android loses access or the source file moves:

1. Tap the link icon on the recent project.
2. Select the replacement source with the system picker.
3. SwingFrame updates the source URI while retaining the stable project identity and bookmarks.
4. Existing annotation vectors are migrated to the replacement URI.

The replacement should represent the same clip; geometry cannot be guaranteed to match a different video.

## Bookmarks

- Presets: Address, Top, Impact, Finish.
- Custom labels up to 40 characters.
- One bookmark per frame; saving again updates that frame's label.
- Amber ticks render on the full timeline.
- The current bookmark label replaces the generic TIMELINE label.
- The bookmark list supports jump and delete actions.
- Bookmark frames are validated against the source when a project is reopened.

## Persistence

- Project index: `files/projects/index.json`
- Annotation vectors: `files/annotation-projects/<source-hash>.json`
- Both use Android `AtomicFile`.
- Last-frame changes are debounced to avoid storage writes during every scrub event.
- No database, account, cloud, or network permission is required.

## Build later

Per the current workflow, do not build until all planned implementation phases are complete. When authorized, use `swingframe-build.ps1` from the project root.

## Test plan for the eventual build

1. Import and analyze a clip.
2. Add Address, Top, Impact, Finish, and a custom mark.
3. Scrub and verify amber ticks.
4. Open the bookmark list and jump to each mark.
5. Exit to Home and reopen the recent project.
6. Verify the last frame, bookmarks, and annotations return.
7. Re-link the project to the same source selected again.
8. Delete a bookmark and confirm persistence.
9. Delete a recent project and confirm it leaves Home.
10. Confirm no network permission exists.
