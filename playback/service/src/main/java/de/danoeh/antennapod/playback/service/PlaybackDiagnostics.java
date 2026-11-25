package de.danoeh.antennapod.playback.service;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class PlaybackDiagnostics {
    private static final String TAG = "PlaybackDiagnostics";
    private static final String FILE_NAME = "playback-log.txt";
    private static final long MAX_SIZE = 256 * 1024;
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    private PlaybackDiagnostics() {
    }

    static void log(@NonNull Context context, @NonNull String message) {
        log(context, message, null);
    }

    static void log(@NonNull Context context, @NonNull String message, @Nullable Throwable error) {
        Log.w(TAG, message, error);
        Context appContext = context.getApplicationContext();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(new Date());
        EXECUTOR.execute(() -> write(appContext, time + " " + message, error));
    }

    private static void write(@NonNull Context context, @NonNull String line, @Nullable Throwable error) {
        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            return;
        }
        File file = new File(dir, FILE_NAME);
        try {
            if (file.length() > MAX_SIZE) {
                keepSecondHalf(file);
            }
            try (PrintWriter writer = new PrintWriter(new FileWriter(file, true))) {
                writer.println(line);
                if (error != null) {
                    error.printStackTrace(writer);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not write playback log", e);
        }
    }

    private static void keepSecondHalf(@NonNull File file) throws IOException {
        byte[] tail;
        try (RandomAccessFile in = new RandomAccessFile(file, "r")) {
            long start = in.length() / 2;
            tail = new byte[(int) (in.length() - start)];
            in.seek(start);
            in.readFully(tail);
        }
        try (RandomAccessFile out = new RandomAccessFile(file, "rw")) {
            out.setLength(0);
            out.write(tail);
        }
    }
}
