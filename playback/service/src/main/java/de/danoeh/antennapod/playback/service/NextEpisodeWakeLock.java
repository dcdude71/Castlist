package de.danoeh.antennapod.playback.service;

import android.content.Context;
import android.os.PowerManager;

import androidx.annotation.NonNull;

final class NextEpisodeWakeLock {
    private static final long TIMEOUT_MS = 60_000;

    private final PowerManager.WakeLock wakeLock;

    private NextEpisodeWakeLock(@NonNull Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Castlist:NextEpisode");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(TIMEOUT_MS);
    }

    @NonNull
    static NextEpisodeWakeLock acquire(@NonNull Context context) {
        return new NextEpisodeWakeLock(context);
    }

    void release() {
        if (wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
