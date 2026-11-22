# Watch

Castlist has a companion app for Wear OS watches (Galaxy Watch, Pixel Watch and others). It is a **remote control**: the phone does the playing, the watch starts and controls it.

## What it does

- **Now playing** at the top: the episode playing on the phone. Tap it for play/pause and skip back and forward. Skip forward uses the podcast's own [skip forward time](player.md) when it has one.
- **Playlists**: your playlists, with the same icons as on the phone (✦ smart, ≡ manual), how many episodes each holds and the time left. Tap one to start it on the phone, from the top. **All playlists** starts your playlists from the top, like the ▶ button in the Playlists tab.
- **Queue**, **Downloads**, **Episodes** and **Subscriptions**: browse the phone's lists and start an episode from the watch.

The watch needs to be connected to the phone (Bluetooth or Wi-Fi, as for notifications). Episodes are not copied to the watch.

## Which Castlist works with the watch

The watch app talks to the phone through Google Play Services, so it works with **Castlist from GitHub (or Obtainium)**, version 0.10.2 or later. A Castlist built without Google Play Services, such as an F-Droid build, doesn't answer the watch.

Even without the watch app, any Wear OS watch shows the phone's media controls (play, pause, skip) while Castlist plays.

## Installing the watch app

Each GitHub release has two files: `Castlist-<version>.apk` for the phone and `Castlist-Wear-<version>.apk` for the watch. Watches can't install an APK by tapping it, so the watch app is installed over Wi-Fi debugging, once per release:

1. **Turn on developer mode on the watch.** On a Galaxy Watch: Settings → About watch → Software → tap **Software version** about 5 times. (Pixel Watch: Settings → System → About → tap **Build number** 7 times.)
2. In **Settings → Developer options**, turn on **ADB debugging** and **Wireless debugging**. The watch and the phone (or computer) must be on the same Wi-Fi network.
3. Install `Castlist-Wear-<version>.apk` with one of these:
   - **From the phone:** a watch installer app, for example *Wear Installer* or *Bugjaeger*. Download the watch APK on the phone, open it in the installer, and follow its steps to pair with the watch (Wireless debugging → **Pair new device** shows a code).
   - **From a computer** with the Android platform tools: on the watch, Wireless debugging → **Pair new device**, then run `adb pair <ip>:<pairing port>` with the code shown, `adb connect <ip>:<port>` with the address shown on the Wireless debugging screen, and `adb install Castlist-Wear-<version>.apk`.
4. Turn **Wireless debugging** off again when done; it uses battery.

To update, install the new `Castlist-Wear-<version>.apk` the same way. Your settings on the watch are kept.

**Keep the phone and watch apps on the same version.** Both must come from the same place (the GitHub release), or the watch won't find the phone app.

## Tips

- If the watch says it can't reach the phone, open Castlist on the phone once, then try again.
- Watches turn Wi-Fi off when the screen sleeps or when they are close to the phone. While installing, stay on the Wireless debugging screen, or set Settings → Connections → Wi-Fi to **Always on**.
