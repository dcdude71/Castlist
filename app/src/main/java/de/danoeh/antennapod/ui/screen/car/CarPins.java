package de.danoeh.antennapod.ui.screen.car;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.feed.SortOrder;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class CarPins {
    public static final int MAX = 8;
    public static final int TAB_PLAYLISTS = 0;
    public static final int TAB_PODCASTS = 1;
    private static final String PREF_NAME = "CarPlayer";
    private static final String KEY_PLAYLISTS = "playlists";
    private static final String KEY_PODCASTS = "podcasts";
    private static final String KEY_INITIALIZED = "initialized";
    private static final String KEY_TAB = "tab";
    private static final int HISTORY_SCAN = 300;

    private CarPins() {
    }

    @NonNull
    public static List<Long> getPlaylists(@NonNull Context context) {
        return read(context, KEY_PLAYLISTS);
    }

    @NonNull
    public static List<Long> getPodcasts(@NonNull Context context) {
        return read(context, KEY_PODCASTS);
    }

    public static void setPlaylists(@NonNull Context context, @NonNull List<Long> ids) {
        write(context, KEY_PLAYLISTS, ids);
    }

    public static void setPodcasts(@NonNull Context context, @NonNull List<Long> ids) {
        write(context, KEY_PODCASTS, ids);
    }

    public static int getTab(@NonNull Context context) {
        return getPrefs(context).getInt(KEY_TAB, TAB_PLAYLISTS);
    }

    public static void setTab(@NonNull Context context, int tab) {
        getPrefs(context).edit().putInt(KEY_TAB, tab).apply();
    }

    @WorkerThread
    public static synchronized void ensureInitialized(@NonNull Context context) {
        if (getPrefs(context).getBoolean(KEY_INITIALIZED, false)) {
            return;
        }
        List<Long> playlists = new ArrayList<>();
        for (Playlist playlist : PlaylistReader.getPlaylists()) {
            if (playlists.size() < MAX) {
                playlists.add(playlist.getId());
            }
        }
        List<Long> podcasts = new ArrayList<>();
        List<FeedItem> history = DBReader.getEpisodes(0, HISTORY_SCAN,
                new FeedItemFilter(FeedItemFilter.IS_IN_HISTORY), SortOrder.COMPLETION_DATE_NEW_OLD);
        for (FeedItem item : history) {
            Feed feed = item.getFeed();
            if (podcasts.size() < MAX && feed != null && feed.getState() == Feed.STATE_SUBSCRIBED
                    && !podcasts.contains(feed.getId())) {
                podcasts.add(feed.getId());
            }
        }
        write(context, KEY_PLAYLISTS, playlists);
        write(context, KEY_PODCASTS, podcasts);
        getPrefs(context).edit().putBoolean(KEY_INITIALIZED, true).apply();
    }

    public static boolean isPlaylistPinned(@NonNull Context context, long playlistId) {
        return getPlaylists(context).contains(playlistId);
    }

    public static boolean isPodcastPinned(@NonNull Context context, long feedId) {
        return getPodcasts(context).contains(feedId);
    }

    public static void prepareFeedMenu(@NonNull Context context, @NonNull Menu menu, @NonNull Feed feed) {
        MenuItem item = menu.findItem(R.id.car_pin_feed);
        if (item == null) {
            return;
        }
        item.setVisible(CarPlayerPreferences.isEnabled(context) && feed.getState() == Feed.STATE_SUBSCRIBED);
        item.setChecked(isPodcastPinned(context, feed.getId()));
    }

    public static void togglePlaylist(@NonNull Context context, long playlistId) {
        toggle(context, KEY_PLAYLISTS, playlistId);
    }

    public static void togglePodcast(@NonNull Context context, long feedId) {
        toggle(context, KEY_PODCASTS, feedId);
    }

    private static void toggle(@NonNull Context context, @NonNull String key, long id) {
        Context appContext = context.getApplicationContext();
        Single.fromCallable(() -> {
            ensureInitialized(appContext);
            List<Long> ids = read(appContext, key);
            if (ids.remove(id)) {
                write(appContext, key, ids);
                return R.string.car_unpinned_toast;
            }
            if (ids.size() >= MAX) {
                return R.string.car_pins_full_menu;
            }
            ids.add(id);
            write(appContext, key, ids);
            return R.string.car_pinned_toast;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(message -> Toast.makeText(appContext, message, Toast.LENGTH_SHORT).show(),
                        error -> { });
    }

    @NonNull
    private static List<Long> read(@NonNull Context context, @NonNull String key) {
        List<Long> ids = new ArrayList<>();
        String value = getPrefs(context).getString(key, "");
        for (String part : TextUtils.split(value, ",")) {
            try {
                ids.add(Long.parseLong(part));
            } catch (NumberFormatException e) {
                continue;
            }
        }
        return ids;
    }

    private static void write(@NonNull Context context, @NonNull String key, @NonNull List<Long> ids) {
        getPrefs(context).edit().putString(key, TextUtils.join(",", ids)).apply();
    }

    private static SharedPreferences getPrefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
