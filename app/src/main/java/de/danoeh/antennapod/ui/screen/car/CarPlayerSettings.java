package de.danoeh.antennapod.ui.screen.car;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import de.danoeh.antennapod.R;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

public final class CarPlayerSettings {
    private static final String TAG = "CarPlayerSettings";
    private static final String PREF_PINNED = "prefCarPinned";
    private static final String PREF_SHORTCUT = "prefCarShortcut";

    private CarPlayerSettings() {
    }

    public static void setup(@NonNull PreferenceFragmentCompat fragment) {
        ListPreference screenOn = fragment.findPreference(CarPlayerPreferences.PREF_SCREEN_ON);
        if (screenOn != null && screenOn.getValue() == null) {
            screenOn.setValue(CarPlayerPreferences.getScreenOn(fragment.requireContext()));
        }
        Preference pinned = fragment.findPreference(PREF_PINNED);
        if (pinned != null) {
            pinned.setOnPreferenceClickListener(preference -> {
                fragment.startActivity(new Intent(fragment.requireContext(), CarPinnedActivity.class));
                return true;
            });
        }
        Preference shortcut = fragment.findPreference(PREF_SHORTCUT);
        if (shortcut != null) {
            shortcut.setOnPreferenceClickListener(preference -> {
                if (!CarPlayerActivity.pinShortcut(fragment.requireContext())) {
                    Toast.makeText(fragment.requireContext(), R.string.playlist_shortcut_unsupported,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            });
        }
    }

    public static void updateSummary(@NonNull PreferenceFragmentCompat fragment) {
        Preference pinned = fragment.findPreference(PREF_PINNED);
        if (pinned == null) {
            return;
        }
        Context context = fragment.requireContext().getApplicationContext();
        Single.fromCallable(() -> {
            CarPins.ensureInitialized(context);
            int playlists = CarPins.getPlaylists(context).size();
            int podcasts = CarPins.getPodcasts(context).size();
            return context.getString(R.string.car_source,
                    context.getResources().getQuantityString(R.plurals.num_playlists, playlists, playlists),
                    context.getResources().getQuantityString(R.plurals.car_num_podcasts, podcasts, podcasts));
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pinned::setSummary, error -> Log.e(TAG, Log.getStackTraceString(error)));
    }
}
