package de.danoeh.antennapod.ui.screen.playlist;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.actionbutton.ItemActionButton;
import de.danoeh.antennapod.actionbutton.PlayActionButton;
import de.danoeh.antennapod.actionbutton.StreamActionButton;
import de.danoeh.antennapod.activity.MainActivity;
import de.danoeh.antennapod.event.FeedItemEvent;
import de.danoeh.antennapod.event.MessageEvent;
import de.danoeh.antennapod.event.PlaylistEvent;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.feed.FeedItemFilter;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.model.playlist.PlaylistRule;
import de.danoeh.antennapod.net.download.serviceinterface.AutoDownloadManager;
import de.danoeh.antennapod.storage.database.PlaylistPlayback;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.storage.database.PlaylistWriter;
import de.danoeh.antennapod.ui.common.ConfirmationDialog;
import de.danoeh.antennapod.ui.common.PlaylistTime;
import de.danoeh.antennapod.ui.screen.car.CarPins;
import de.danoeh.antennapod.ui.screen.car.CarPlayerPreferences;
import de.danoeh.antennapod.ui.episodeslist.EpisodeItemViewHolder;
import de.danoeh.antennapod.ui.episodeslist.EpisodesListFragment;
import de.danoeh.antennapod.ui.swipeactions.ManualPlaylistSwipeActions;

