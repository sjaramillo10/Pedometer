package dev.sjaramillo.nemi.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public final class NemiDatabase extends SQLiteOpenHelper {
    public NemiDatabase(Context context) {
        super(context, "nemi-db", null, 1);
        setWriteAheadLoggingEnabled(true);
    }

    @Override public void onCreate(SQLiteDatabase database) {
        database.execSQL(StepQueries.CREATE_DAILY_STEPS);
    }

    @Override public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        throw new IllegalStateException(
                "Unsupported Nemi database upgrade from " + oldVersion + " to " + newVersion);
    }
}
