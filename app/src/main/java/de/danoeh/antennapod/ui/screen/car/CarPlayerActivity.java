package de.danoeh.antennapod.ui.screen.car;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.graphics.drawable.IconCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.session.SessionCommand;
import androidx.palette.graphics.Palette;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.actionbutton.ItemActionButton;
import de.danoeh.antennapod.actionbutton.PlayActionButton;
import de.danoeh.antennapod.actionbutton.StreamActionButton;
import de.danoeh.antennapod.event.PlayerStatusEvent;
import de.danoeh.antennapod.event.playback.PlaybackPositionEvent;
import de.danoeh.antennapod.event.playback.PlaybackServiceEvent;
import de.danoeh.antennapod.model.feed.Chapter;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedMedia;
import de.danoeh.antennapod.model.playback.Playable;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.playback.service.PlaybackController;
import de.danoeh.antennapod.playback.service.PlaybackServiceInterface;
import de.danoeh.antennapod.playback.service.PlaybackServiceStarter;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.FeedSkipForward;
import de.danoeh.antennapod.storage.database.PlaylistPlayback;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.storage.preferences.PlaybackPreferences;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.appstartintent.PlaylistPlayStarter;
import de.danoeh.antennapod.ui.chapters.ChapterUtils;
import de.danoeh.antennapod.ui.common.PlaylistTime;
import de.danoeh.antennapod.ui.episodes.ImageResourceUtils;
import de.danoeh.antennapod.ui.episodes.PlaybackSpeedUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class CarPlayerActivity extends AppCompatActivity {
    private static final String TAG = "CarPlayerActivity";
    private static final String SHORTCUT_ID = "car_player";
    private static final long UNDO_MS = 6000;
    private static final double MIN_ACCENT_CONTRAST = 4.5;
    private static final SessionCommand NEXT_CHAPTER = new SessionCommand("next_chapter", Bundle.EMPTY);

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final List<View> tiles = new ArrayList<>();

    private View playerView;
    private View pinnedView;
    private TextView source;
    private ImageView cover;
    private TextView podcast;
    private TextView title;
    private ProgressBar progress;
    private TextView timeLeft;
    private TextView rewindLabel;
    private TextView forwardLabel;
    private ImageButton play;
    private View nextChapter;
    private View nextEpisode;
    private TextView tabPlaylists;
    private TextView tabPodcasts;
    private View undo;

    @Nullable
    private FeedMedia currentMedia;
    private int lastPosition;
    @ColorInt
    private int accent;
    @Nullable
    private String loadedImage;
    @Nullable
    private CustomTarget<Bitmap> coverTarget;
    private long undoMediaId;
    private long undoPlaylistId;
    private long undoPodcastId;

    public static Intent getIntent(@NonNull Context context) {
        return new Intent(context, CarPlayerActivity.class);
    }

    public static boolean pinShortcut(@NonNull Context context) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) {
            return false;
        }
        String label = context.getString(R.string.car_player_title);
        ShortcutInfoCompat shortcut = new ShortcutInfoCompat.Builder(context, SHORTCUT_ID)
                .setShortLabel(label)
                .setLongLabel(label)
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_car_shortcut))
                .setIntent(getIntent(context).setAction(Intent.ACTION_VIEW))
                .build();
        return ShortcutManagerCompat.requestPinShortcut(context, shortcut, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!CarPlayerPreferences.isEnabled(this)) {
            Toast.makeText(this, R.string.car_player_disabled, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        setContentView(R.layout.car_player_activity);
        accent = ContextCompat.getColor(this, R.color.car_accent_default);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat insetsController =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(false);
        View root = findViewById(R.id.carRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        playerView = findViewById(R.id.carPlayerView);
        pinnedView = findViewById(R.id.carPinnedView);
        source = findViewById(R.id.carSource);
        cover = findViewById(R.id.carCover);
        podcast = findViewById(R.id.carPodcast);
        title = findViewById(R.id.carTitle);
        progress = findViewById(R.id.carProgress);
        timeLeft = findViewById(R.id.carTimeLeft);
        rewindLabel = findViewById(R.id.carRewindLabel);
        forwardLabel = findViewById(R.id.carForwardLabel);
        play = findViewById(R.id.carPlay);
        nextChapter = findViewById(R.id.carNextChapter);
        nextEpisode = findViewById(R.id.carNextEpisode);
        tabPlaylists = findViewById(R.id.carTabPlaylists);
        tabPodcasts = findViewById(R.id.carTabPodcasts);
        undo = findViewById(R.id.carUndo);
        cover.setClipToOutline(true);
        cover.setBackground(rounded(ContextCompat.getColor(this, R.color.car_surface), 18, 0, 0));

        findViewById(R.id.carExit).setOnClickListener(v -> finish());
        findViewById(R.id.carRewind).setOnClickListener(v -> PlaybackController.bindToMedia3Service(this,
                controller -> {
                    if (controller.getCurrentMediaItem() != null) {
                        controller.seekBack();
                    }
                }));
        findViewById(R.id.carForward).setOnClickListener(v -> PlaybackController.bindToMedia3Service(this,
                controller -> {
                    if (controller.getCurrentMediaItem() != null) {
                        controller.seekForward();
                    }
                }));
        play.setOnClickListener(v -> togglePlay());
        nextEpisode.setOnClickListener(v -> skipToNextEpisode());
        nextChapter.setOnClickListener(v -> skipToNextChapter());
        findViewById(R.id.carPinned).setOnClickListener(v -> showPinned());
        findViewById(R.id.carPinnedBack).setOnClickListener(v -> showPlayer());
        findViewById(R.id.carUndoButton).setOnClickListener(v -> undoSkip());
        tabPlaylists.setOnClickListener(v -> selectTab(CarPins.TAB_PLAYLISTS));
        tabPodcasts.setOnClickListener(v -> selectTab(CarPins.TAB_PODCASTS));
        createTiles();
        applyAccent(accent);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (pinnedView.getVisibility() == View.VISIBLE) {
                    showPlayer();
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (CarPlayerPreferences.isKeepScreenOn(this)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        String next = CarPlayerPreferences.getNextButton(this);
        nextChapter.setVisibility(CarPlayerPreferences.NEXT_BOTH.equals(next) ? View.VISIBLE : View.GONE);
        boolean chapterOnly = CarPlayerPreferences.NEXT_CHAPTER.equals(next);
        TextView nextLabel = findViewById(R.id.carNextEpisodeLabel);
        nextLabel.setText(chapterOnly ? R.string.car_next_chapter : R.string.car_next_episode);
        float labelSize = CarPlayerPreferences.NEXT_BOTH.equals(next) ? 18 : 22;
        nextLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, labelSize);
        ((TextView) ((LinearLayout) nextChapter).getChildAt(1)).setTextSize(TypedValue.COMPLEX_UNIT_SP, labelSize);
        ((ImageView) ((LinearLayout) nextEpisode).getChildAt(0)).setImageResource(chapterOnly
                ? R.drawable.ic_car_next_chapter : R.drawable.ic_car_next_episode);
        nextEpisode.setOnClickListener(v -> {
            if (chapterOnly) {
                skipToNextChapter();
            } else {
                skipToNextEpisode();
            }
        });
        EventBus.getDefault().register(this);
        loadMedia();
        if (pinnedView.getVisibility() == View.VISIBLE) {
            loadPinned();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        disposables.clear();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlayerStatusChanged(PlayerStatusEvent event) {
        loadMedia();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaybackServiceChanged(PlaybackServiceEvent event) {
        loadMedia();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPositionChanged(PlaybackPositionEvent event) {
        if (event.getPosition() == Playable.INVALID_TIME || event.getDuration() == Playable.INVALID_TIME) {
            return;
        }
        updatePosition(event.getPosition(), event.getDuration());
    }

    private static final class PlayerState {
        @Nullable
        FeedMedia media;
        String source = "";
        int forwardSeconds;
    }

    private void loadMedia() {
        Context context = getApplicationContext();
        disposables.add(Single.fromCallable(() -> {
            PlayerState state = new PlayerState();
            state.media = DBReader.getFeedMedia(PlaybackPreferences.getCurrentlyPlayingFeedMediaId());
            if (state.media != null) {
                ChapterUtils.loadChapters(state.media, context, false);
            }
            state.source = describeSource(context);
            state.forwardSeconds = FeedSkipForward.getEffectiveSeconds(state.media);
            return state;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showMedia, error -> Log.e(TAG, Log.getStackTraceString(error))));
    }

    @NonNull
    private static String describeSource(@NonNull Context context) {
        long podcastId = PlaylistPlayback.getActivePodcast(context);
        if (podcastId != PlaylistPlayback.NO_PLAYLIST) {
            Feed feed = findFeed(podcastId);
            int left = PlaylistReader.getPodcastEpisodes(podcastId, Collections.emptySet()).size();
            return withCount(context, feed != null ? feed.getTitle() : "", left);
        }
        long playlistId = PlaylistPlayback.getActivePlaylist(context);
        if (playlistId != PlaylistPlayback.NO_PLAYLIST) {
            Playlist playlist = PlaylistReader.getPlaylist(playlistId);
            int left = PlaylistReader.getEpisodes(playlistId).size();
            return withCount(context, playlist != null ? playlist.getName() : "", left);
        }
        return withCount(context, context.getString(R.string.queue_label), DBReader.getQueue().size());
    }

    @NonNull
    private static String withCount(@NonNull Context context, @NonNull String name, int left) {
        return context.getString(R.string.car_source, name,
                context.getResources().getQuantityString(R.plurals.car_episodes_left, left, left));
    }

    @Nullable
    private static Feed findFeed(long feedId) {
        for (Feed feed : DBReader.getFeedList()) {
            if (feed.getId() == feedId) {
                return feed;
            }
        }
        return null;
    }

    private void showMedia(@NonNull PlayerState state) {
        currentMedia = state.media;
        source.setText(state.source);
        boolean playing = PlaybackServiceInterface.isPlaying
                && PlaybackPreferences.getCurrentPlayerStatus() == PlaybackPreferences.PLAYER_STATUS_PLAYING;
        play.setImageResource(playing ? R.drawable.ic_car_pause : R.drawable.ic_car_play);
        play.setContentDescription(getString(playing ? R.string.pause_label : R.string.play_label));
        rewindLabel.setText(getString(R.string.car_seconds, UserPreferences.getRewindSecs()));
        forwardLabel.setText(getString(R.string.car_seconds, state.forwardSeconds));
        if (currentMedia == null) {
            podcast.setText("");
            title.setText(R.string.car_nothing_playing);
            progress.setProgress(0);
            timeLeft.setText("");
            cover.setImageDrawable(null);
            loadedImage = null;
            applyAccent(ContextCompat.getColor(this, R.color.car_accent_default));
            return;
        }
        podcast.setText(currentMedia.getFeedTitle());
        title.setText(currentMedia.getEpisodeTitle());
        updatePosition(currentMedia.getPosition(), currentMedia.getDuration());
        loadCover(currentMedia);
    }

    private void updatePosition(int position, int duration) {
        lastPosition = position;
        if (duration <= 0) {
            progress.setProgress(0);
            timeLeft.setText("");
            return;
        }
        progress.setProgress((int) (1000L * position / duration));
        float speed = currentMedia != null ? PlaybackSpeedUtils.getCurrentPlaybackSpeed(currentMedia) : 1;
        long left = (long) (Math.max(0, duration - position) / Math.max(0.1f, speed));
        timeLeft.setText(getString(R.string.playlist_time_left, PlaylistTime.format(this, left)));
    }

    private void loadCover(@NonNull FeedMedia media) {
        String image = media.getImageLocation();
        if (image != null && image.equals(loadedImage)) {
            return;
        }
        loadedImage = image;
        Glide.with(this)
                .load(ImageResourceUtils.getEpisodeListImageLocation(media))
                .error(Glide.with(this).load(ImageResourceUtils.getFallbackImageLocation(media)))
                .centerCrop()
                .into(cover);
        coverTarget = new CustomTarget<Bitmap>(256, 256) {
            @Override
            public void onResourceReady(@NonNull Bitmap bitmap, @Nullable Transition<? super Bitmap> transition) {
                Palette.from(bitmap).generate(palette -> {
                    if (!isDestroyed() && palette != null) {
                        applyAccent(accentFrom(palette));
                    }
                });
            }

            @Override
            public void onLoadFailed(@Nullable Drawable errorDrawable) {
                applyAccent(ContextCompat.getColor(CarPlayerActivity.this, R.color.car_accent_default));
            }

            @Override
            public void onLoadCleared(@Nullable Drawable placeholder) {
            }
        };
        Glide.with(this)
                .asBitmap()
                .load(image)
                .error(Glide.with(this).asBitmap().load(ImageResourceUtils.getFallbackImageLocation(media)))
                .into(coverTarget);
    }

    @ColorInt
    private int accentFrom(@NonNull Palette palette) {
        int fallback = ContextCompat.getColor(this, R.color.car_accent_default);
        int color = palette.getVibrantColor(palette.getLightVibrantColor(palette.getDominantColor(fallback)));
        final int background = ContextCompat.getColor(this, R.color.car_background);
        for (int i = 0; i < 12 && ColorUtils.calculateContrast(color, background) < MIN_ACCENT_CONTRAST; i++) {
            color = ColorUtils.blendARGB(color, Color.WHITE, 0.15f);
        }
        return color;
    }

    private void applyAccent(@ColorInt int color) {
        accent = color;
        play.setBackgroundTintList(ColorStateList.valueOf(color));
        progress.setProgressTintList(ColorStateList.valueOf(color));
        timeLeft.setTextColor(color);
        updateTabs();
    }

    private void togglePlay() {
        if (currentMedia == null) {
            showPinned();
            return;
        }
        if (PlaybackServiceInterface.isPlaying
                && PlaybackPreferences.getCurrentPlayerStatus() == PlaybackPreferences.PLAYER_STATUS_PLAYING) {
            PlaybackController.bindToMedia3Service(this, controller -> controller.pause());
        } else {
            new PlaybackServiceStarter(this, currentMedia).callEvenIfRunning(true).start();
        }
    }

    private void skipToNextEpisode() {
        FeedMedia media = currentMedia;
        if (media == null) {
            return;
        }
        undoMediaId = media.getId();
        undoPlaylistId = PlaylistPlayback.getActivePlaylist(this);
        undoPodcastId = PlaylistPlayback.getActivePodcast(this);
        PlaybackController.bindToMedia3Service(this, controller -> controller.seekToNextMediaItem());
        undo.setVisibility(View.VISIBLE);
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> undo.setVisibility(View.GONE), UNDO_MS);
    }

    private void skipToNextChapter() {
        FeedMedia media = currentMedia;
        if (media == null) {
            return;
        }
        List<Chapter> chapters = media.getChapters();
        if (chapters != null && Chapter.getAfterPosition(chapters, lastPosition) + 1 < chapters.size()) {
            PlaybackController.bindToMedia3Service(this,
                    controller -> controller.sendCustomCommand(NEXT_CHAPTER, Bundle.EMPTY));
        } else {
            skipToNextEpisode();
        }
    }

    private void undoSkip() {
        handler.removeCallbacksAndMessages(null);
        undo.setVisibility(View.GONE);
        long mediaId = undoMediaId;
        long playlistId = undoPlaylistId;
        long podcastId = undoPodcastId;
        Context context = getApplicationContext();
        disposables.add(Maybe.fromCallable(() -> DBReader.getFeedMedia(mediaId))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(media -> {
                    if (podcastId != PlaylistPlayback.NO_PLAYLIST) {
                        PlaylistPlayback.startPodcast(context, podcastId);
                    } else if (playlistId != PlaylistPlayback.NO_PLAYLIST) {
                        PlaylistPlayback.start(context, playlistId);
                    }
                    new PlaybackServiceStarter(context, media).callEvenIfRunning(true).start();
                }, error -> Log.e(TAG, Log.getStackTraceString(error))));
    }

    private void showPinned() {
        playerView.setVisibility(View.GONE);
        pinnedView.setVisibility(View.VISIBLE);
        undo.setVisibility(View.GONE);
        loadPinned();
    }

    private void showPlayer() {
        pinnedView.setVisibility(View.GONE);
        playerView.setVisibility(View.VISIBLE);
    }

    private void selectTab(int tab) {
        CarPins.setTab(this, tab);
        loadPinned();
    }

    private void updateTabs() {
        boolean playlists = CarPins.getTab(this) == CarPins.TAB_PLAYLISTS;
        styleTab(tabPlaylists, playlists);
        styleTab(tabPodcasts, !playlists);
    }

    private void styleTab(@NonNull TextView tab, boolean selected) {
        tab.setBackground(selected ? rounded(ContextCompat.getColor(this, R.color.car_text), 16, 0, 0) : null);
        tab.setTextColor(ContextCompat.getColor(this, selected ? R.color.car_background : R.color.car_text_secondary));
        tab.setSelected(selected);
    }

    private static final class Tile {
        long id;
        boolean podcast;
        boolean manual;
        boolean playing;
        String name;
        String sub;
        String image;
    }

    private void createTiles() {
        LinearLayout container = findViewById(R.id.carTiles);
        LayoutInflater inflater = LayoutInflater.from(this);
        int gap = dp(12);
        for (int row = 0; row < CarPins.MAX / 2; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
            if (row > 0) {
                lineParams.topMargin = gap;
            }
            container.addView(line, lineParams);
            for (int column = 0; column < 2; column++) {
                View tile = inflater.inflate(R.layout.car_tile, line, false);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) tile.getLayoutParams();
                if (column > 0) {
                    params.setMarginStart(gap);
                }
                tile.setClipToOutline(true);
                line.addView(tile, params);
                tiles.add(tile);
            }
        }
    }

    private void loadPinned() {
        updateTabs();
        Context context = getApplicationContext();
        boolean playlists = CarPins.getTab(this) == CarPins.TAB_PLAYLISTS;
        disposables.add(Single.fromCallable(() -> {
            CarPins.ensureInitialized(context);
            return playlists ? loadPlaylistTiles(context) : loadPodcastTiles(context);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::showTiles, error -> Log.e(TAG, Log.getStackTraceString(error))));
    }

    @NonNull
    private static List<Tile> loadPlaylistTiles(@NonNull Context context) {
        long active = PlaylistPlayback.getActivePlaylist(context);
        List<Tile> result = new ArrayList<>();
        for (long id : CarPins.getPlaylists(context)) {
            Tile tile = new Tile();
            tile.id = id;
            List<FeedItem> episodes = new ArrayList<>();
            if (id == PlaylistPlayStarter.ALL_PLAYLISTS) {
                tile.name = context.getString(R.string.playlists_all);
                for (Playlist playlist : PlaylistReader.getPlaylists()) {
                    episodes.addAll(PlaylistReader.getEpisodes(playlist.getId()));
                }
            } else {
                Playlist playlist = PlaylistReader.getPlaylist(id);
                if (playlist == null) {
                    continue;
                }
                tile.name = playlist.getName();
                tile.manual = playlist.isManual();
                tile.playing = id == active;
                episodes = PlaylistReader.getEpisodes(id);
            }
            tile.sub = describeEpisodes(context, episodes);
            result.add(tile);
        }
        return result;
    }

    @NonNull
    private static List<Tile> loadPodcastTiles(@NonNull Context context) {
        long active = PlaylistPlayback.getActivePodcast(context);
        Map<Long, Feed> feeds = new HashMap<>();
        for (Feed feed : DBReader.getFeedList()) {
            feeds.put(feed.getId(), feed);
        }
        List<Tile> result = new ArrayList<>();
        for (long id : CarPins.getPodcasts(context)) {
            Feed feed = feeds.get(id);
            if (feed == null || feed.getState() != Feed.STATE_SUBSCRIBED) {
                continue;
            }
            Tile tile = new Tile();
            tile.id = id;
            tile.podcast = true;
            tile.name = feed.getTitle();
            tile.image = feed.getImageUrl();
            tile.playing = id == active;
            int left = PlaylistReader.getPodcastEpisodes(id, Collections.emptySet()).size();
            tile.sub = context.getResources().getQuantityString(R.plurals.car_episodes_left, left, left);
            result.add(tile);
        }
        return result;
    }

    @NonNull
    private static String describeEpisodes(@NonNull Context context, @NonNull List<FeedItem> episodes) {
        String count = context.getResources().getQuantityString(R.plurals.car_episodes_left,
                episodes.size(), episodes.size());
        long millis = PlaylistReader.getTimeLeftMillis(episodes);
        return millis > 0 ? context.getString(R.string.car_source, count, PlaylistTime.format(context, millis))
                : count;
    }

    private void showTiles(@NonNull List<Tile> data) {
        for (int i = 0; i < tiles.size(); i++) {
            bindTile(tiles.get(i), i < data.size() ? data.get(i) : null);
        }
    }

    private void bindTile(@NonNull View view, @Nullable Tile tile) {
        ImageView tileCover = view.findViewById(R.id.tileCover);
        ImageView kind = view.findViewById(R.id.tileKind);
        TextView playing = view.findViewById(R.id.tilePlaying);
        View label = view.findViewById(R.id.tileLabel);
        final TextView name = view.findViewById(R.id.tileName);
        final TextView sub = view.findViewById(R.id.tileSub);
        int surface = ContextCompat.getColor(this, R.color.car_surface);
        int outline = ContextCompat.getColor(this, R.color.car_outline);
        final int background = ContextCompat.getColor(this, R.color.car_background);
        int text = ContextCompat.getColor(this, R.color.car_text);
        int secondary = ContextCompat.getColor(this, R.color.car_text_secondary);
        int onAccent = ContextCompat.getColor(this, R.color.car_on_accent);
        if (tile == null) {
            view.setBackground(rounded(Color.TRANSPARENT, 22, 2, ColorUtils.setAlphaComponent(outline, 90)));
            view.setOnClickListener(null);
            view.setClickable(false);
            tileCover.setVisibility(View.GONE);
            kind.setVisibility(View.GONE);
            playing.setVisibility(View.GONE);
            label.setVisibility(View.INVISIBLE);
            view.setContentDescription(null);
            return;
        }
        label.setVisibility(View.VISIBLE);
        view.setClickable(true);
        view.setOnClickListener(v -> onTileClicked(tile));
        view.setContentDescription(tile.name);
        playing.setVisibility(tile.playing ? View.VISIBLE : View.GONE);
        playing.setBackground(rounded(background, 999, 0, 0));
        playing.setTextColor(accent);
        name.setText(tile.name);
        sub.setText(tile.sub);
        if (tile.podcast) {
            int stroke = tile.playing ? 4 : 2;
            view.setBackground(ripple(rounded(background, 22, stroke, tile.playing ? accent : outline)));
            tileCover.setVisibility(View.VISIBLE);
            int inset = dp(stroke);
            ((View) tileCover.getParent()).setPadding(inset, inset, inset, 0);
            kind.setVisibility(View.GONE);
            label.setBackgroundColor(background);
            name.setTextColor(text);
            name.setMaxLines(1);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            sub.setTextColor(tile.playing ? accent : secondary);
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            Glide.with(this).load(tile.image).centerCrop().into(tileCover);
        } else {
            view.setBackground(ripple(tile.playing ? rounded(accent, 22, 0, 0) : rounded(surface, 22, 2, outline)));
            Glide.with(this).clear(tileCover);
            tileCover.setVisibility(View.GONE);
            ((View) tileCover.getParent()).setPadding(0, 0, 0, 0);
            kind.setVisibility(View.VISIBLE);
            kind.setImageResource(tile.manual ? R.drawable.ic_playlist_kind_manual : R.drawable.ic_playlist_kind_smart);
            kind.setImageTintList(ColorStateList.valueOf(tile.playing ? onAccent : text));
            label.setBackground(null);
            name.setTextColor(tile.playing ? onAccent : text);
            name.setMaxLines(2);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
            sub.setTextColor(tile.playing ? onAccent : secondary);
            sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        }
    }

    private void onTileClicked(@NonNull Tile tile) {
        if (!tile.podcast) {
            startActivity(PlaylistPlayStarter.getIntent(this, tile.id));
            showPlayer();
            return;
        }
        Context context = getApplicationContext();
        long current = PlaybackPreferences.getCurrentlyPlayingFeedMediaId();
        disposables.add(Maybe.fromCallable(() -> {
            List<FeedItem> episodes = PlaylistReader.getPodcastEpisodes(tile.id, Collections.emptySet());
            FeedItem first = null;
            for (FeedItem episode : episodes) {
                if (episode.getMedia() == null) {
                    continue;
                }
                if (episode.getMedia().getId() == current) {
                    return episode;
                }
                if (first == null || (first.getMedia().getPosition() == 0 && episode.getMedia().getPosition() > 0)) {
                    first = episode;
                }
            }
            return first;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(episode -> {
                    PlaylistPlayback.startPodcast(context, tile.id);
                    ItemActionButton button = episode.getMedia().isDownloaded()
                            ? new PlayActionButton(episode) : new StreamActionButton(episode);
                    button.onClick(this);
                    showPlayer();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)),
                        () -> Toast.makeText(context, R.string.car_podcast_nothing, Toast.LENGTH_SHORT).show()));
    }

    @NonNull
    private GradientDrawable rounded(@ColorInt int fill, int radiusDp, int strokeDp, @ColorInt int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setColor(fill);
        if (strokeDp > 0) {
            drawable.setStroke(dp(strokeDp), stroke);
        }
        return drawable;
    }

    @NonNull
    private static Drawable ripple(@NonNull Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, null);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }
}