public class PlaylistEpisodesFragment extends EpisodesListFragment
        implements ManualPlaylistSwipeActions.Listener {
    public static final String TAG = "PlaylistEpisodesFragment";
    private static final String ARG_PLAYLIST_ID = "playlist_id";
    private static final String ARG_PREVIEW = "preview";
    private static final long JUST_ADDED_MILLIS = 2000;
    private static final int JUST_ADDED_COLOR = 0x5543A047;

    private long playlistId;
    private volatile Playlist playlist;
    private volatile int episodeCount;
    private volatile long timeLeftMillis;
    private volatile int hiddenByFilters;
    private volatile boolean hasUnlimitedRule;
    private volatile Set<Long> ruleFeedIds = Collections.emptySet();
    private boolean preview;
    private volatile Set<Long> previousEpisodeIds;
    private volatile Set<Long> justAddedIds = Collections.emptySet();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ManualPlaylistSwipeActions manualSwipeActions;

    public static PlaylistEpisodesFragment newInstance(long playlistId) {
        PlaylistEpisodesFragment fragment = new PlaylistEpisodesFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PLAYLIST_ID, playlistId);
        fragment.setArguments(args);
        return fragment;
    }

    public static PlaylistEpisodesFragment newPreviewInstance(long playlistId) {
        PlaylistEpisodesFragment fragment = newInstance(playlistId);
        fragment.requireArguments().putBoolean(ARG_PREVIEW, true);
        return fragment;
    }

    long getPlaylistId() {
        return playlistId;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playlistId = requireArguments().getLong(ARG_PLAYLIST_ID);
        preview = requireArguments().getBoolean(ARG_PREVIEW);
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = super.onCreateView(inflater, container, savedInstanceState);
        if (preview) {
            toolbar.setTitle(R.string.playlist_preview);
        } else {
            toolbar.inflateMenu(R.menu.playlist_episodes);
            toolbar.setTitle(R.string.playlists_label);
        }
        emptyView.setIcon(R.drawable.ic_playlist_play);
        emptyView.setTitle(R.string.playlist_episodes_empty_title);
        emptyView.setMessage(R.string.playlist_episodes_empty_message);
        return root;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.playlist_play) {
            playPlaylist();
            return true;
        } else if (itemId == R.id.playlist_edit_rules) {
            ((MainActivity) requireActivity()).loadChildFragment(PlaylistEditFragment.newInstance(playlistId));
            return true;
        } else if (itemId == R.id.playlist_auto_download) {
            Playlist current = playlist;
            if (current != null && !current.isAutoDownload() && hasUnlimitedRule) {
                showAutoDownloadNeedsLimitDialog();
                return true;
            }
            if (current != null) {
                current.setAutoDownload(!current.isAutoDownload());
                item.setChecked(current.isAutoDownload());
                Context context = requireContext().getApplicationContext();
                Future<?> update = PlaylistWriter.updatePlaylist(current);
                if (current.isAutoDownload()) {
                    Completable.fromFuture(update).subscribeOn(Schedulers.io()).subscribe(
                            () -> AutoDownloadManager.getInstance().autodownloadUndownloadedItems(context),
                            error -> Log.e(TAG, Log.getStackTraceString(error)));
                }
            }
            return true;
        } else if (itemId == R.id.playlist_downloaded_only) {
            Playlist current = playlist;
            if (current != null) {
                current.setDownloadedOnly(!current.isDownloadedOnly());
                item.setChecked(current.isDownloadedOnly());
                PlaylistWriter.updatePlaylist(current);
            }
            return true;
        } else if (itemId == R.id.playlist_android_auto) {
            Playlist current = playlist;
            if (current != null) {
                current.setShowInAndroidAuto(!current.isShowInAndroidAuto());
                item.setChecked(current.isShowInAndroidAuto());
                PlaylistWriter.updatePlaylist(current);
            }
            return true;
        } else if (itemId == R.id.playlist_car_pin) {
            CarPins.togglePlaylist(requireContext(), playlistId);
            item.setChecked(!item.isChecked());
            return true;
        } else if (itemId == R.id.playlist_add_to_home_screen) {
            Playlist current = playlist;
            if (current != null && !PlaylistShortcuts.pin(requireContext(), current)) {
                EventBus.getDefault().post(new MessageEvent(getString(R.string.playlist_shortcut_unsupported)));
            }
            return true;
        } else if (itemId == R.id.playlist_rename) {
            Playlist current = playlist;
            if (current != null) {
                PlaylistNameDialog.show(requireContext(), R.string.playlist_rename, current.getName(), name -> {
                    current.setName(name);
                    PlaylistWriter.updatePlaylist(current);
                });
            }
            return true;
        } else if (itemId == R.id.playlist_duplicate) {
            Playlist current = playlist;
            if (current != null) {
                PlaylistNameDialog.show(requireContext(), R.string.playlist_duplicate,
                        getString(R.string.playlist_copy_name, current.getName()),
                        name -> PlaylistWriter.duplicatePlaylist(playlistId, name));
            }
            return true;
        } else if (itemId == R.id.playlist_delete) {
            confirmDelete();
            return true;
        } else if (itemId == R.id.playlist_keep_played) {
            Playlist current = playlist;
            if (current != null) {
                current.setKeepPlayed(!current.isKeepPlayed());
                item.setChecked(current.isKeepPlayed());
                PlaylistWriter.updatePlaylist(current);
            }
            return true;
        } else if (itemId == R.id.playlist_convert_to_manual) {
            Playlist current = playlist;
            if (current != null) {
                PlaylistNameDialog.show(requireContext(), R.string.playlist_convert_to_manual, current.getName(),
                        name -> PlaylistWriter.convertToManual(playlistId, name));
            }
            return true;
        } else if (itemId == R.id.playlist_sort_date_old_new) {
            sortEpisodes((a, b) -> Long.compare(pubDate(a), pubDate(b)));
            return true;
        } else if (itemId == R.id.playlist_sort_date_new_old) {
            sortEpisodes((a, b) -> Long.compare(pubDate(b), pubDate(a)));
            return true;
        } else if (itemId == R.id.playlist_sort_duration_short_long) {
            sortEpisodes((a, b) -> Long.compare(duration(a), duration(b)));
            return true;
        } else if (itemId == R.id.playlist_sort_duration_long_short) {
            sortEpisodes((a, b) -> Long.compare(duration(b), duration(a)));
            return true;
        }
        return super.onMenuItemClick(item);
    }

    private void playPlaylist() {
        if (episodes.isEmpty() || episodes.get(0).getMedia() == null) {
            return;
        }
        FeedItem first = episodes.get(0);
        PlaylistPlayback.start(requireContext(), playlistId);
        ItemActionButton button = first.getMedia().isDownloaded()
                ? new PlayActionButton(first) : new StreamActionButton(first);
        button.onClick(requireContext());
    }

    private void playFrom(@NonNull FeedItem item) {
        if (item.getMedia() == null) {
            return;
        }
        PlaylistPlayback.start(requireContext(), playlistId);
        ItemActionButton button = item.getMedia().isDownloaded()
                ? new PlayActionButton(item) : new StreamActionButton(item);
        button.onClick(requireContext());
    }

    private void sortEpisodes(@NonNull Comparator<FeedItem> comparator) {
        List<FeedItem> sorted = new ArrayList<>(episodes);
        Collections.sort(sorted, comparator);
        PlaylistWriter.setEpisodeOrder(playlistId, sorted);
    }

    private static long pubDate(@NonNull FeedItem item) {
        return item.getPubDate() != null ? item.getPubDate().getTime() : 0;
    }

    private static long duration(@NonNull FeedItem item) {
        return item.getMedia() != null ? item.getMedia().getDuration() : 0;
    }

    private boolean isManual() {
        Playlist current = playlist;
        return current != null && current.isManual();
    }

    @Override
    public boolean onMove(int from, int to) {
        if (from >= episodes.size() || to >= episodes.size()) {
            return false;
        }
        episodes.add(to, episodes.remove(from));
        listAdapter.notifyItemMoved(from, to);
        return true;
    }

    @Override
    public void onMoveFinished() {
        PlaylistWriter.setEpisodeOrder(playlistId, new ArrayList<>(episodes));
    }

    @Override
    public void onRemove(@NonNull FeedItem item) {
        Context context = requireContext().getApplicationContext();
        Single.fromFuture(PlaylistWriter.removeEpisode(playlistId, item))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(position -> EventBus.getDefault().post(new MessageEvent(
                        context.getString(R.string.removed_from_playlist_message),
                        ctx -> PlaylistWriter.addEpisodeAt(playlistId, item, position),
                        context.getString(R.string.undo))),
                        error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void showAutoDownloadNeedsLimitDialog() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.playlist_auto_download_needs_limit_title)
                .setMessage(R.string.playlist_auto_download_needs_limit_message)
                .setPositiveButton(R.string.playlist_edit_rules, (dialog, which) ->
                        ((MainActivity) requireActivity()).loadChildFragment(
                                PlaylistEditFragment.newInstance(playlistId)))
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    @Override
    protected void onEpisodeBound(EpisodeItemViewHolder holder) {
        FeedItem item = holder.getFeedItem();
        if (isManual() && !preview && item != null) {
            bindManualRow(holder, item);
        } else {
            holder.secondaryActionButton.setVisibility(View.GONE);
        }
        boolean justAdded = item != null && justAddedIds.contains(item.getId());
        holder.itemView.setForeground(justAdded ? new ColorDrawable(JUST_ADDED_COLOR) : null);
    }

    private void bindManualRow(@NonNull EpisodeItemViewHolder holder, @NonNull FeedItem item) {
        holder.dragHandle.setVisibility(View.VISIBLE);
        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && manualSwipeActions != null) {
                manualSwipeActions.startDrag(holder);
            }
            return false;
        });
        if (!holder.isPlayingItem() && item.getMedia() != null) {
            holder.secondaryActionIcon.setImageResource(R.drawable.ic_play_24dp);
            holder.secondaryActionButton.setContentDescription(getString(R.string.playlist_play_from_here));
            holder.secondaryActionButton.setOnClickListener(v -> playFrom(item));
        }
    }

    private void setUpManualMode() {
        if (manualSwipeActions != null || preview || !isManual()) {
            return;
        }
        swipeActions.detach();
        manualSwipeActions = new ManualPlaylistSwipeActions(this, getFragmentTag(), this);
        manualSwipeActions.attachTo(recyclerView);
        swipeActions = manualSwipeActions;
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (isResumed()) {
            returnToPanesIfWide();
        }
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden) {
            returnToPanesIfWide();
        }
    }

    private void returnToPanesIfWide() {
        if (!preview && getParentFragment() == null && !isHidden()
                && PlaylistPanes.isWide(requireContext())) {
            PlaylistPanes.setSelected(requireContext(), playlistId);
            getParentFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void confirmDelete() {
        Playlist current = playlist;
        if (current == null) {
            return;
        }
        new ConfirmationDialog(requireContext(), R.string.playlist_delete,
                getString(R.string.playlist_delete_confirm, current.getName())) {
            @Override
            public void onConfirmButtonPressed(DialogInterface dialog) {
                dialog.dismiss();
                if (PlaylistPlayback.getActivePlaylist(requireContext()) == playlistId) {
                    PlaylistPlayback.stop(requireContext());
                }
                PlaylistWriter.deletePlaylist(playlistId);
                if (getParentFragment() instanceof PlaylistsFragment) {
                    ((PlaylistsFragment) getParentFragment()).onSelectedPlaylistDeleted();
                } else {
                    getParentFragmentManager().popBackStack();
                }
            }
        }.createNewDialog().show();
    }

    @Override
    protected void updateToolbar() {
        if (preview) {
            toolbar.setSubtitle(getResources().getQuantityString(
                    R.plurals.playlist_preview_summary, episodeCount, episodeCount));
            return;
        }
        Playlist current = playlist;
        if (current != null) {
            setUpManualMode();
            toolbar.setTitle(current.getName());
            String count = getResources().getQuantityString(R.plurals.playlist_num_episodes,
                    episodeCount, episodeCount);
            String subtitle = timeLeftMillis > 0 ? getString(R.string.playlist_summary, count,
                    getString(R.string.playlist_time_left,
                            PlaylistTime.format(requireContext(), timeLeftMillis))) : count;
            toolbar.setSubtitle(PlaylistKindUi.withKind(requireContext(), current.isManual(), subtitle,
                    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14, getResources().getDisplayMetrics())));
            boolean manual = current.isManual();
            toolbar.getMenu().findItem(R.id.playlist_edit_rules).setVisible(!manual);
            toolbar.getMenu().findItem(R.id.playlist_downloaded_only).setVisible(!manual);
            toolbar.getMenu().findItem(R.id.playlist_convert_to_manual).setVisible(!manual);
            toolbar.getMenu().findItem(R.id.playlist_sort).setVisible(manual);
            toolbar.getMenu().findItem(R.id.playlist_keep_played).setVisible(manual);
            toolbar.getMenu().findItem(R.id.playlist_keep_played).setChecked(current.isKeepPlayed());
            toolbar.getMenu().findItem(R.id.playlist_auto_download).setChecked(current.isAutoDownload());
            toolbar.getMenu().findItem(R.id.playlist_android_auto).setChecked(current.isShowInAndroidAuto());
            toolbar.getMenu().findItem(R.id.playlist_car_pin)
                    .setVisible(CarPlayerPreferences.isEnabled(requireContext()));
            toolbar.getMenu().findItem(R.id.playlist_car_pin).setChecked(CarPins.isPlaylistPinned(requireContext(),
                    playlistId));
            toolbar.getMenu().findItem(R.id.playlist_downloaded_only).setChecked(current.isDownloadedOnly());
            emptyView.setMessage(current.isDownloadedOnly()
                    ? R.string.playlist_episodes_empty_downloaded_message
                    : R.string.playlist_episodes_empty_message);
            if (manual) {
                emptyView.setMessage(R.string.playlist_manual_empty_message);
            }
            if (hiddenByFilters > 0) {
                emptyView.setMessage(getResources().getQuantityString(R.plurals.playlist_hidden_by_filters,
                        hiddenByFilters, hiddenByFilters));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onPlaylistEvent(PlaylistEvent event) {
        if (event.playlistId == playlistId || event.playlistId == PlaylistWriter.ALL_PLAYLISTS) {
            loadItems();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onFeedItemChanged(FeedItemEvent event) {
        Set<Long> feedIds = ruleFeedIds;
        for (FeedItem item : event.items) {
            if (item != null && feedIds.contains(item.getFeedId())) {
                loadItems();
                return;
            }
        }
    }

    @Override
    protected FeedItemFilter getFilter() {
        return FeedItemFilter.unfiltered();
    }

    @Override
    protected String getFragmentTag() {
        return TAG;
    }

    @NonNull
    @Override
    protected List<FeedItem> loadData() {
        playlist = PlaylistReader.getPlaylist(playlistId);
        boolean unlimited = false;
        Set<Long> feedIds = new HashSet<>();
        for (PlaylistRule rule : PlaylistReader.getRules(playlistId)) {
            if (rule.getEpisodeCount() == PlaylistRule.ALL_EPISODES) {
                unlimited = true;
            }
            feedIds.add(rule.getFeedId());
        }
        hasUnlimitedRule = unlimited;
        List<FeedItem> items = PlaylistReader.getEpisodes(playlistId);
        for (FeedItem item : items) {
            if (playlist != null && playlist.isManual()) {
                feedIds.add(item.getFeedId());
            }
        }
        ruleFeedIds = feedIds;
        episodeCount = items.size();
        timeLeftMillis = PlaylistReader.getTimeLeftMillis(items);
        hiddenByFilters = items.isEmpty() ? PlaylistReader.countEpisodesHiddenByFilters(playlistId) : 0;
        if (preview) {
            Set<Long> ids = new HashSet<>();
            for (FeedItem item : items) {
                ids.add(item.getId());
            }
            Set<Long> previous = previousEpisodeIds;
            previousEpisodeIds = ids;
            if (previous != null) {
                Set<Long> added = new HashSet<>(ids);
                added.removeAll(previous);
                if (!added.isEmpty()) {
                    justAddedIds = added;
                    handler.removeCallbacksAndMessages(null);
                    handler.postDelayed(this::clearJustAdded, JUST_ADDED_MILLIS);
                }
            }
        }
        return items;
    }

    private void clearJustAdded() {
        justAddedIds = Collections.emptySet();
        if (isAdded()) {
            loadItems();
        }
    }

    @NonNull
    @Override
    protected List<FeedItem> loadMoreData(int page) {
        return new ArrayList<>();
    }

    @Override
    protected int loadTotalItemCount() {
        return episodeCount;
    }
}
