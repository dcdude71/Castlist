# Fork notes

Handoff notes for working on this fork (on any machine, with or without Claude Code). Upstream `AGENTS.md` still applies.

## Repo setup
- Fork: `dcdude71/Castlist` (`origin`, renamed from `dcdude71/AntennaPod` in v0.9.3). Upstream: `AntennaPod/AntennaPod` (add it as `upstream`).
- All fork work is on `develop`, rebased on upstream `develop`. The old `fork-setup` and `playlists` branches are gone; their commits are on `develop`.
- App name: "Castlist" (debug: "Castlist Debug"), set in `app/build.gradle`. It was "AntennaPod Mix" up to v0.9.2. Release APKs are named `Castlist-<tag>.apk`.
- Application id: `io.github.dcdude71.castlist` (was `io.github.dcdude71.antennapodmix` up to v0.9.2), so it installs next to the official app. Icon sources: `art/castlist-icon/`.
- Version: `app/castlist-version.properties` holds `versionName` and `versionCode` (read at the end of `app/build.gradle`; upstream's values in `defaultConfig` are overridden). Bump both in a commit before tagging. Scheme: `versionCode = 4000000 + major*10000 + minor*100 + patch` (0.9.8 = 4000908, 1.0.0 = 4010000), always above upstream's 3130001 so installs keep updating. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (500 characters max) for each release.
- Store listing: `fastlane/metadata/android/en-US` (title, descriptions, icon, screenshots from `docs/images`). Upstream's `store-metadata` submodule was removed.
- F-Droid: draft recipe in `fdroid/io.github.dcdude71.castlist.yml` (free flavor, same Wear OS prebuild as AntennaPod's recipe, `UpdateCheckData` reads the version file). A local check of what F-Droid runs: apply the prebuild `sed`, `./gradlew :app:assembleFreeRelease`, then restore the two Wear OS build files.
- GitHub releases ship the **play** flavor since v0.10.2 (Wear OS link and Chromecast), plus the watch APK `Castlist-Wear-<tag>.apk`; F-Droid builds the free flavor itself. The two are signed with different keys, so users can't switch between them without reinstalling. Local release build: `:app:assemblePlayRelease :app-wearos:assemblePlayRelease` with the signing properties.
- Releases: push a `v*` tag and `.github/workflows/fork-release.yml` builds and signs the APK. The keystore and passwords are GitHub secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`), so nothing needs to be copied between machines. A tag with a dash (`v0.7.0-beta.1`) is published as a GitHub pre-release; Obtainium users get it only with "Include prereleases" on.
- If a tag push doesn't start the release build (happened with v0.7.1, published from a local build), push the tag anyway, then start it by hand: GitHub → Actions → Fork Release → Run workflow, entering the tag. Or from the command line: `gh workflow run fork-release.yml -R dcdude71/Castlist -f tag=v0.7.2`. A local build needs `-PforkVersionName` and the signing properties, exactly as in the workflow.
- `local.properties` and `.idea/` are machine-specific and not committed.
- Upstream's `Checks` and `Copilot Setup Steps` workflows are disabled on the fork (GitHub → Actions; their emulator tests expect upstream's UI). Before a release, run `./gradlew checkstyle lint` plus the module tests locally, and upstream's XML formatter on layouts: `find . -wholename "*/res/layout/*.xml" | xargs java -jar android-xml-formatter.jar` (1.1.0, from github.com/ByteHamster/android-xml-formatter); it must leave no diff.
- Commit as `dcdude71 <dcdude71@users.noreply.github.com>` (set with `git config user.name/user.email` in this repo), never under a real name.

## Keep the diff against upstream small
The fork is rebased on upstream regularly, so:
- Put new code in new, fork-only classes and files. Only add small hooks to upstream files.
- Never bump the database version. Fork tables are created with `CREATE TABLE IF NOT EXISTS` from `PodDBAdapter`'s helper `onOpen` (`PlaylistDbAdapter.createTables`, `FeedSkipForwardDbAdapter.createTables`).
- `git diff upstream/develop...HEAD --stat` lists everything the fork touches.

## Castlist settings screen
Settings → Castlist features (`preferences_castlist.xml`, `CastlistPreferencesFragment`) holds the app-wide fork settings: player style, player background, tint controls, playlist end action, episode filter style. The entry is added from `ForkProjectSettings`, just above the Project category; `PreferenceActivity` and the settings search have one-line hooks. Upstream's settings XML files are untouched. Per-podcast skip forward stays in podcast settings and the official-app export in Backup.

## Rule-based playlists
Approved design (open to adjustment once used in practice):
- Playlists exist alongside the untouched queue. No master playlist, no per-podcast default playlist. Two kinds since v0.10.0: smart (rules, below) and manual (see "Manual playlists").
- Playlist = name + rules + auto-download + show in Android Auto.
- Rule = podcast, count (0 = all), pick from most recent / oldest, play order oldest / newest first, priority 1-10. Same priority: rule order (drag to reorder).
- "Most recent N" = the N newest episodes, only the unplayed ones among them (does not walk into the back catalog). "Oldest N" = the next N unplayed.
- The playlist is recalculated live after each episode. Playing from a playlist never adds to the queue; starting any other episode goes back to the normal queue.
- Playlist mode starts only from a toolbar play button (30dp icon, `ic_play_30dp`): "Play playlist" on a playlist, or "Play playlists from the top" on the Playlists tab (first playlist with episodes). Episode rows in a playlist have no play button (hidden via the hook `EpisodesListFragment.onEpisodeBound`); to play one episode, go to its podcast. Playing from the episode details page does not start playlist mode.
- At the end of a playlist: setting `prefPlaylistEndAction` (Settings → Castlist), "Continue with the next playlist" (default, follows Playlists tab order, skips empty ones, stops after the last) or "Stop". Never falls back to the queue. Read via `PlaylistPreferences`.
- Playlist auto-downloads follow the normal download settings, never enqueue, and playlist episodes are protected from download cleanup.
- Playlist downloads do not count toward the global episode limit (`AutomaticDownloadAlgorithm`, `EpisodeCleanupAlgorithm.getNumEpisodesToCleanup`). To keep them bounded, rules in an auto-download playlist need a count of 1-10 (`PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES`); the rule dialog disables "All" and the auto-download toggle refuses while an "All" rule exists. `PlaylistReader.limitForAutoDownload` caps legacy rules anyway. The same cap applies to cleanup protection in every playlist: only the first 10 episodes per rule are protected and left out of the limit, so a playlist with an "All" rule cannot keep unlimited downloads.
- Fixed (also an upstream bug): when downloaded episodes exceeded the episode limit, `episodeSpaceLeft` went negative and `subList` threw inside an executor, silently skipping all auto-downloads. Now clamped to 0.
- Upstream clears an episode's auto-download flag after its first successful download (`MediaDownloadedHandler`) or a cancelled download, so a deleted episode is never auto-downloaded again. Playlist auto-download ignores that flag (it is false for every episode downloaded once, including in the official app before an import). Instead, episodes the user deletes or cancels by hand are recorded in the fork table `PlaylistDismissedEpisodes` (`PlaylistWriter.dismissEpisode`, one-line hooks in `DeleteActionButton`, `DeleteSwipeAction`, `FeedItemMenuHandler`, `EpisodeMultiSelectActionHandler`, `CancelDownloadActionButton`) and never auto-downloaded by playlists. Cleanup and delete-after-playing are not recorded.
- All three cleanup algorithms protect playlist episodes (`APCleanupAlgorithm` only deletes played ones, which playlists never hold). Leaving the rule editor (`PlaylistEditFragment.onStop`) starts auto-download.
- Code: `model/.../playlist/`, `storage/database/.../Playlist*` (DbAdapter, Engine, Reader, Writer, Playback), `app/.../ui/screen/playlist/`. Tests in `storage/database/src/test/.../Playlist*Test.java`.

Phases:
1. Data model and engine: done
2. Screens and rule editor: done
3. Playback: done
4. Auto-download and cleanup protection: done
5. Android Auto: done, tested in a car on 2026-11-19 (Playlists folder, playlist playback). Sideloaded installs (GitHub, Obtainium, F-Droid) need Android Auto's developer setting "Unknown sources" to show the app. A "Playlists" folder appears after "Continue listening" when at least one playlist has "Show in Android Auto" ticked (playlist screen ⋮ menu; on by default for new playlists, off for existing ones). Starting an episode from it starts playlist playback. Code: `PlaylistMediaLibrary` plus small hooks in `MediaLibrarySessionCallback`.

- Podcast picker (Edit rules → +), since v0.10.5 after a Reddit request: multi-select; podcasts already in the playlist are hidden until "Show added (N)" (they stay selectable, a podcast can have several rules). Several picks open one `PlaylistRuleDialog` and each podcast gets a copy of the rule (`PlaylistRulesFragment.showPodcastPicker`).

## Manual playlists
Added in v0.10.0 (designed 2026-11-20, mockups: https://claude.ai/artifact/DjXtvtMi6k417dMAkU4HsU). The queue stays untouched; manual playlists only add to it.
- Data: `Playlists.manual` and `Playlists.keep_played` columns, fork table `PlaylistEpisodes (playlist, item, position)`. `PlaylistReader.getEpisodes` returns the hand-picked list for a manual playlist, so the widget, Android Auto and the player line work unchanged. Played episodes drop off unless "Keep played episodes" is on (they stay in the table, filtered on read).
- Visual cue everywhere: smart = sparkle on a filled tile, manual = list on an outlined tile (`PlaylistKindUi`, `ic_playlist_kind_*`, `bg_playlist_kind_*`). Toolbar and row subtitles start with the kind.
- "New playlist" asks for the kind and the name (`PlaylistKindDialog`). The kind never changes; "Copy into a manual playlist" freezes a smart playlist's current episodes into a new manual one (`PlaylistWriter.convertToManual`).
- Adding: "Add to playlist…" in the episode long-press menu, the episode screen menu and multi-select (hooks in `FeedItemMenuHandler` and `EpisodeMultiSelectActionHandler`, items in the upstream menu XML). `AddToPlaylistDialog` lists manual playlists only, with checkboxes (unchecking removes), plus "New manual playlist". New episodes go to the end; the queue's "Enqueue location" is ignored.
- Manual playlist screen (`PlaylistEpisodesFragment`, manual mode set up after the first load): drag handles, swipe either way removes with undo (`ManualPlaylistSwipeActions`, ignores the screen's swipe settings), Sort submenu, a play button on every row that starts playlist mode at that episode.
- Playback: the next episode is the one after the current in list order (`PlaylistPlayback.getNextManualEpisode`), skipping played ones unless kept.
- Auto-download: per-playlist toggle, downloads and protects the next 10 unplayed episodes (same cap as rules).
- Official-app export appends the unplayed manual-playlist episodes to the queue (playlists in tab order, no duplicates), then drops the fork tables.
- Not done: Android Auto and home screen shortcuts use the same icon for both kinds; no swipe action "Add to playlist" in other lists.

## Wear OS watch app
Upstream's `:app-wearos` (a remote control talking to the phone through the Wearable Data Layer, play flavor only) is shipped since v0.10.2.
- Identity: a block at the end of `app-wearos/build.gradle` sets `applicationId io.github.dcdude71.castlist`, the version from `app/castlist-version.properties` and the app name per build type (upstream sets "AntennaPod" in `common.gradle`). Castlist's launcher PNGs are copied into `app-wearos/src/main/res/mipmap-*`. Phone and watch must share package name and signing key.
- Playlists: `CastlistWearPlaylists` (wear-interface) defines `/castlist/playlists` (JSON list: id, name, kind, episodes, time left; "All playlists" first, id -1) and `/castlist/play_playlist/<id>`. Phone side: `CastlistWearHandler` (app/src/play), hooked at the top of upstream's `WearListenerService.handleMessage`. It must start playback with the application context: the listener service is short-lived, and a MediaController bound to it leaks and later crashes the app. Watch side: `PlaylistListActivity`/`PlaylistListViewModel` (+ `PlaylistRepository`), hooks in `MainListActivity` (one `item { PlaylistsMenuItem() }`) and `WearDataListenerService` (first line), activity declared in the watch manifest.
- Skip ad on the watch was removed in favour of per-podcast skip forward: the watch's fast-forward goes through `WearListenerService` → `seekForward()`, which uses the podcast's time.
- Testing: the user's Galaxy Watch Ultra (SM-L715U) is paired with the laptop's adb; its Wi-Fi debugging port changes, and Wi-Fi drops when the screen sleeps. Debug phone and debug watch apps pair with each other (same debug key). Samsung's `watchuniteplugin` crashes on its own (BLUETOOTH_PRIVILEGED); not ours.
- User guide: `docs/watch.md` (install over wireless debugging).

Inspired by Downcast's playlist engine.

## Testing on a device
- Debug build: `./gradlew --console=plain :app:installPlayDebug`, then `adb shell monkey -p io.github.dcdude71.castlist.debug -c android.intent.category.LAUNCHER 1` (AGENTS.md's launch command uses the upstream package name and does not work here).
- The debug app is separate from the release app. Import a database export to test with real podcasts. Downloaded files from the release app are not readable by the debug app, so reset `FeedMedia.downloaded`/`file_url` in the imported copy (`adb exec-out run-as io.github.dcdude71.castlist.debug cat databases/Antennapod.db`, edit with sqlite3, push back with `run-as ... sh -c 'cat > databases/Antennapod.db'` while the app is force-stopped).

## Skip forward per podcast
Replaced "Skip ad" (2026-11-22). Per-podcast "Skip forward time" in podcast settings (`FeedSkipForwardDialog`); 0/empty = global fast-forward. Stored in the fork table `FeedAdSkip` (name kept so old ad skip values carry over) via `FeedSkipForward` / `FeedSkipForwardDbAdapter`, cached in memory so playback code can read it on the main thread. `FeedSkipForward.getEffectiveSeconds(feedId|playable)` is used by `Media3PlaybackService.seekForward` (notification, headset, Android Auto, watch), the legacy `PlaybackService`, `AudioPlayerFragment` (button label and seek; long-press edits the podcast's time when set) and `VideoplayerActivity`. No extra buttons anywhere.

## Player background (modern style)
`PlayerBackground` (audio package): Settings → Castlist features → "Player background" (`prefPlayerBackground`: theme / gradient (default) / solid / blur) and "Tint controls with cover colour" (`prefPlayerTint`, default on). Loads the episode artwork with Glide (256 px), Palette picks colours; background views `playerBackgroundImage` + `playerBackground` at the bottom of `audioplayer_fragment_modern.xml`. Blur uses `RenderEffect` (Android 12+, gradient otherwise). Text/icons are re-coloured only for Solid (originals kept in tag `playerOriginalColor`); accent is the cover's vibrant colour, lightened/darkened until 2.6:1 against the background. Lookups are scoped to `playerContent` because the mini player reuses ids (`butPlay`). Status bar icons follow the background while the sheet is expanded. Episode title and podcast name are centred.

## Episode filter (advanced)
- Model: `AdvancedEpisodeFilter` = keep conditions (any/all) + skip conditions (any/all); `EpisodeCondition` = field (POST = title or description, TITLE, DESCRIPTION, LENGTH), operator, value, whole word. Matching ignores case and accents (`EpisodeCondition.normalize`). Unknown length never fails a keep condition nor triggers a skip one.
- Storage: `FeedEpisodeFilters` (one row per feed; `format` 1 = conditions, `match_all` = keep, `skip_match_all`) plus `FeedEpisodeFilterConditions`. Rows with `format` 0 are the older word-list filters and are converted on read by `AdvancedEpisodeFilter.fromLegacy`; saving rewrites them as conditions.
- Per-episode author and categories are not stored by upstream, so they are not filter fields (yet).

## Player style
Settings → Castlist features → Player style: Modern (default) or Classic (upstream layout). Modern aims for an Apple Podcasts-like look: small artwork, bold title, big controls. Fork-only files: `PlayerStyle.java`, `PlayerLayout.java`, `audioplayer_fragment_modern.xml`.
- Foldables (Modern only, `PlayerLayout`): window ≥ 600dp wide → shownotes in `descriptionPane` beside the controls (pager shows the cover only, Shownotes button hidden). Half-open with a horizontal fold (tabletop, via `androidx.window`) → cover above the fold, controls below; overrides the wide layout. MainActivity handles size changes itself (no recreate), so the layout is re-applied from `AudioPlayerFragment.onConfigurationChanged`.
- Upstream `NestedScrollableHost` was changed so it works outside a ViewPager2 and no longer cancels its first frame (cancelling froze the unfold animation: `BLASTSyncEngine` sync timeout).
- Testing needs the real phone: folded, unfolded, half-folded and flat again.

## Playlists on wide screens
- `PlaylistsFragment` (layout `playlist_panes.xml`): ≥ 600dp wide → list (360dp) + selected playlist (`PlaylistEpisodesFragment` as a child in `detailPane`). Selection is remembered in prefs (`PlaylistPanes`).
- Pencil opens `PlaylistEditFragment` (always, also when narrow): `PlaylistRulesFragment` on the left, `PlaylistEpisodesFragment.newPreviewInstance` on the right when wide. Preview tints newly added episodes for 2 s. Narrow: rules toolbar subtitle "Preview: N episodes".
- Fold with two panes → selected playlist full screen (Back = list). A full-screen playlist that becomes visible on a wide screen (unfold, or Back from the editor) pops itself so the two panes show it.
- All fork-only files; no upstream changes.

## Episode details pane (wide screens)
- `EpisodeDetailPane` (fork-only) wraps `main_content_view` at runtime in a horizontal LinearLayout with a right-hand `episodeDetailPane` container (ids in `values/ids_fork.xml`), so upstream layout files stay untouched. It mirrors the main view's bottom margin (mini player).
- Hooks in `MainActivity`: `install` after `setContentView`, `show` at the top of `loadChildFragment` (intercepts `ItemPagerFragment` when ≥ 600dp), `close` in `loadFragment` (tab switch), `onConfigurationChanged` (folding moves the episode to full screen).
- In the pane the toolbar's back arrow becomes ✕; Back closes the pane (its callback is re-registered on open so it wins over MainActivity's).
- Known edge: `ItemFragment` pops the activity back stack if the episode no longer exists; in the pane that also steps the list back once.

## Working preferences
- Ask design questions as a short numbered list with a recommendation per item, and let the answer come back as plain text (not a multiple-choice picker); answers often add nuance the options don't cover.
