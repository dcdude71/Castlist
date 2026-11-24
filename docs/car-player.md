# Car player

A full-screen player with big buttons, for cars without Android Auto or CarPlay. Put the phone in a portrait mount and you can see what's playing at a glance and control it with one tap.

## Opening it

- In the player, tap the **car** button at the top.
- Or add a home screen icon: Settings → Castlist features → Car player → **Add to home screen**.

- Or let it open by itself when your car's Bluetooth connects (see below).

Tap **✕** (or Back) to leave. The screen stays on while the car player is open; change that with **Keep screen on**.

## The screen

From top to bottom:

- What you're listening from: the playlist, the podcast or the queue, and how many episodes are left.
- The artwork, the podcast and the episode title.
- A progress bar and the time left in the episode.
- **Rewind** and **fast-forward**. Fast-forward uses the podcast's own [skip forward time](player.md) when it has one, so it doubles as an ad skip.
- **Play / pause**, the biggest button.
- **Next episode**. Pressed it by mistake? Tap **Undo** in the bar that shows for a few seconds to go back to where you were.
- **Pinned**, to start something else.

The play button, progress bar and time left take a colour from the artwork.

## Opening by itself in the car

Settings → Castlist features → Car player → **Open when connected to**: tick your car's Bluetooth (or several). Castlist asks for the **Nearby devices** permission the first time, so it can tell which device connected.

While one of those devices is connected:

- Opening Castlist (its icon, a shortcut, or tapping the player) takes you to the car player.
- A notification, **Connected to …**, opens the car player with one tap, handy when the phone was locked or showing another app. Turn it off with **Notification when connected**.
- Tap **✕** to use the normal player instead; Castlist won't switch back until the next time the car connects.

When the car disconnects, a car player that opened this way closes and the notification goes away. Android doesn't let apps jump in front of what you're doing on their own, so the car player waits until you open Castlist or tap the notification. Playback doesn't start by itself.

## Pinned playlists and podcasts

**Pinned** shows up to 8 big tiles. The switch at the top picks **Playlists** or **Podcasts**; the screen opens on the one you used last.

- A **playlist** tile starts the playlist, like its Play button. "All playlists" can be pinned too.
- A **podcast** tile plays that podcast: the episode you've started, otherwise the oldest unplayed one, then the next unplayed episodes in order. It stops after the last one. The podcast's [episode filter](episode-filter.md) applies.

The tile that's playing is highlighted.

### Choosing what's pinned

The first time, Castlist pins your first 8 playlists and the 8 podcasts you played most recently. To change them:

- Settings → Castlist features → Car player → **Pinned**: one tab for playlists and one for podcasts. Drag to reorder (the order on the car screen), tap ✕ to unpin, **Add** to pick one.
- Or in a playlist's or a podcast's ⋮ menu, tick **Pin to car player**.

Pins can't be changed from the car screen itself, so nothing needs reading or precise taps while driving.

## Settings

Settings → Castlist features → **Car player**:

- **Car player**: on by default. Off hides the car button and the menu entries.
- **Next button**: **Next episode** (default), **Next chapter** (then the next episode after the last chapter), or **Both** side by side.
- **Pinned**: the editor described above.
- **Keep screen on**: **Always** (default), **Only when charging** (follows the cable live: plug in and the screen stays on, unplug and it can turn off as usual), or **Never**.
- **Open when connected to**: the car Bluetooth devices described above. Off until you pick one.
- **Notification when connected**: on by default.
- **Add to home screen**: a shortcut that opens the car player.
