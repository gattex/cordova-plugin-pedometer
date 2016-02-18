package de.antwerpes.cordova.pedometer;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by marclutz on 17.02.16.
 */
public class StepDbHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "steps.db";
    public static final String STEPS_TABLE_NAME = "steps";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_DAY = "day";
    public static final String COLUMN_DAY_STEPS_1 = "f1";
    public static final String COLUMN_DAY_STEPS_2 = "f2";
    public static final String COLUMN_DAY_STEPS_3 = "f3";
    public static final String COLUMN_DAY_STEPS_4 = "f4";
    public static final String COLUMN_DAY_STEPS_5 = "f5";
    public static final String COLUMN_DAY_STEPS_6 = "f6";
    private static final String DEFAULT_ZERO = " DEFAULT 0";
    private static final String TEXT_TYPE = " TEXT";
    private static final String INT_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + STEPS_TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY," +
                    COLUMN_DAY + TEXT_TYPE + COMMA_SEP +
                    COLUMN_DAY_STEPS_1 + INT_TYPE + DEFAULT_ZERO + COMMA_SEP +
                    COLUMN_DAY_STEPS_2 + INT_TYPE  + DEFAULT_ZERO + COMMA_SEP +
                    COLUMN_DAY_STEPS_3 + INT_TYPE  + DEFAULT_ZERO + COMMA_SEP +
                    COLUMN_DAY_STEPS_4 + INT_TYPE  + DEFAULT_ZERO + COMMA_SEP +
                    COLUMN_DAY_STEPS_5 + INT_TYPE  + DEFAULT_ZERO + COMMA_SEP +
                    COLUMN_DAY_STEPS_6 + INT_TYPE  + DEFAULT_ZERO +
            " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + STEPS_TABLE_NAME;


    public StepDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Migrate
    }
}
