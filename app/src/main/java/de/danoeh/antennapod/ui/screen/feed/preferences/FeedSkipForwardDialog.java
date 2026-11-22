package de.danoeh.antennapod.ui.screen.feed.preferences;

import android.content.Context;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.storage.database.FeedSkipForward;
import de.danoeh.antennapod.storage.preferences.UserPreferences;
import de.danoeh.antennapod.ui.common.databinding.EditTextDialogBinding;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/** Edits a podcast's own skip forward time (Castlist). Empty or 0 means "use the global setting". */
public final class FeedSkipForwardDialog {
    private static final String TAG = "FeedSkipForwardDialog";
    private static final int MAX_SECONDS = 3600;

    public interface Callback {
        void onSecondsChanged(int seconds);
    }

    private FeedSkipForwardDialog() {
    }

    public static void show(Context context, long feedId, int currentSeconds, Callback callback) {
        EditTextDialogBinding binding = EditTextDialogBinding.inflate(LayoutInflater.from(context));
        binding.textInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        binding.textInputLayout.setSuffixText(context.getString(R.string.time_seconds));
        binding.textInputLayout.setHint(context.getString(R.string.feed_skip_forward_hint,
                UserPreferences.getFastForwardSecs()));
        if (currentSeconds > 0) {
            binding.textInput.setText(String.valueOf(currentSeconds));
            binding.textInput.selectAll();
        }
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.feed_skip_forward)
                .setMessage(R.string.feed_skip_forward_message)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.confirm_label, (dialog, which) ->
                        save(feedId, String.valueOf(binding.textInput.getText()), callback))
                .setNeutralButton(R.string.feed_skip_forward_use_global, (dialog, which) ->
                        save(feedId, "0", callback))
                .setNegativeButton(R.string.cancel_label, null)
                .show();
    }

    private static void save(long feedId, String text, Callback callback) {
        int seconds;
        try {
            seconds = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            seconds = 0;
        }
        final int newSeconds = Math.max(0, Math.min(MAX_SECONDS, seconds));
        Completable.fromAction(() -> FeedSkipForward.setSeconds(feedId, newSeconds))
                .subscribeOn(Schedulers.io())
                .subscribe(() -> { }, error -> Log.e(TAG, Log.getStackTraceString(error)));
        callback.onSecondsChanged(newSeconds);
    }
}
