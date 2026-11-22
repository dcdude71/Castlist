# Backups

All in Settings → **Backup & restore**.

## Regular backup

**Database export** is your regular backup. It includes everything: subscriptions, played episodes, the queue, and the Castlist additions (playlists with their rules or hand-picked episodes, skip forward times, advanced episode filters). Restore it in Castlist with **Database import**. **Automatic database export** makes the same backup every few days.

App-wide settings (like Player style or Episode filter style) are not part of the database backup, as in AntennaPod. Settings of each podcast are.

## Switching back to the official AntennaPod

**Export for official AntennaPod** makes the same backup without the fork's additions (playlists, playlist rules, skip forward times, advanced episode filters), so the official app can read it. Import it there with **Database import**. Your subscriptions, played episodes, queue and basic episode filters carry over. Unplayed episodes from your manual playlists are added to the end of the queue, so you don't lose them.

One caveat: Castlist follows AntennaPod's development version. If AntennaPod changes its database format there, Castlist can get ahead of the official release for a while, and the official app refuses to import a backup from a newer version until it is updated too.

## Moving from AntennaPod to Castlist

The two apps install side by side and do not share data. In AntennaPod, use **Database export**, then **Database import** in Castlist.
