package de.danoeh.antennapod.ui.screen.car;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public final class CarPlayerPreferences {
    public static final String PREF_ENABLED = "prefCarPlayer";
    public static final String PREF_NEXT_BUTTON = "prefCarNextButton";
    public static final String PREF_KEEP_SCREEN_ON = "prefCarKeepScreenOn";
    public static final String NEXT_EPISODE = "episode";
    public static final String NEXT_CHAPTER = "chapter";
    public static final String NEXT_BOTH = "both";

    private CarPlayerPreferences() {
    }

    public static boolean isEnabled(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_ENABLED, true);
    }

    @NonNull
    public static String getNextButton(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(PREF_NEXT_BUTTON, NEXT_EPISODE);
    }

    public static boolean isKeepScreenOn(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_KEEP_SCREEN_ON, true);
    }
}
