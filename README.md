# Castlist

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![GitHub Release](https://img.shields.io/github/v/release/dcdude71/Castlist)](https://github.com/dcdude71/Castlist/releases)

Castlist is a personal fork of [AntennaPod](https://github.com/AntennaPod/AntennaPod), the easy-to-use, flexible and open-source podcast manager for Android. I keep it close to upstream and add a few features I wanted for my own listening.

**This is not the official AntennaPod app**, and it is not affiliated with or endorsed by the AntennaPod team. If you are looking for AntennaPod, you will be much happier with the official version:

- Website: [antennapod.org](https://antennapod.org/)
- [Google Play](https://play.google.com/store/apps/details?id=de.danoeh.antennapod) · [F-Droid](https://f-droid.org/app/de.danoeh.antennapod) · [GitHub](https://github.com/AntennaPod/AntennaPod)

## What's different in this fork

- **[Smart and manual playlists](docs/playlists.md):** next to the regular queue, which stays as it is. *Smart* playlists are built from rules, one per podcast: how many episodes, the most recent or the oldest unplayed, play order and priority (pick several podcasts at once to give them the same rule). They update themselves as episodes come in and as you listen. *Manual* playlists work like extra queues: add episodes with **Add to playlist…**, drag to reorder, play from any episode. Both can download their episodes automatically and show up in Android Auto.
- **[Playlists on your home screen](docs/home-screen.md):** a 4×1 widget that starts one of four playlists with one tap, playlist icons, and playlists in the app's long-press shortcuts.
- **[A Wear OS watch app](docs/watch.md) (experimental, unsupported):** start any playlist from your wrist, see what's playing and control it, and browse the queue, downloads and subscriptions on the phone.
- **[Advanced episode filter](docs/episode-filter.md):** include and skip words together, search titles and descriptions, whole words, a length range, with a live preview that shows which episodes are kept or skipped, and why. Playlists respect the filter.
- **[A modern player and per-podcast skip forward](docs/player.md):** a redesigned full-screen player that takes its colours from the artwork (AntennaPod's original player is one setting away), and a fast-forward time per podcast, handy for skipping ads.
- **[Made for foldables and big screens](docs/foldables.md):** two-pane layouts for playlists and episode details, show notes beside the player, and tabletop mode.
- **[Backups that work both ways](docs/backups.md):** the regular backup includes everything Castlist adds, and a separate export lets you go back to the official app.
- **Installs next to the official app:** its own name, package name (`io.github.dcdude71.castlist`) and icon, so you can run both and tell them apart. The two apps do not share data.

**New here? Start with the [Castlist guide](docs/README.md)** (also in the app: Settings → Castlist guide) and the [FAQ](docs/faq.md).

## Staying close to AntennaPod

Castlist is built to follow AntennaPod, not to drift away from it.

**Easy to keep up to date with AntennaPod.** The fork is meant to be rebased onto new AntennaPod versions as they come out, so it is designed to make that painless:

- New features live in new files. AntennaPod's own code is touched only where a feature has to hook in, usually with a line or two, and is never reorganized, renamed or reformatted.
- Castlist's look (name, icon, splash screen) replaces AntennaPod's through the app's own settings and resources instead of editing them.
- Where Castlist changes how something looks or works, AntennaPod's way stays available as a setting: Player style → AntennaPod, and Episode filter style → Basic (the default).

**Your data stays compatible with the official app.**

- AntennaPod's database is never modified. Its tables and its version number stay exactly as in AntennaPod; everything Castlist adds (playlists, skip forward times, advanced filters) lives in separate tables of its own.
- A backup from the official AntennaPod imports into Castlist as is.
- **Export for official AntennaPod** gives you a backup the official app can import, without Castlist's additions. See [Backups](docs/backups.md).
- Updating Castlist never needs a fresh start: new features add what they need to your existing data automatically.

## Installing

Signed APKs are published on the [Releases page](https://github.com/dcdude71/Castlist/releases). The easiest way to install and stay up to date is [Obtainium](https://github.com/ImranR98/Obtainium): add `https://github.com/dcdude71/Castlist` as an app source. Each release has two files: `Castlist-<version>.apk` for the phone and `Castlist-Wear-<version>.apk` for the [watch app](docs/watch.md). In Obtainium, set **Filter APKs by regular expression** to `^Castlist-v` so it picks the phone app.

The release APK has been scanned by Google Play Protect, which found no issues. Because it does not come from the Play Store, Android asks you to allow installs from the app you install it with (Obtainium or your browser).

### Coming from AntennaPod Mix?
This app used to be called AntennaPod Mix. It was renamed to Castlist so it has its own identity, separate from the official AntennaPod. Because the app's package name changed too, Castlist installs as a new app next to AntennaPod Mix rather than updating it. To bring your data over:

1. In AntennaPod Mix: Settings → Backup & restore → **Database export**.
2. Install Castlist. In Obtainium, add `https://github.com/dcdude71/Castlist` as a new app.
3. In Castlist: Settings → Backup & restore → **Database import**, and choose the file from step 1.
4. Check everything is there, then uninstall AntennaPod Mix.

Your subscriptions, played episodes, queue, playlists, skip forward times and advanced filters all come along. App-wide settings, such as Player style, need to be set again.

## Please read: what to expect

This is mainly a personal project, maintained in my spare time and shaped by how I like to listen to podcasts. A few honest notes:

- Features may change, or disappear, as I experiment.
- Comments, issues and suggestions are very welcome here, but I may not get to them quickly, and some may never be addressed.
- Please don't report problems with this fork to the AntennaPod team. Their issue tracker is for the official app.
- The fork is provided as is, without any warranty. Use it at your own risk, and keep backups of your data.

If you need something dependable, the official AntennaPod is the way to go. Thank you for understanding!

## Privacy

Castlist collects no data and sends nothing to its developer. Like AntennaPod, it only connects to the podcast feeds you subscribe to and to services you choose to use (such as podcast search or synchronization). The fork's added features (playlists, widgets and shortcuts, Android Auto, the watch app, per-podcast skip forward, the advanced episode filter, the foldable layouts) work entirely on your devices. For the official app's policy, see [antennapod.org/privacy](https://antennapod.org/privacy/).

## Credits

All the hard work behind this app comes from the [AntennaPod](https://github.com/AntennaPod/AntennaPod) team, its contributors and translators. This fork only adds a few things on top. If you enjoy the app, consider supporting the original project at [antennapod.org](https://antennapod.org/).

## License

Like AntennaPod, this fork is licensed under the GNU General Public License (GPL-3.0). You can find the license text in the [LICENSE](LICENSE) file. The source code of every release is available in this repository.

## Building

You can build the app like any other Android project, for example with `./gradlew :app:assembleDebug`. See [CONTRIBUTING.md](CONTRIBUTING.md) for more details on the project structure (written for upstream AntennaPod).
