package de.danoeh.antennapod.storage.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;

public class FeedSkipForwardDbAdapter {
    /** Table name kept from the former "ad skip" feature, so existing values carry over. */
    public static final String TABLE_NAME_FEED_SKIP_FORWARD = "FeedAdSkip";

    private static final String KEY_FEED = "feed";
    private static final String KEY_SECONDS = "seconds";

    private static final String CREATE_TABLE_FEED_SKIP_FORWARD = "CREATE TABLE IF NOT EXISTS "
            + TABLE_NAME_FEED_SKIP_FORWARD + " ("
            + KEY_FEED + " INTEGER PRIMARY KEY, "
            + KEY_SECONDS + " INTEGER NOT NULL DEFAULT 0)";

    private final SQLiteDatabase db;

    FeedSkipForwardDbAdapter(SQLiteDatabase db) {
        this.db = db;
    }

    static void createTables(SQLiteDatabase db) {
        FeedSkipForward.invalidate();
        if (db.isReadOnly()) {
            return;
        }
        db.execSQL(CREATE_TABLE_FEED_SKIP_FORWARD);
    }

    Map<Long, Integer> getAll() {
        Map<Long, Integer> result = new HashMap<>();
        try (Cursor cursor = db.query(TABLE_NAME_FEED_SKIP_FORWARD, new String[]{KEY_FEED, KEY_SECONDS},
                KEY_SECONDS + ">0", null, null, null, null)) {
            while (cursor.moveToNext()) {
                result.put(cursor.getLong(0), cursor.getInt(1));
            }
        } catch (RuntimeException e) {
            // Table missing on a read-only database: no overrides.
        }
        return result;
    }

    void setSeconds(long feedId, int seconds) {
        if (seconds <= 0) {
            deleteFeed(feedId);
            return;
        }
        ContentValues values = new ContentValues();
        values.put(KEY_FEED, feedId);
        values.put(KEY_SECONDS, seconds);
        db.insertWithOnConflict(TABLE_NAME_FEED_SKIP_FORWARD, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    void deleteFeed(long feedId) {
        db.delete(TABLE_NAME_FEED_SKIP_FORWARD, KEY_FEED + "=?", new String[]{String.valueOf(feedId)});
        FeedSkipForward.forget(feedId);
    }
}
