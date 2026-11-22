package de.danoeh.antennapod.storage.database;

import androidx.annotation.Nullable;

import org.greenrobot.eventbus.EventBus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.danoeh.antennapod.event.FeedSkipForwardEvent;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.storage.preferences.UserPreferences;

/**
 * Per-podcast skip forward time (Castlist). When set, it replaces the global fast-forward time for
 * every forward skip of that podcast's episodes: player, notification, headset, Android Auto, watch.
 * Values are cached in memory so playback code can read them on the main thread.
 */
public final class FeedSkipForward {
    private static final Map<Long, Integer> cache = new ConcurrentHashMap<>();
    private static volatile boolean loaded = false;

    private FeedSkipForward() {
    }

    /** The podcast's own time in seconds, or 0 when it uses the global setting. */
    public static int getSeconds(long feedId) {
        ensureLoaded();
        Integer seconds = cache.get(feedId);
        return seconds == null ? 0 : seconds;
    }

    /** The forward skip time to use for this podcast, in seconds. */
    public static int getEffectiveSeconds(long feedId) {
        int seconds = feedId < 0 ? 0 : getSeconds(feedId);
        return seconds > 0 ? seconds : UserPreferences.getFastForwardSecs();
    }

    /** The forward skip time to use for this media, in seconds. */
    public static int getEffectiveSeconds(@Nullable Playable playable) {
        if (playable instanceof FeedMedia && ((FeedMedia) playable).getItem() != null) {
            return getEffectiveSeconds(((FeedMedia) playable).getItem().getFeedId());
        }
        return UserPreferences.getFastForwardSecs();
    }

    public static synchronized void setSeconds(long feedId, int seconds) {
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            adapter.getFeedSkipForwardDbAdapter().setSeconds(feedId, seconds);
        } finally {
            adapter.close();
        }
        if (loaded) {
            if (seconds > 0) {
                cache.put(feedId, seconds);
            } else {
                cache.remove(feedId);
            }
        }
        EventBus.getDefault().post(new FeedSkipForwardEvent(feedId));
    }

    static void forget(long feedId) {
        cache.remove(feedId);
    }

    static void invalidate() {
        loaded = false;
    }

    private static synchronized void ensureLoaded() {
        if (loaded) {
            return;
        }
        PodDBAdapter adapter = PodDBAdapter.getInstance();
        adapter.open();
        try {
            Map<Long, Integer> all = adapter.getFeedSkipForwardDbAdapter().getAll();
            cache.clear();
            cache.putAll(all);
            loaded = true;
        } finally {
            adapter.close();
        }
    }
}
