package de.danoeh.antennapod.ui.screen.playback.audio;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.List;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.databinding.CoverFragmentBinding;
import de.danoeh.antennapod.model.feed.FeedItem;
import de.danoeh.antennapod.model.playlist.Playlist;
import de.danoeh.antennapod.storage.database.PlaylistPlayback;
import de.danoeh.antennapod.storage.database.PlaylistReader;
import de.danoeh.antennapod.ui.common.PlaylistTime;
import de.danoeh.antennapod.ui.common.ThemeUtils;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.schedulers.Schedulers;

final class PlaylistLine {
    private static final String TAG = "PlaylistLine";

    private PlaylistLine() {
    }

    static void update(@NonNull CoverFragmentBinding binding) {
        TextView line = getOrCreate(binding);
        Context context = line.getContext().getApplicationContext();
        long playlistId = PlaylistPlayback.getActivePlaylist(context);
        if (playlistId == PlaylistPlayback.NO_PLAYLIST) {
            line.setVisibility(View.GONE);
            return;
        }
        Maybe.<String>create(emitter -> {
            Playlist playlist = PlaylistReader.getPlaylist(playlistId);
            if (playlist == null) {
                emitter.onComplete();
                return;
            }
            List<FeedItem> episodes = PlaylistReader.getEpisodes(playlistId);
            long timeLeft = PlaylistReader.getTimeLeftMillis(episodes);
            emitter.onSuccess(timeLeft > 0 ? context.getString(R.string.playlist_summary, playlist.getName(),
                    context.getString(R.string.playlist_time_left,
                            PlaylistTime.format(context, timeLeft))) : playlist.getName());
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(text -> {
                    line.setText(text);
                    line.setVisibility(View.VISIBLE);
                }, error -> Log.e(TAG, Log.getStackTraceString(error)), () -> line.setVisibility(View.GONE));
    }

    @NonNull
    private static TextView getOrCreate(@NonNull CoverFragmentBinding binding) {
        LinearLayout container = binding.coverFragmentTextContainer;
        TextView line = container.findViewById(R.id.playlistLine);
        if (line != null) {
            return line;
        }
        Context context = container.getContext();
        line = new TextView(context);
        line.setId(R.id.playlistLine);
        line.setGravity(binding.txtvPodcastTitle.getGravity());
        line.setTextSize(TypedValue.COMPLEX_UNIT_PX, binding.txtvPodcastTitle.getTextSize());
        PlayerBackground.Colors colors = PlayerBackground.currentColors();
        line.setTextColor(colors != null && colors.accent != null ? colors.accent
                : ThemeUtils.getColorFromAttr(context, androidx.appcompat.R.attr.colorPrimary));
        line.setSingleLine(true);
        line.setEllipsize(TextUtils.TruncateAt.END);
        line.setVisibility(View.GONE);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int index = container.indexOfChild(binding.txtvPodcastTitle) + 1;
        container.addView(line, index, params);
        return line;
    }
}
