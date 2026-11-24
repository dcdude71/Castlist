package de.danoeh.antennapod.ui.screen.car;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.model.feed.EpisodeCondition;
import de.danoeh.antennapod.model.feed.Feed;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.storage.database.DBReader;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.ui.appstartintent.PlaylistPlayStarter;
import de.danoeh.antennapod.ui.common.ThemeSwitcher;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class CarPinnedActivity extends AppCompatActivity {
    private static final String TAG = "CarPinnedActivity";

    private static final class Entry {
        final long id;
        final String name;
        final boolean podcast;
        final boolean manual;
        final String image;

        Entry(long id, String name, boolean podcast, boolean manual, String image) {
            this.id = id;
            this.name = name;
            this.podcast = podcast;
            this.manual = manual;
            this.image = image;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    private final List<Entry> pinned = new ArrayList<>();
    private final List<Entry> allPlaylists = new ArrayList<>();
    private final List<Entry> allPodcasts = new ArrayList<>();
    private int tab = CarPins.TAB_PLAYLISTS;
    private Adapter adapter;
    private ItemTouchHelper touchHelper;
    private TextView count;
    private TextView full;
    private MaterialButton add;
    private Disposable disposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeSwitcher.getNoTitleTheme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.car_pinned_activity);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        count = findViewById(R.id.count);
        full = findViewById(R.id.full);
        add = findViewById(R.id.add);
        add.setOnClickListener(v -> showAddDialog());

        TabLayout tabs = findViewById(R.id.tabs);
        tabs.addTab(tabs.newTab().setText(R.string.playlists_label));
        tabs.addTab(tabs.newTab().setText(R.string.car_podcasts));
        tab = CarPins.getTab(this);
        TabLayout.Tab selected = tabs.getTabAt(tab);
        if (selected != null) {
            selected.select();
        }
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab selectedTab) {
                tab = selectedTab.getPosition();
                showTab();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab unselectedTab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab reselectedTab) {
            }
        });

        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        list.setAdapter(adapter);
        touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int from = viewHolder.getBindingAdapterPosition();
                int to = target.getBindingAdapterPosition();
                Collections.swap(pinned, from, to);
                adapter.notifyItemMoved(from, to);
                save();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });
        touchHelper.attachToRecyclerView(list);
        load();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (disposable != null) {
            disposable.dispose();
        }
    }

    private void load() {
        disposable = Single.fromCallable(() -> {
            CarPins.ensureInitialized(getApplicationContext());
            List<Entry> playlists = new ArrayList<>();
            playlists.add(new Entry(PlaylistPlayStarter.ALL_PLAYLISTS, getString(R.string.playlists_all),
                    false, false, null));
            for (Playlist playlist : PlaylistReader.getPlaylists()) {
                playlists.add(new Entry(playlist.getId(), playlist.getName(), false, playlist.isManual(), null));
            }
            List<Entry> podcasts = new ArrayList<>();
            for (Feed feed : DBReader.getFeedList()) {
                if (feed.getState() == Feed.STATE_SUBSCRIBED) {
                    podcasts.add(new Entry(feed.getId(), feed.getTitle(), true, false, feed.getImageUrl()));
                }
            }
            Collections.sort(podcasts, (a, b) -> String.valueOf(a.name).compareToIgnoreCase(String.valueOf(b.name)));
            List<List<Entry>> result = new ArrayList<>();
            result.add(playlists);
            result.add(podcasts);
            return result;
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    allPlaylists.clear();
                    allPlaylists.addAll(result.get(0));
                    allPodcasts.clear();
                    allPodcasts.addAll(result.get(1));
                    showTab();
                }, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }

    private void showTab() {
        CarPins.setTab(this, tab);
        boolean podcasts = tab == CarPins.TAB_PODCASTS;
        List<Long> ids = podcasts ? CarPins.getPodcasts(this) : CarPins.getPlaylists(this);
        List<Entry> available = podcasts ? allPodcasts : allPlaylists;
        pinned.clear();
        for (long id : ids) {
            for (Entry entry : available) {
                if (entry.id == id) {
                    pinned.add(entry);
                    break;
                }
            }
        }
        adapter.notifyDataSetChanged();
        add.setText(podcasts ? R.string.car_add_podcast : R.string.car_add_playlist);
        updateCount();
    }

    private void updateCount() {
        count.setText(getString(R.string.car_pinned_count, pinned.size(), CarPins.MAX));
        boolean isFull = pinned.size() >= CarPins.MAX;
        add.setVisibility(isFull ? View.GONE : View.VISIBLE);
        full.setVisibility(isFull ? View.VISIBLE : View.GONE);
    }

    private void save() {
        List<Long> ids = new ArrayList<>();
        for (Entry entry : pinned) {
            ids.add(entry.id);
        }
        if (tab == CarPins.TAB_PODCASTS) {
            CarPins.setPodcasts(this, ids);
        } else {
            CarPins.setPlaylists(this, ids);
        }
        updateCount();
    }

    private void pin(@NonNull Entry entry) {
        if (pinned.size() >= CarPins.MAX) {
            return;
        }
        pinned.add(entry);
        adapter.notifyItemInserted(pinned.size() - 1);
        save();
    }

    private void showAddDialog() {
        List<Entry> candidates = new ArrayList<>();
        for (Entry entry : tab == CarPins.TAB_PODCASTS ? allPodcasts : allPlaylists) {
            if (!pinned.contains(entry)) {
                candidates.add(entry);
            }
        }
        if (candidates.isEmpty()) {
            new MaterialAlertDialogBuilder(this)
                    .setMessage(R.string.car_nothing_to_add)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        final int padding = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20,
                getResources().getDisplayMetrics()));
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        EditText search = new EditText(this);
        search.setHint(R.string.search_label);
        search.setSingleLine(true);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        searchParams.setMargins(padding, padding / 2, padding, 0);
        ListView listView = new ListView(this);
        ArrayAdapter<Entry> listAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, new ArrayList<>(candidates));
        listView.setAdapter(listAdapter);
        if (candidates.size() >= 10) {
            layout.addView(search, searchParams);
        }
        layout.addView(listView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(tab == CarPins.TAB_PODCASTS ? R.string.car_add_podcast : R.string.car_add_playlist)
                .setView(layout)
                .setNegativeButton(R.string.cancel_label, null)
                .create();
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                String query = EpisodeCondition.normalize(s.toString());
                listAdapter.clear();
                for (Entry entry : candidates) {
                    if (EpisodeCondition.normalize(entry.name).contains(query)) {
                        listAdapter.add(entry);
                    }
                }
            }
        });
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Entry entry = listAdapter.getItem(position);
            if (entry != null) {
                pin(entry);
            }
            dialog.dismiss();
        });
        dialog.show();
    }

    private class Adapter extends RecyclerView.Adapter<Holder> {
        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.car_pinned_item, parent, false));
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Entry entry = pinned.get(position);
            holder.title.setText(entry.name);
            if (entry.podcast) {
                holder.icon.setImageTintList(null);
                Glide.with(CarPinnedActivity.this).load(entry.image).centerCrop().into(holder.icon);
            } else {
                Glide.with(CarPinnedActivity.this).clear(holder.icon);
                holder.icon.setImageResource(entry.manual
                        ? R.drawable.ic_playlist_kind_manual : R.drawable.ic_playlist_kind_smart);
                holder.icon.setImageTintList(holder.title.getTextColors());
            }
            holder.remove.setOnClickListener(v -> {
                int index = holder.getBindingAdapterPosition();
                if (index != RecyclerView.NO_POSITION) {
                    pinned.remove(index);
                    notifyItemRemoved(index);
                    save();
                }
            });
            holder.dragHandle.setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    touchHelper.startDrag(holder);
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return pinned.size();
        }
    }

    private static class Holder extends RecyclerView.ViewHolder {
        final ImageView dragHandle;
        final ImageView icon;
        final TextView title;
        final ImageButton remove;

        Holder(@NonNull View itemView) {
            super(itemView);
            dragHandle = itemView.findViewById(R.id.dragHandle);
            icon = itemView.findViewById(R.id.icon);
            title = itemView.findViewById(R.id.title);
            remove = itemView.findViewById(R.id.remove);
        }
    }
}
