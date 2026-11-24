package de.danoeh.antennapod.storage.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.model.playlist.PlaylistRule;
import de.danoeh.antennapod.storage.database.mapper.FeedItemCursor;

public final class PlaylistReader {
    private static final int MIN_PAGE_SIZE = 50;
    private static final int MAX_SCANNED_EPISODES = 2000;
    private static final int ID_CHUNK_SIZE = 500;


    private PlaylistReader() {
    }

    @NonNull
    public static synchronized List<Playlist> getPlaylists() {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.getPlaylistDbAdapter().getPlaylists();
        } finally {
            adapter.close();
        }
    }

    @Nullable
    public static synchronized Playlist getPlaylist(long playlistId) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.getPlaylistDbAdapter().getPlaylist(playlistId);
        } finally {
            adapter.close();
        }
    }

    @NonNull
    public static synchronized List<PlaylistRule> getRules(long playlistId) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.getPlaylistDbAdapter().getRules(playlistId);
        } finally {
            adapter.close();
        }
    }

    @NonNull
    public static List<FeedItem> getEpisodes(long playlistId) {
        return getEpisodes(playlistId, Collections.emptySet());
    }

    @NonNull
    public static List<FeedItem> getAutoDownloadEpisodes() {
        List<FeedItem> episodes = new ArrayList<>();
        Set<Long> ids = new HashSet<>();
        Map<Long, Feed> feeds = loadFeeds();
        for (Playlist playlist : getPlaylists()) {
            if (playlist.isAutoDownload()) {
                for (FeedItem item : getProtectedEpisodes(playlist, feeds)) {
                    if (ids.add(item.getId())) {
                        episodes.add(item);
                    }
                }
            }
        }
        return episodes;
    }

    public static long getTimeLeftMillis(@NonNull List<FeedItem> episodes) {
        long total = 0;
        for (FeedItem item : episodes) {
            if (item.getMedia() != null) {
                total += Math.max(0, item.getMedia().getDuration() - item.getMedia().getPosition());
            }
        }
        return total;
    }

    @NonNull
    public static synchronized Set<Long> getDismissedEpisodeIds() {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.getPlaylistDbAdapter().getDismissedEpisodes();
        } finally {
            adapter.close();
        }
    }

    @NonNull
    static List<PlaylistRule> limitForAutoDownload(@NonNull List<PlaylistRule> rules) {
        List<PlaylistRule> limited = new ArrayList<>();
        for (PlaylistRule rule : rules) {
            int count = rule.getEpisodeCount();
            if (count == PlaylistRule.ALL_EPISODES || count > PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES) {
                count = PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES;
            }
            limited.add(new PlaylistRule(rule.getId(), rule.getPlaylistId(), rule.getFeedId(), count,
                    rule.getPickFrom(), rule.getPlayOrder(), rule.getPriority(), rule.getPosition()));
        }
        return limited;
    }

    public static int getDownloadedEpisodeCountInPlaylists() {
        int count = 0;
        for (FeedItem item : getProtectedEpisodes().values()) {
            if (item.getMedia() != null && item.getMedia().isDownloaded()) {
                count++;
            }
        }
        return count;
    }

    @NonNull
    public static Set<Long> getEpisodeIdsInPlaylists() {
        return getProtectedEpisodes().keySet();
    }

    @NonNull
    private static Map<Long, FeedItem> getProtectedEpisodes() {
        Map<Long, FeedItem> episodes = new HashMap<>();
        Map<Long, Feed> feeds = loadFeeds();
        for (Playlist playlist : getPlaylists()) {
            for (FeedItem item : getProtectedEpisodes(playlist, feeds)) {
                episodes.put(item.getId(), item);
            }
        }
        return episodes;
    }

    @NonNull
    private static List<FeedItem> getProtectedEpisodes(@NonNull Playlist playlist, @NonNull Map<Long, Feed> feeds) {
        if (playlist.isManual()) {
            List<FeedItem> unplayed = getManualEpisodes(playlist.getId(), false);
            return new ArrayList<>(unplayed.subList(0,
                    Math.min(unplayed.size(), PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES)));
        }
        return getEpisodes(limitForAutoDownload(getRules(playlist.getId())), Collections.emptySet(), feeds, false);
    }

    @NonNull
    public static synchronized List<Long> getManualEpisodeIds(long playlistId) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.getPlaylistDbAdapter().getEpisodeIds(playlistId);
        } finally {
            adapter.close();
        }
    }

    @NonNull
    public static synchronized Set<Long> getPlaylistIdsContaining(long itemId) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            return adapter.getPlaylistDbAdapter().getPlaylistIdsContaining(itemId);
        } finally {
            adapter.close();
        }
    }

    @NonNull
    public static List<FeedItem> getManualEpisodes(long playlistId, boolean includePlayed) {
        List<Long> ids = getManualEpisodeIds(playlistId);
        Map<Long, FeedItem> loaded = new HashMap<>();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            for (int start = 0; start < ids.size(); start += ID_CHUNK_SIZE) {
                List<Long> chunk = ids.subList(start, Math.min(ids.size(), start + ID_CHUNK_SIZE));
                String[] args = new String[chunk.size()];
                for (int i = 0; i < chunk.size(); i++) {
                    args[i] = String.valueOf(chunk.get(i));
                }
                try (FeedItemCursor cursor = new FeedItemCursor(adapter.getFeedItemCursor(args))) {
                    while (cursor.moveToNext()) {
                        FeedItem item = cursor.getFeedItem();
                        loaded.put(item.getId(), item);
                    }
                }
            }
        } finally {
            adapter.close();
        }
        List<FeedItem> result = new ArrayList<>();
        for (long id : ids) {
            FeedItem item = loaded.get(id);
            if (item != null && (includePlayed || !item.isPlayed())) {
                result.add(item);
            }
        }
        DBReader.loadFeedDataOfFeedItemList(result);
        return result;
    }

    @NonNull
    public static List<FeedItem> getEpisodes(long playlistId, @NonNull final Set<Long> excludedOldestIds) {
        Playlist manualPlaylist = getPlaylist(playlistId);
        if (manualPlaylist != null && manualPlaylist.isManual()) {
            return getManualEpisodes(playlistId, manualPlaylist.isKeepPlayed());
        }
        List<PlaylistRule> rules = getRules(playlistId);
        if (rules.isEmpty()) {
            return new ArrayList<>();
        }
        Playlist playlist = getPlaylist(playlistId);
        boolean downloadedOnly = playlist != null && playlist.isDownloadedOnly();
        return getEpisodes(rules, excludedOldestIds, loadFeeds(), downloadedOnly);
    }

    @NonNull
    public static List<FeedItem> getPodcastEpisodes(long feedId, @NonNull Set<Long> excludedIds) {
        PlaylistRule rule = new PlaylistRule(0, 0, feedId, PlaylistRule.ALL_EPISODES, PlaylistRule.PickFrom.OLDEST,
                PlaylistRule.PlayOrder.OLDEST_FIRST, PlaylistRule.MIN_PRIORITY, 0);
        return getEpisodes(Collections.singletonList(rule), excludedIds, loadFeeds(), false);
    }

    public static int countEpisodesHiddenByFilters(long playlistId) {
        List<PlaylistRule> rules = getRules(playlistId);
        if (rules.isEmpty()) {
            return 0;
        }
        Playlist playlist = getPlaylist(playlistId);
        boolean downloadedOnly = playlist != null && playlist.isDownloadedOnly();
        Map<Long, Feed> feeds = loadFeeds();
        int unfiltered = getEpisodes(rules, Collections.emptySet(), feeds, downloadedOnly, false).size();
        return Math.max(0, unfiltered - getEpisodes(rules, Collections.emptySet(), feeds, downloadedOnly).size());
    }

    @NonNull
    private static Map<Long, Feed> loadFeeds() {
        Map<Long, Feed> feeds = new HashMap<>();
        for (Feed feed : DBReader.getFeedList()) {
            if (feed.getState() == Feed.STATE_SUBSCRIBED) {
                feeds.put(feed.getId(), feed);
            }
        }
        return feeds;
    }

    @NonNull
    private static List<FeedItem> getEpisodes(@NonNull List<PlaylistRule> rules,
                                              @NonNull final Set<Long> excludedOldestIds,
                                              @NonNull final Map<Long, Feed> feeds,
                                              final boolean downloadedOnly) {
        return getEpisodes(rules, excludedOldestIds, feeds, downloadedOnly, true);
    }

    @NonNull
    private static List<FeedItem> getEpisodes(@NonNull List<PlaylistRule> rules,
                                              @NonNull final Set<Long> excludedOldestIds,
                                              @NonNull final Map<Long, Feed> feeds,
                                              final boolean downloadedOnly, final boolean applyFilters) {
        if (rules.isEmpty()) {
            return new ArrayList<>();
        }
        return PlaylistEngine.computeEpisodes(rules, new PlaylistEngine.EpisodeSource() {
            @NonNull
            @Override
            public List<FeedItem> getNewest(long feedId, int limit) {
                Feed feed = feeds.get(feedId);
                if (feed == null) {
                    return new ArrayList<>();
                }
                return collect(feed, itemFilter(downloadedOnly, FeedItemFilter.HAS_MEDIA),
                        SortOrder.DATE_NEW_OLD, limit, Collections.emptySet(), applyFilters);
            }

            @NonNull
            @Override
            public List<FeedItem> getOldestUnplayed(long feedId, int limit) {
                Feed feed = feeds.get(feedId);
                if (feed == null) {
                    return new ArrayList<>();
                }
                return collect(feed, itemFilter(downloadedOnly, FeedItemFilter.HAS_MEDIA, FeedItemFilter.UNPLAYED),
                        SortOrder.DATE_OLD_NEW, limit, excludedOldestIds, applyFilters);
            }
        });
    }

    @NonNull
    private static FeedItemFilter itemFilter(boolean downloadedOnly, String... properties) {
        FeedItemFilter filter = new FeedItemFilter(properties);
        return downloadedOnly ? new FeedItemFilter(filter, FeedItemFilter.DOWNLOADED) : filter;
    }

    @NonNull
    private static List<FeedItem> collect(@NonNull Feed feed, @NonNull FeedItemFilter itemFilter,
                                          @NonNull SortOrder order, int limit, @NonNull Set<Long> skippedIds,
                                          boolean applyFilters) {
        List<FeedItem> result = new ArrayList<>();
        int pageSize = limit == Integer.MAX_VALUE ? Integer.MAX_VALUE
                : Math.max(MIN_PAGE_SIZE, limit + skippedIds.size());
        int offset = 0;
        while (result.size() < limit) {
            List<FeedItem> page = DBReader.getFeedItemList(feed, itemFilter, order, offset, pageSize);
            for (FeedItem item : applyFilters ? EpisodeFilters.filter(feed, page) : page) {
                if (result.size() < limit && !skippedIds.contains(item.getId())) {
                    result.add(item);
                }
            }
            if (page.size() < pageSize || pageSize == Integer.MAX_VALUE || offset >= MAX_SCANNED_EPISODES) {
                break;
            }
            offset += pageSize;
        }
        return result;
    }
}
