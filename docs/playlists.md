# Playlists

Next to the regular queue, you can create any number of playlists. There are two kinds:

- **Smart playlists** (✦ sparkle icon on a filled tile) are a set of **rules**, one per podcast, and the app works out the episodes for you. They update themselves as new episodes come in and as you listen.
- **Manual playlists** (≡ list icon on an outlined tile) work like the queue: you add episodes one by one and put them in order. You can have as many as you like, and the queue stays exactly as it is. See [Manual playlists](#manual-playlists).

The icon shows the kind everywhere a playlist appears: the Playlists tab, the top of a playlist, the widget and its settings.

![A playlist and its episodes](images/playlist.png)

## Creating a playlist

1. Open the **Playlists** tab and tap **+**. Choose **Smart playlist** and give it a name. (The kind can't be changed later.)
2. Open the playlist and tap the **pencil** (Edit rules).
3. Tap **Add podcast** (**+**), tick one or more podcasts, tap **Next** and set the rule (below). When you pick several podcasts, each one gets its own copy of that rule, which you can change afterwards. With 10 or more subscriptions, a search field at the top filters the list; your ticks stay while you search.
4. Add more podcasts the same way. Podcasts already in the playlist are left out of the list; tap **Show added** to see them too, for example to give a podcast a second rule. Drag a rule to reorder it, swipe it away to remove it.

The Playlists tab shows, for each playlist, how many podcasts and episodes it holds and how much listening time is left ("2 podcasts · 5 episodes · 3 h 10 min left"). Time left counts only what is still unheard in partly played episodes.

To start a similar playlist, open a playlist's menu (⋮) and choose **Duplicate**: the copy gets the same rules and options under a new name.

If a playlist is empty because the podcasts' [episode filters](episode-filter.md) hide all its episodes, it says how many are hidden, so you know where to look.

![Editing a playlist's rules](images/playlist-rules.png)

## Rules

Each rule says which episodes of one podcast go into the playlist:

| Setting | Choices | Notes |
|---|---|---|
| **Episodes** | All unplayed episodes, or a limit (for example 3) | |
| **Pick from** | Most recent, or Oldest unplayed | Only with a limit. "Most recent" looks at the newest episodes and keeps the unplayed ones among them. "Oldest unplayed" is for catching up on a series. |
| **Play order** | Oldest first, or Newest first | The order of this podcast's episodes inside the playlist |
| **Priority** | 1 to 10 | Priority 1 podcasts play first, then priority 2, and so on. Podcasts with the same priority follow the order of the rules. |

If a podcast has an [episode filter](episode-filter.md), the rule only picks episodes that pass it. "3 most recent" then means the 3 most recent episodes that pass the filter.

### Examples

- **Morning news:** the 2 most recent episodes of a news podcast (priority 1), then the most recent episode of a second news podcast (priority 1), then the next unplayed episode of a history series you are catching up on (priority 2).
- **Catching up on a series:** one rule, "4 oldest unplayed, oldest first". As you finish an episode, the next one in the series takes its place.
- **All new episodes from a set of podcasts:** one rule per podcast with "All unplayed episodes", same priority, newest first.

On a big screen the rules editor shows a live preview of the playlist next to the rules. On a phone, the rules screen shows how many episodes the playlist currently has.

## Playing a playlist

- In a playlist, the **play** button at the top plays it from the start.
- At the top of the Playlists tab, **play** starts with your first playlist that has episodes.

Smart playlists always play from the top. To hear one particular episode, open its podcast and play it from there; that uses the normal queue. Manual playlists can also start from any episode (see below).

While a playlist plays, the player shows its name and the time left under the podcast name, for example *Morning · 1 h 20 min left*.

When an episode ends, the next one comes from the playlist, never from your queue, and playing from a playlist never adds episodes to the queue. Starting an episode from anywhere else (the queue, the inbox, a podcast page) switches back to the normal queue.

## When a playlist runs out

The app moves on to the next playlist, in the order of your Playlists tab, skipping empty ones. You can change this in Settings → Castlist features → **At the end of a playlist**:

- **Continue with the next playlist** (default)
- **Stop**

A short message says "Now playing: *next playlist*" when the app moves on, or "Playlist finished" when it stops. Either way, playback never falls back to the queue. Reorder your playlists by dragging them in the Playlists tab.

## Automatic downloads

In a playlist's menu (⋮), turn on **Auto-download episodes** to keep its episodes downloaded. Downloads follow your usual settings (Wi-Fi only, battery, and so on). These downloads:

- are not added to the queue,
- are kept when the app cleans up downloads to make room,
- do not count toward the episode limit in Settings → Downloads → Automatic download. That limit is left entirely to your podcasts' own automatic downloads.

A playlist episode is downloaded even if it was downloaded and removed before, for example by the official app before you moved to Castlist. The exception is an episode whose download you deleted or cancelled yourself in Castlist: it stays as it is. Download it again by hand if you change your mind. Downloads also start right after you edit a playlist's rules, not only at the next refresh.

To keep a playlist from filling your storage, every rule in a playlist with auto-download needs a number of episodes, up to 10 per podcast. "All unplayed episodes" is not available there. If a playlist already uses it, the app asks you to edit its rules first.

## Only downloaded episodes

In a playlist's menu (⋮), **Only downloaded episodes** makes the rules pick only from episodes that are already downloaded. This is handy for:

- listening offline or on mobile data: the playlist only contains what is on the phone;
- combining with a podcast's [episode filter](episode-filter.md), which decides what gets downloaded, so the playlist holds exactly the episodes you want.

A playlist with both **Auto-download episodes** and **Only downloaded episodes** still downloads its episodes, then shows them once they are downloaded.

## Android Auto

In a playlist's menu (⋮), **Show in Android Auto** adds it to a **Playlists** folder in Android Auto, right after "Continue listening". It is on for new playlists. Starting an episode from that folder plays the playlist, just like on the phone.

**Castlist doesn't show up in Android Auto?** Android Auto hides apps that were not installed from the Play Store, which includes Castlist from GitHub, Obtainium or F-Droid. To allow it, on the phone open Settings → Connected devices → Connection preferences → Android Auto, scroll down and tap **Version** about 10 times to turn on developer mode, then in the menu (⋮) open **Developer settings** and turn on **Unknown sources**. Reconnect to the car and Castlist appears with the other media apps.

## Manual playlists

A manual playlist holds the episodes you put in it, in the order you choose. Use it for a road trip, a "saved for later" list, or episodes to share with someone.

**Creating one:** tap **+** in the Playlists tab and choose **Manual playlist**, or create it on the spot when adding an episode (below).

**Adding episodes:** next to **Add to queue**, episodes now have **Add to playlist…**:

- in an episode's long-press menu,
- in the menu (⋮) of the episode screen,
- with several episodes selected (multi-select).

A short list of your manual playlists opens, with a check mark on the ones that already hold the episode. Tick or untick playlists, then tap **Done**, or tap **New playlist** to create one with the episode in it. Smart playlists are not in that list: their rules choose their episodes. New episodes go at the end of the playlist. The same episode can be in several playlists.

**In the playlist:**

- Drag an episode by its handle to move it, or use the menu (⋮) → **Sort** (date or duration).
- Swipe an episode either way to remove it from the playlist. Tap **Undo** to bring it back.
- Each episode has a **play** button: the playlist plays from that episode on, down the list. The play button at the top starts from the first episode.

**Played episodes** drop off the playlist, like in the queue. To keep them instead, turn on **Keep played episodes** in the playlist's menu (⋮); the playlist then plays them again in order, like a music playlist.

**Automatic downloads** work as for smart playlists: turn on **Auto-download episodes** in the menu (⋮) and the next 10 unplayed episodes stay downloaded and are kept when the app cleans up.

**From smart to manual:** in a smart playlist's menu (⋮), **Copy into a manual playlist** creates a manual playlist with the episodes the smart one holds right now. The smart playlist stays as it is. There is no way back from manual to smart.

When you [export for the official app](backups.md#switching-back-to-the-official-antennapod), manual playlists don't exist there, so their unplayed episodes are added to the end of the queue in the exported file.

## On your home screen

Playlists can also be started from a widget, a home screen icon or the app's shortcuts. See [Home screen](home-screen.md).
