package de.danoeh.antennapod.storage.database;

import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;

import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
public class FeedSkipForwardTest {

    @Before
    public void setUp() {
        Context context = RuntimeEnvironment.getApplication();
        UserPreferences.init(context);
        PodDBAdapter.init(context);
        PodDBAdapter.deleteDatabase();
    }

    @After
    public void tearDown() {
        PodDBAdapter.tearDownTests();
    }

    @Test
    public void testSetAndGet() {
        assertEquals(0, FeedSkipForward.getSeconds(1));
        FeedSkipForward.setSeconds(1, 30);
        FeedSkipForward.setSeconds(2, 45);
        assertEquals(30, FeedSkipForward.getSeconds(1));
        assertEquals(45, FeedSkipForward.getSeconds(2));
        FeedSkipForward.setSeconds(1, 20);
        assertEquals(20, FeedSkipForward.getSeconds(1));
        FeedSkipForward.setSeconds(1, 0);
        assertEquals(0, FeedSkipForward.getSeconds(1));
    }

    @Test
    public void testEffectiveSecondsFallsBackToGlobal() {
        UserPreferences.setFastForwardSecs(30);
        assertEquals(30, FeedSkipForward.getEffectiveSeconds(1));
        FeedSkipForward.setSeconds(1, 90);
        assertEquals(90, FeedSkipForward.getEffectiveSeconds(1));
        assertEquals(30, FeedSkipForward.getEffectiveSeconds(2));
        assertEquals(30, FeedSkipForward.getEffectiveSeconds(-1));
        FeedSkipForward.setSeconds(1, 0);
        assertEquals(30, FeedSkipForward.getEffectiveSeconds(1));
    }

    @Test
    public void testRemovingFeedRemovesSkipForward() {
        Feed feed = new Feed("url", null, "Feed");
        feed.setItems(new ArrayList<>());
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        adapter.setCompleteFeed(feed);
        adapter.close();
        FeedSkipForward.setSeconds(feed.getId(), 30);

        adapter.open();
        adapter.removeFeed(feed);
        adapter.close();
        assertEquals(0, FeedSkipForward.getSeconds(feed.getId()));
    }
}
