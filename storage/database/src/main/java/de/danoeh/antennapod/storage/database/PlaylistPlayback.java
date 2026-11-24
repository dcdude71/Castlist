package de.danoeh.antennapod.storage.database;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.greenrobot.eventbus.EventBus;

import de.danoeh.antennapod.event.PlaylistTransitionEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.net.download.serviceinterface.AutoDownloadManager;
import de.danoeh.antennapod.storage.preferences.PlaylistPreferences;

public final class PlaylistPlayback {
    public static final long NO_PLAYLIST = 0;
    private static final String PREF_NAME = "PlaylistPlayback";
    private static final String PREF_ACTIVE_PLAYLIST = "activePlaylist";
    private static final String PREF_ACTIVE_PODCAST = "activePodcast";

    private static final Set<Long> skippedItemIds = Collections.synchronizedSet(new HashSet<>());

    private PlaylistPlayback() {
    }

    public static void start(@NonNull Context context, long playlistId) {
        skippedItemIds.clear();
        getPrefs(context).edit().putLong(PREF_ACTIVE_PLAYLIST, playlistId)
                .putLong(PREF_ACTIVE_PODCAST, NO_PLAYLIST).apply();
    }

    public static void startPodcast(@NonNull Context context, long feedId) {
        skippedItemIds.clear();
        getPrefs(context).edit().putLong(PREF_ACTIVE_PLAYLIST, NO_PLAYLIST)
                .putLong(PREF_ACTIVE_PODCAST, feedId).apply();
    }

    public static void stop(@NonNull Context context) {
        skippedItemIds.clear();
        getPrefs(context).edit().putLong(PREF_ACTIVE_PLAYLIST, NO_PLAYLIST)
                .putLong(PREF_ACTIVE_PODCAST, NO_PLAYLIST).apply();
    }

    public static long getActivePodcast(@NonNull Context context) {
        return getPrefs(context).getLong(PREF_ACTIVE_PODCAST, NO_PLAYLIST);
    }

    public static boolean isActive(@NonNull Context context) {
        return getActivePlaylist(context) != NO_PLAYLIST || getActivePodcast(context) != NO_PLAYLIST;
    }

    public static void unskip(long itemId) {
        skippedItemIds.remove(itemId);
    }

    public static long getActivePlaylist(@NonNull Context context) {
        return getPrefs(context).getLong(PREF_ACTIVE_PLAYLIST, NO_PLAYLIST);
    }

    public static boolean isPlayingFromPlaylist(@NonNull Context context, @NonNull FeedItem item) {
        long podcastId = getActivePodcast(context);
        if (podcastId != NO_PLAYLIST) {
            if (item.getFeedId() == podcastId) {
                return true;
            }
            stop(context);
            return false;
        }
        long playlistId = getActivePlaylist(context);
        if (playlistId == NO_PLAYLIST) {
            return false;
        }
        Playlist playlist = PlaylistReader.getPlaylist(playlistId);
        if (playlist != null && playlist.isManual()) {
            if (PlaylistReader.getManualEpisodeIds(playlistId).contains(item.getId())) {
                return true;
            }
            stop(context);
            return false;
        }
        for (FeedItem episode : PlaylistReader.getEpisodes(playlistId)) {
            if (episode.getId() == item.getId()) {
                return true;
            }
        }
        stop(context);
        return false;
    }

    @Nullable
    public static FeedItem getNextItem(@NonNull Context context, @NonNull FeedItem current, boolean skipped) {
        long podcastId = getActivePodcast(context);
        if (podcastId != NO_PLAYLIST) {
            return getNextPodcastItem(context, podcastId, current, skipped);
        }
        long playlistId = getActivePlaylist(context);
        if (playlistId == NO_PLAYLIST) {
            return DBReader.getNextInQueue(current);
        }
        if (skipped) {
            skippedItemIds.add(current.getId());
        }
        Set<Long> skippedIds;
        synchronized (skippedItemIds) {
            skippedIds = new HashSet<>(skippedItemIds);
        }
        Playlist playlist = PlaylistReader.getPlaylist(playlistId);
        FeedItem next = playlist != null && playlist.isManual()
                ? getNextManualEpisode(PlaylistReader.getManualEpisodes(playlistId, true),
                        current.getId(), skippedIds, playlist.isKeepPlayed())
                : getNextEpisode(PlaylistReader.getEpisodes(playlistId, skippedIds), current.getId(), skippedIds);
        if (next == null && PlaylistPreferences.shouldContinueWithNextPlaylist(context)) {
            next = startNextPlaylist(context, playlistId, current.getId());
            if (next != null) {
                Playlist nextPlaylist = PlaylistReader.getPlaylist(getActivePlaylist(context));
                EventBus.getDefault().post(new PlaylistTransitionEvent(
                        nextPlaylist != null ? nextPlaylist.getName() : ""));
            }
        }
        if (next == null) {
            stop(context);
            EventBus.getDefault().post(new PlaylistTransitionEvent(null));
        }
        AutoDownloadManager.getInstance().autodownloadUndownloadedItems(context);
        return next;
    }

    @Nullable
    private static FeedItem getNextPodcastItem(@NonNull Context context, long podcastId,
                                               @NonNull FeedItem current, boolean skipped) {
        if (skipped) {
            skippedItemIds.add(current.getId());
        }
        Set<Long> skippedIds;
        synchronized (skippedItemIds) {
            skippedIds = new HashSet<>(skippedItemIds);
        }
        FeedItem next = getNextEpisode(PlaylistReader.getPodcastEpisodes(podcastId, skippedIds),
                current.getId(), skippedIds);
        if (next == null) {
            stop(context);
        }
        return next;
    }

    @Nullable
    private static FeedItem startNextPlaylist(@NonNull Context context, long playlistId, long currentId) {
        List<Long> playlistIds = new ArrayList<>();
        for (Playlist playlist : PlaylistReader.getPlaylists()) {
            playlistIds.add(playlist.getId());
        }
        for (long nextPlaylistId : getFollowingPlaylistIds(playlistIds, playlistId)) {
            FeedItem first = getNextEpisode(PlaylistReader.getEpisodes(nextPlaylistId), currentId,
                    Collections.emptySet());
            if (first != null) {
                start(context, nextPlaylistId);
                return first;
            }
        }
        return null;
    }

    @NonNull
    static List<Long> getFollowingPlaylistIds(@NonNull List<Long> playlistIds, long playlistId) {
        int index = playlistIds.indexOf(playlistId);
        if (index < 0) {
            return new ArrayList<>();
        }
        return new ArrayList<>(playlistIds.subList(index + 1, playlistIds.size()));
    }

    @Nullable
    static FeedItem getNextEpisode(@NonNull List<FeedItem> episodes, long currentId, @NonNull Set<Long> skippedIds) {
        for (FeedItem episode : episodes) {
            if (episode.getId() != currentId && !skippedIds.contains(episode.getId())) {
                return episode;
            }
        }
        return null;
    }

    @Nullable
    static FeedItem getNextManualEpisode(@NonNull List<FeedItem> episodes, long currentId,
                                         @NonNull Set<Long> skippedIds, boolean includePlayed) {
        int start = 0;
        for (int i = 0; i < episodes.size(); i++) {
            if (episodes.get(i).getId() == currentId) {
                start = i + 1;
                break;
            }
        }
        for (int i = start; i < episodes.size(); i++) {
            FeedItem episode = episodes.get(i);
            if (episode.getId() != currentId && !skippedIds.contains(episode.getId())
                    && (includePlayed || !episode.isPlayed())) {
                return episode;
            }
        }
        return null;
    }

    private static SharedPreferences getPrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
