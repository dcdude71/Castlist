package de.danoeh.antennapod.storage.database;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.danoeh.antennapod.model.feed.AdvancedEpisodeFilter;
import de.danoeh.antennapod.model.feed.EpisodeCondition;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.model.playlist.PlaylistRule;
import de.danoeh.antennapod.storage.preferences.EpisodeFilterPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PlaylistDatabaseTest {

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        UserPreferences.init(context);
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.getPlaylistDbAdapter().deleteAll();
        adapter.close();
    }

    @After
    public void tearDown() {
        setFilterStyle(EpisodeFilterPreferences.STYLE_BASIC);
        PodDBAdapter.tearDownTests();
    }

    @Test
    public void testCreateUpdateDeletePlaylist() throws Exception {
        Playlist playlist = new Playlist("Morning");
        long id = PlaylistWriter.createPlaylist(playlist).get();
        assertTrue(id > 0);

        Playlist stored = PlaylistReader.getPlaylist(id);
        assertEquals("Morning", stored.getName());
        assertFalse(stored.isAutoDownload());
        assertTrue(stored.isShowInAndroidAuto());

        stored.setName("Commute");
        stored.setAutoDownload(true);
        stored.setShowInAndroidAuto(false);
        PlaylistWriter.updatePlaylist(stored).get();
        Playlist updated = PlaylistReader.getPlaylist(id);
        assertEquals("Commute", updated.getName());
        assertTrue(updated.isAutoDownload());
        assertFalse(updated.isShowInAndroidAuto());

        PlaylistWriter.addRule(new PlaylistRule(id, 42)).get();
        PlaylistWriter.deletePlaylist(id).get();
        assertNull(PlaylistReader.getPlaylist(id));
        assertTrue(PlaylistReader.getRules(id).isEmpty());
    }

    @Test
    public void testPlaylistOrder() throws Exception {
        PlaylistWriter.createPlaylist(new Playlist("A")).get();
        PlaylistWriter.createPlaylist(new Playlist("B")).get();
        PlaylistWriter.createPlaylist(new Playlist("C")).get();
        List<Playlist> playlists = PlaylistReader.getPlaylists();
        assertEquals(List.of("A", "B", "C"), names(playlists));

        List<Playlist> reordered = new ArrayList<>();
        reordered.add(playlists.get(2));
        reordered.add(playlists.get(0));
        reordered.add(playlists.get(1));
        PlaylistWriter.setPlaylistOrder(reordered).get();
        assertEquals(List.of("C", "A", "B"), names(PlaylistReader.getPlaylists()));
    }

    @Test
    public void testRules() throws Exception {
        long playlistId = PlaylistWriter.createPlaylist(new Playlist("P")).get();
        PlaylistRule first = new PlaylistRule(playlistId, 10);
        first.setEpisodeCount(4);
        first.setPickFrom(PlaylistRule.PickFrom.OLDEST);
        first.setPlayOrder(PlaylistRule.PlayOrder.NEWEST_FIRST);
        first.setPriority(3);
        PlaylistWriter.addRule(first).get();
        PlaylistWriter.addRule(new PlaylistRule(playlistId, 20)).get();

        List<PlaylistRule> rules = PlaylistReader.getRules(playlistId);
        assertEquals(2, rules.size());
        PlaylistRule stored = rules.get(0);
        assertEquals(10, stored.getFeedId());
        assertEquals(4, stored.getEpisodeCount());
        assertEquals(PlaylistRule.PickFrom.OLDEST, stored.getPickFrom());
        assertEquals(PlaylistRule.PlayOrder.NEWEST_FIRST, stored.getPlayOrder());
        assertEquals(3, stored.getPriority());

        List<PlaylistRule> reordered = new ArrayList<>();
        reordered.add(rules.get(1));
        reordered.add(rules.get(0));
        PlaylistWriter.setRuleOrder(playlistId, reordered).get();
        assertEquals(20, PlaylistReader.getRules(playlistId).get(0).getFeedId());

        PlaylistWriter.deleteRule(rules.get(1)).get();
        assertEquals(1, PlaylistReader.getRules(playlistId).size());
    }

    @Test
    public void testRemovingFeedRemovesItsRules() throws Exception {
        Feed feed = createFeed(0);
        long playlistId = PlaylistWriter.createPlaylist(new Playlist("P")).get();
        PlaylistWriter.addRule(new PlaylistRule(playlistId, feed.getId())).get();
        PlaylistWriter.addRule(new PlaylistRule(playlistId, feed.getId() + 1000)).get();

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.removeFeed(feed);
        adapter.close();

        List<PlaylistRule> rules = PlaylistReader.getRules(playlistId);
        assertEquals(1, rules.size());
        assertEquals(feed.getId() + 1000, rules.get(0).getFeedId());
    }

    @Test
    public void testGetEpisodes() throws Exception {
        Feed news = createFeed(5);
        Feed series = createFeed(5);
        markPlayed(series.getItems().get(0));

        long playlistId = PlaylistWriter.createPlaylist(new Playlist("P")).get();
        PlaylistRule seriesRule = new PlaylistRule(playlistId, series.getId());
        seriesRule.setEpisodeCount(2);
        seriesRule.setPickFrom(PlaylistRule.PickFrom.OLDEST);
        seriesRule.setPriority(2);
        PlaylistWriter.addRule(seriesRule).get();
        PlaylistRule newsRule = new PlaylistRule(playlistId, news.getId());
        newsRule.setEpisodeCount(1);
        newsRule.setPriority(1);
        PlaylistWriter.addRule(newsRule).get();

        List<FeedItem> episodes = PlaylistReader.getEpisodes(playlistId);
        List<Long> expected = List.of(news.getItems().get(4).getId(),
                series.getItems().get(1).getId(), series.getItems().get(2).getId());
        assertEquals(expected, ids(episodes));
        assertEquals(news.getId(), episodes.get(0).getFeed().getId());
    }

    @Test
    public void testExcludedEpisodesExtendOldestWindow() throws Exception {
        Feed series = createFeed(5);
        Feed news = createFeed(5);
        long playlistId = PlaylistWriter.createPlaylist(new Playlist("P")).get();
        PlaylistRule seriesRule = new PlaylistRule(playlistId, series.getId());
        seriesRule.setEpisodeCount(1);
        seriesRule.setPickFrom(PlaylistRule.PickFrom.OLDEST);
        PlaylistWriter.addRule(seriesRule).get();
        PlaylistRule newsRule = new PlaylistRule(playlistId, news.getId());
        newsRule.setEpisodeCount(1);
        newsRule.setPriority(2);
        PlaylistWriter.addRule(newsRule).get();

        Set<Long> excluded = new HashSet<>();
        excluded.add(series.getItems().get(0).getId());
        excluded.add(news.getItems().get(4).getId());
        List<Long> expected = List.of(series.getItems().get(1).getId(), news.getItems().get(4).getId());
        assertEquals(expected, ids(PlaylistReader.getEpisodes(playlistId, excluded)));
    }

    @Test
    public void testAutoDownloadAndProtectedEpisodes() throws Exception {
        Feed news = createFeed(3);
        Feed series = createFeed(3);
        Playlist autoPlaylist = new Playlist("Auto");
        autoPlaylist.setAutoDownload(true);
        long autoId = PlaylistWriter.createPlaylist(autoPlaylist).get();
        PlaylistRule newsRule = new PlaylistRule(autoId, news.getId());
        newsRule.setEpisodeCount(1);
        PlaylistWriter.addRule(newsRule).get();
        long manualId = PlaylistWriter.createPlaylist(new Playlist("Manual")).get();
        PlaylistRule seriesRule = new PlaylistRule(manualId, series.getId());
        seriesRule.setEpisodeCount(1);
        seriesRule.setPickFrom(PlaylistRule.PickFrom.OLDEST);
        PlaylistWriter.addRule(seriesRule).get();

        assertEquals(List.of(news.getItems().get(2).getId()), ids(PlaylistReader.getAutoDownloadEpisodes()));
        Set<Long> expected = new HashSet<>();
        expected.add(news.getItems().get(2).getId());
        expected.add(series.getItems().get(0).getId());
        assertEquals(expected, PlaylistReader.getEpisodeIdsInPlaylists());
    }

    @Test
    public void testManualPlaylistKeepsOrderAndDropsPlayed() throws Exception {
        Feed feed = createFeed(4);
        Playlist manual = new Playlist("Road trip");
        manual.setManual(true);
        long id = PlaylistWriter.createPlaylist(manual).get();
        assertTrue(PlaylistReader.getPlaylist(id).isManual());

        PlaylistWriter.addEpisodes(id, List.of(feed.getItems().get(2), feed.getItems().get(0))).get();
        PlaylistWriter.addEpisodes(id, List.of(feed.getItems().get(0), feed.getItems().get(3))).get();
        assertEquals(List.of(itemId(feed, 2), itemId(feed, 0), itemId(feed, 3)), ids(PlaylistReader.getEpisodes(id)));

        markPlayed(feed.getItems().get(0));
        assertEquals(List.of(itemId(feed, 2), itemId(feed, 3)), ids(PlaylistReader.getEpisodes(id)));
        Playlist stored = PlaylistReader.getPlaylist(id);
        stored.setKeepPlayed(true);
        PlaylistWriter.updatePlaylist(stored).get();
        assertEquals(List.of(itemId(feed, 2), itemId(feed, 0), itemId(feed, 3)), ids(PlaylistReader.getEpisodes(id)));

        int position = PlaylistWriter.removeEpisode(id, feed.getItems().get(0)).get();
        assertEquals(1, position);
        PlaylistWriter.addEpisodeAt(id, feed.getItems().get(0), position).get();
        assertEquals(List.of(itemId(feed, 2), itemId(feed, 0), itemId(feed, 3)), ids(PlaylistReader.getEpisodes(id)));

        PlaylistWriter.setEpisodeOrder(id, List.of(feed.getItems().get(3), feed.getItems().get(2))).get();
        assertEquals(List.of(itemId(feed, 3), itemId(feed, 2), itemId(feed, 0)), ids(PlaylistReader.getEpisodes(id)));
        assertEquals(Set.of(id), PlaylistReader.getPlaylistIdsContaining(itemId(feed, 3)));
    }

    @Test
    public void testManualPlaylistMembershipAndProtection() throws Exception {
        Feed feed = createFeed(3);
        Playlist first = new Playlist("First");
        first.setManual(true);
        first.setAutoDownload(true);
        long firstId = PlaylistWriter.createPlaylist(first).get();
        Playlist second = new Playlist("Second");
        second.setManual(true);
        long secondId = PlaylistWriter.createPlaylist(second).get();

        PlaylistWriter.setMembership(List.of(feed.getItems().get(1)), List.of(firstId, secondId), List.of()).get();
        assertEquals(Set.of(firstId, secondId), PlaylistReader.getPlaylistIdsContaining(itemId(feed, 1)));
        assertEquals(List.of(itemId(feed, 1)), ids(PlaylistReader.getAutoDownloadEpisodes()));
        assertEquals(Set.of(itemId(feed, 1)), PlaylistReader.getEpisodeIdsInPlaylists());

        PlaylistWriter.setMembership(List.of(feed.getItems().get(1)), List.of(), List.of(firstId)).get();
        assertEquals(Set.of(secondId), PlaylistReader.getPlaylistIdsContaining(itemId(feed, 1)));
        assertTrue(PlaylistReader.getAutoDownloadEpisodes().isEmpty());
    }

    @Test
    public void testConvertSmartPlaylistToManual() throws Exception {
        Feed feed = createFeed(3);
        long smartId = PlaylistWriter.createPlaylist(new Playlist("Smart")).get();
        PlaylistRule rule = new PlaylistRule(smartId, feed.getId());
        rule.setEpisodeCount(2);
        rule.setPickFrom(PlaylistRule.PickFrom.OLDEST);
        PlaylistWriter.addRule(rule).get();

        long manualId = PlaylistWriter.convertToManual(smartId, "Frozen").get();
        Playlist manual = PlaylistReader.getPlaylist(manualId);
        assertTrue(manual.isManual());
        assertTrue(PlaylistReader.getRules(manualId).isEmpty());
        assertEquals(ids(PlaylistReader.getEpisodes(smartId)), ids(PlaylistReader.getEpisodes(manualId)));
        long copyId = PlaylistWriter.duplicatePlaylist(manualId, "Copy").get();
        assertTrue(PlaylistReader.getPlaylist(copyId).isManual());
        assertEquals(ids(PlaylistReader.getEpisodes(manualId)), ids(PlaylistReader.getEpisodes(copyId)));
    }

    @Test
    public void testDuplicatePlaylist() throws Exception {
        Feed feed = createFeed(3);
        Playlist original = new Playlist("Original");
        original.setAutoDownload(true);
        long originalId = PlaylistWriter.createPlaylist(original).get();
        PlaylistRule rule = new PlaylistRule(originalId, feed.getId());
        rule.setEpisodeCount(2);
        rule.setPickFrom(PlaylistRule.PickFrom.OLDEST);
        PlaylistWriter.addRule(rule).get();

        long copyId = PlaylistWriter.duplicatePlaylist(originalId, "Copy").get();
        assertTrue(copyId != originalId);
        Playlist copy = PlaylistReader.getPlaylist(copyId);
        assertEquals("Copy", copy.getName());
        assertTrue(copy.isAutoDownload());
        List<PlaylistRule> rules = PlaylistReader.getRules(copyId);
        assertEquals(1, rules.size());
        assertEquals(2, rules.get(0).getEpisodeCount());
        assertEquals(PlaylistRule.PickFrom.OLDEST, rules.get(0).getPickFrom());
        assertEquals(ids(PlaylistReader.getEpisodes(originalId)), ids(PlaylistReader.getEpisodes(copyId)));
    }

    @Test
    public void testCountEpisodesHiddenByFilters() throws Exception {
        Feed feed = createFeed(3);
        long playlistId = PlaylistWriter.createPlaylist(new Playlist("Filtered")).get();
        PlaylistRule rule = new PlaylistRule(playlistId, feed.getId());
        rule.setPickFrom(PlaylistRule.PickFrom.OLDEST);
        PlaylistWriter.addRule(rule).get();
        EpisodeFilters.setAdvancedFilter(feed.getId(), new AdvancedEpisodeFilter(Collections.emptyList(), false,
                List.of(EpisodeCondition.contains(EpisodeCondition.Field.TITLE, "Episode", false)), false));

        setFilterStyle(EpisodeFilterPreferences.STYLE_BASIC);
        assertEquals(0, PlaylistReader.countEpisodesHiddenByFilters(playlistId));
        setFilterStyle(EpisodeFilterPreferences.STYLE_ADVANCED);
        assertTrue(PlaylistReader.getEpisodes(playlistId).isEmpty());
        assertEquals(3, PlaylistReader.countEpisodesHiddenByFilters(playlistId));
    }

    @Test
    public void testDismissedEpisodes() throws Exception {
        Feed feed = createFeed(2);
        assertTrue(PlaylistReader.getDismissedEpisodeIds().isEmpty());
        PlaylistWriter.dismissEpisode(feed.getItems().get(1)).get();
        PlaylistWriter.dismissEpisode(feed.getItems().get(1)).get();
        assertEquals(Set.of(feed.getItems().get(1).getId()), PlaylistReader.getDismissedEpisodeIds());
    }

    @Test
    public void testProtectedEpisodesAreCappedPerRule() throws Exception {
        Feed feed = createFeed(15);
        long playlistId = PlaylistWriter.createPlaylist(new Playlist("All")).get();
        PlaylistRule rule = new PlaylistRule(playlistId, feed.getId());
        rule.setPickFrom(PlaylistRule.PickFrom.OLDEST);
        PlaylistWriter.addRule(rule).get();

        assertEquals(15, PlaylistReader.getEpisodes(playlistId).size());
        Set<Long> expected = new HashSet<>();
        for (int i = 0; i < PlaylistRule.MAX_AUTO_DOWNLOAD_EPISODES; i++) {
            expected.add(feed.getItems().get(i).getId());
        }
        assertEquals(expected, PlaylistReader.getEpisodeIdsInPlaylists());
        assertEquals(0, PlaylistReader.getDownloadedEpisodeCountInPlaylists());

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        for (int i : new int[]{0, 1, 14}) {
            FeedMedia media = feed.getItems().get(i).getMedia();
            media.setLocalFileUrl("file-" + i);
            media.setDownloaded(true, 1);
            adapter.setMedia(media);
        }
        adapter.close();
        assertEquals(2, PlaylistReader.getDownloadedEpisodeCountInPlaylists());
    }

    @Test
    public void testAdvancedFilterAppliesToPlaylists() throws Exception {
        Feed feed = createFeed(6);
        long playlistId = PlaylistWriter.createPlaylist(new Playlist("Filtered")).get();
        PlaylistRule rule = new PlaylistRule(playlistId, feed.getId());
        rule.setPickFrom(PlaylistRule.PickFrom.OLDEST);
        rule.setEpisodeCount(3);
        PlaylistWriter.addRule(rule).get();
        AdvancedEpisodeFilter filter = new AdvancedEpisodeFilter(Collections.emptyList(), false,
                List.of(EpisodeCondition.contains(EpisodeCondition.Field.TITLE, "Episode 1", true)), false);
        EpisodeFilters.setAdvancedFilter(feed.getId(), filter);
        AdvancedEpisodeFilter stored = EpisodeFilters.getAdvancedFilter(feed.getId());
        assertEquals(filter.getSkipConditions(), stored.getSkipConditions());
        assertTrue(stored.getKeepConditions().isEmpty());

        setFilterStyle(EpisodeFilterPreferences.STYLE_BASIC);
        assertEquals(List.of(itemId(feed, 0), itemId(feed, 1), itemId(feed, 2)),
                ids(PlaylistReader.getEpisodes(playlistId)));

        setFilterStyle(EpisodeFilterPreferences.STYLE_ADVANCED);
        assertEquals(List.of(itemId(feed, 0), itemId(feed, 2), itemId(feed, 3)),
                ids(PlaylistReader.getEpisodes(playlistId)));
    }

    @Test
    public void testDownloadedOnlyPlaylist() throws Exception {
        Feed feed = createFeed(6);
        Playlist playlist = new Playlist("Downloaded");
        long playlistId = PlaylistWriter.createPlaylist(playlist).get();
        PlaylistRule rule = new PlaylistRule(playlistId, feed.getId());
        rule.setPickFrom(PlaylistRule.PickFrom.OLDEST);
        rule.setEpisodeCount(3);
        PlaylistWriter.addRule(rule).get();
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        for (int i : new int[]{1, 4}) {
            FeedMedia media = feed.getItems().get(i).getMedia();
            media.setLocalFileUrl("file-" + i);
            media.setDownloaded(true, 1);
            adapter.setMedia(media);
        }
        adapter.close();
        assertEquals(3, PlaylistReader.getEpisodes(playlistId).size());

        Playlist stored = PlaylistReader.getPlaylist(playlistId);
        stored.setDownloadedOnly(true);
        PlaylistWriter.updatePlaylist(stored).get();
        assertTrue(PlaylistReader.getPlaylist(playlistId).isDownloadedOnly());
        assertEquals(List.of(itemId(feed, 1), itemId(feed, 4)), ids(PlaylistReader.getEpisodes(playlistId)));
    }

    private void setFilterStyle(String style) {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE).edit()
                .putString(EpisodeFilterPreferences.PREF_EPISODE_FILTER_STYLE, style).commit();
        EpisodeFilterPreferences.init(context);
    }

    @Test
    public void testArchivedPodcastsAreLeftOut() throws Exception {
        Feed kept = createFeed(2);
        Feed archived = createFeed(2);
        long id = PlaylistWriter.createPlaylist(new Playlist("Mix")).get();
        PlaylistWriter.addRule(new PlaylistRule(id, kept.getId())).get();
        PlaylistWriter.addRule(new PlaylistRule(id, archived.getId())).get();
        assertEquals(4, PlaylistReader.getEpisodes(id).size());

        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setFeedState(archived.getId(), Feed.STATE_ARCHIVED);
        adapter.close();
        List<FeedItem> episodes = PlaylistReader.getEpisodes(id);
        assertEquals(2, episodes.size());
        for (FeedItem episode : episodes) {
            assertEquals(kept.getId(), episode.getFeedId());
        }
        assertTrue(PlaylistReader.getPodcastEpisodes(archived.getId(), Collections.emptySet()).isEmpty());
    }

    private static long itemId(Feed feed, int index) {
        return feed.getItems().get(index).getId();
    }

    private Feed createFeed(int numEpisodes) {
        Feed feed = new Feed("url-" + System.nanoTime(), null, "Feed");
        feed.setItems(new ArrayList<>());
        for (int i = 0; i < numEpisodes; i++) {
            FeedItem item = new FeedItem(0, "Episode " + i, "guid-" + i, null,
                    new Date(1_000_000L * (i + 1)), FeedItem.UNPLAYED, feed);
            item.setMedia(new FeedMedia(item, "url-" + i, 2, "audio/mp3"));
            feed.getItems().add(item);
        }
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();
        return feed;
    }

    private void markPlayed(FeedItem item) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setFeedItemsRead(FeedItem.PLAYED, false, List.of(item));
        adapter.close();
    }

    private static List<String> names(List<Playlist> playlists) {
        List<String> names = new ArrayList<>();
        for (Playlist playlist : playlists) {
            names.add(playlist.getName());
        }
        return names;
    }

    private static List<Long> ids(List<FeedItem> items) {
        List<Long> ids = new ArrayList<>();
        for (FeedItem item : items) {
            ids.add(item.getId());
        }
        return ids;
    }
}
