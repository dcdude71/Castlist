package de.danoeh.antennapod.storage.importexport;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import de.danoeh.antennapod.storage.database.EpisodeFilterDbAdapter;
import de.danoeh.antennapod.storage.database.FeedSkipForwardDbAdapter;
import de.danoeh.antennapod.storage.database.PlaylistDbAdapter;

public final class OfficialAppExporter {
    private static final String TEMP_FILE_NAME = "official-app-export.db";
    private static final String[] FORK_TABLES = {
        PlaylistDbAdapter.TABLE_NAME_PLAYLIST_RULES,
        PlaylistDbAdapter.TABLE_NAME_PLAYLISTS,
        PlaylistDbAdapter.TABLE_NAME_PLAYLIST_DISMISSED,
        PlaylistDbAdapter.TABLE_NAME_PLAYLIST_EPISODES,
        FeedSkipForwardDbAdapter.TABLE_NAME_FEED_SKIP_FORWARD,
        EpisodeFilterDbAdapter.TABLE_NAME_FEED_EPISODE_FILTERS,
        EpisodeFilterDbAdapter.TABLE_NAME_EPISODE_FILTER_CONDITIONS
    };

    private OfficialAppExporter() {
    }

    public static void exportToDocument(@NonNull Uri uri, @NonNull Context context) throws IOException {
        File tempDb = new File(context.getCacheDir(), TEMP_FILE_NAME);
        try {
            try (FileOutputStream out = new FileOutputStream(tempDb)) {
                DatabaseExporter.exportToStream(out, context);
            }
            removeForkTables(tempDb);
            try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "wt");
                    FileOutputStream out = new FileOutputStream(pfd.getFileDescriptor())) {
                FileUtils.copyFile(tempDb, out);
            }
        } finally {
            deleteWithSidecars(tempDb);
        }
    }

    static void removeForkTables(@NonNull File databaseFile) {
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(databaseFile.getAbsolutePath(), null,
                SQLiteDatabase.OPEN_READWRITE)) {
            moveManualPlaylistsToQueue(db);
            for (String table : FORK_TABLES) {
                db.execSQL("DROP TABLE IF EXISTS " + table);
            }
            db.execSQL("VACUUM");
        }
    }

    private static void moveManualPlaylistsToQueue(@NonNull SQLiteDatabase db) {
        try (Cursor table = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{PlaylistDbAdapter.TABLE_NAME_PLAYLIST_EPISODES})) {
            if (!table.moveToFirst()) {
                return;
            }
        }
        Map<Long, Long> episodes = new LinkedHashMap<>();
        try (Cursor cursor = db.rawQuery("SELECT e.item, i.feed FROM PlaylistEpisodes e"
                + " JOIN Playlists p ON p.id = e.playlist"
                + " JOIN FeedItems i ON i.id = e.item"
                + " WHERE p.manual = 1 AND i.read != 1"
                + " AND e.item NOT IN (SELECT feeditem FROM Queue)"
                + " ORDER BY p.position, p.id, e.position", null)) {
            while (cursor.moveToNext()) {
                if (!episodes.containsKey(cursor.getLong(0))) {
                    episodes.put(cursor.getLong(0), cursor.getLong(1));
                }
            }
        }
        long nextId = 1;
        try (Cursor cursor = db.rawQuery("SELECT MAX(id) FROM Queue", null)) {
            if (cursor.moveToFirst() && !cursor.isNull(0)) {
                nextId = cursor.getLong(0) + 1;
            }
        }
        for (Map.Entry<Long, Long> episode : episodes.entrySet()) {
            ContentValues values = new ContentValues();
            values.put("id", nextId++);
            values.put("feeditem", episode.getKey());
            values.put("feed", episode.getValue());
            db.insert("Queue", null, values);
        }
    }

    private static void deleteWithSidecars(@NonNull File databaseFile) {
        FileUtils.deleteQuietly(databaseFile);
        for (String suffix : new String[]{"-wal", "-shm", "-journal"}) {
            FileUtils.deleteQuietly(new File(databaseFile.getAbsolutePath() + suffix));
        }
    }
}
