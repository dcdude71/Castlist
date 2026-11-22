# FAQ

### What's the difference between a smart and a manual playlist?
A **smart** playlist (✦) picks its episodes from rules and keeps itself up to date. A **manual** playlist (≡) holds the episodes you add yourself, in your order, like an extra queue. The kind is chosen when you create the playlist; a smart playlist can be copied into a manual one. See [Playlists](playlists.md#manual-playlists).

### Can I have a playlist of all new episodes from a set of podcasts?
Yes. In Edit rules, tap **+**, tick all the podcasts at once and choose **All unplayed episodes**: each podcast gets the same rule with the same priority, and the play order you pick (**Newest first** or **Oldest first**). The playlist updates itself as episodes come in and as you finish them. See [Playlists](playlists.md#rules).

### If I set "3 oldest episodes" and listen to them, do the next 3 get added?
Yes. Rules are not a snapshot: "3 oldest unplayed" always means the 3 oldest episodes you have not played yet. Finish one, and the next one moves in.

### Does playing a playlist change my queue?
No. Playlists never add to the queue, and the queue never plays after a playlist. **Add to playlist…** and **Add to queue** are separate: adding an episode to a manual playlist leaves the queue alone. Starting an episode from the queue, the inbox or a podcast page switches back to the queue. See [Playing a playlist](playlists.md#playing-a-playlist).

### Why doesn't an episode show up in my playlist?
In a manual playlist, played episodes drop off unless **Keep played episodes** is on. In a smart playlist, check, in order:
- it is unplayed;
- it is within the rule's number of episodes ("3 most recent" ignores older ones);
- it passes the podcast's [episode filter](episode-filter.md);
- if the playlist has **Only downloaded episodes** on, it is downloaded.

### Can I filter on words in the description?
Yes, with the [advanced filter](episode-filter.md#advanced-filter): Settings → Castlist features → Episode filter style → Advanced, then in a podcast's Episode filter add a condition on **Description** (or **Post**, which looks in the title and the description).

### Will my playlists come back if I reinstall?
Only from a backup made in Castlist (**Database export**). See [Backups](backups.md).

### Can I go back to the official AntennaPod?
Yes. Use **Export for official AntennaPod** in Settings → Backup & restore, and import that file in the official app. Playlists, skip forward times and advanced filters stay behind; the unplayed episodes of manual playlists are added to the queue. See [Backups](backups.md#switching-back-to-the-official-antennapod).

### Is there a watch app?
Yes, for Wear OS watches, as a remote control for the phone: start a playlist, see what's playing, pause and skip. It comes with each GitHub release and is installed over Wi-Fi debugging. See [Watch](watch.md).

### Can I run Castlist next to the official app?
Yes. They are separate apps and do not share data. Castlist has its own name and icon so you can tell them apart.

### How do I get updates?
Through [Obtainium](https://github.com/ImranR98/Obtainium) with `https://github.com/dcdude71/Castlist` as the source, or by downloading the APK from the [Releases page](https://github.com/dcdude71/Castlist/releases).

### Where do I report a problem?
Here, in the [issues](https://github.com/dcdude71/Castlist/issues). Please don't report Castlist problems to the AntennaPod team. Castlist is a personal project, so replies may take a while.

### Why doesn't Castlist appear in Android Auto?
Android Auto only lists apps installed from the Play Store unless you allow others. Turn on **Unknown sources** in Android Auto's developer settings; the steps are in [Playlists → Android Auto](playlists.md#android-auto).

