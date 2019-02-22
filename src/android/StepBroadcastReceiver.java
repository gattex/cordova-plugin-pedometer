package de.antwerpes.cordova.pedometer;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.support.v4.content.WakefulBroadcastReceiver;



import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;


public class StepBroadcastReceiver extends WakefulBroadcastReceiver {

    JSONArray mAchievements;
    Context mContext;
    private Calendar gregorian;
    private static StepBroadcastReceiver instance;
    private static JSONObject achivementsJson;
    private static StepDbHelper mDBHelper;
    private ArrayList<StepBroadcastListener> mStepListeners = new ArrayList<StepBroadcastListener>();

    public StepBroadcastReceiver(Context context) {
        mContext = context;
        mDBHelper = new StepDbHelper(context);
        gregorian = new GregorianCalendar();
        StepBroadcastReceiver.instance = this;
    }

    public static StepBroadcastReceiver getInstance(Context context) {
        if(StepBroadcastReceiver.instance == null){
            StepBroadcastReceiver.instance = new StepBroadcastReceiver(context);
        }
        return StepBroadcastReceiver.instance;
    }

    private JSONArray cursorToJSON(Cursor crs) throws JSONException {
        JSONArray arr = new JSONArray();
        crs.moveToFirst();
        while (!crs.isAfterLast()) {
            int nColumns = crs.getColumnCount();
            JSONObject row = new JSONObject();
            for (int i = 0 ; i < nColumns ; i++) {
                String colName = crs.getColumnName(i);
                if (colName != null) {
                    switch (crs.getType(i)) {
                        case Cursor.FIELD_TYPE_FLOAT:
                            row.put(colName, crs.getDouble(i));
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            row.put(colName, crs.getLong(i));
                            break;
                        case Cursor.FIELD_TYPE_NULL:
                            row.put(colName, null);
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            row.put(colName, crs.getString(i));
                            break;
                    }
                }
            }
            arr.put(row);
            if (!crs.moveToNext())
                break;
        }
        return arr;
    }

    private JSONObject cursorToJSONObject(Cursor crs) throws JSONException {

        crs.moveToFirst();

        int nColumns = crs.getColumnCount();
        JSONObject row = new JSONObject();
        for (int i = 0 ; i < nColumns ; i++) {
            String colName = crs.getColumnName(i);
            if (colName != null) {
                switch (crs.getType(i)) {
                    case Cursor.FIELD_TYPE_FLOAT:
                        row.put(colName, crs.getDouble(i));
                        break;
                    case Cursor.FIELD_TYPE_INTEGER:
                        row.put(colName, crs.getLong(i));
                        break;
                    case Cursor.FIELD_TYPE_NULL:
                        row.put(colName, null);
                        break;
                    case Cursor.FIELD_TYPE_STRING:
                        row.put(colName, crs.getString(i));
                        break;
                }
            }
        }

        return row;
    }


    public JSONArray getSteps() {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();

        Cursor c = db.rawQuery("SELECT * FROM " + StepDbHelper.STEPS_TABLE_NAME, null);
        JSONArray result = null;
        try {
            result = cursorToJSON(c);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        c.close();
        return result;
    }

    public JSONArray queryData(String startDate, String endDate) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        String query = "SELECT * FROM " + StepDbHelper.STEPS_TABLE_NAME + " WHERE DATE("+ StepDbHelper.COLUMN_DAY + ") BETWEEN ? AND ?";
        String[] params = new String[] {startDate, endDate};

        Cursor c = db.rawQuery(query, params);
        JSONArray result = null;
        try {
            result = cursorToJSON(c);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (result == null) {
            result = new JSONArray();
        }
        c.close();
        return result;
    }

    public JSONObject getStepsForToday(){
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date now = new Date();
        String todayDateString = dateFormat.format(now);
        String query = "SELECT * FROM " + StepDbHelper.STEPS_TABLE_NAME + " WHERE " + StepDbHelper.COLUMN_DAY + " = ?";
        String[] params = new String[] {todayDateString};
        Cursor c = db.rawQuery(query, params);
        if (c.getCount() == 0) {
            c.close();
            return null;
        }

        JSONObject result = null;
        try {
            result = cursorToJSONObject(c);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        c.close();
        return result;
    }


    private String getFieldNameCurrentTime() {
        int hour = gregorian.get(Calendar.HOUR_OF_DAY);
        if (hour <= 4) {
            return "f1";
        } else if (hour > 4 && hour <= 8) {
            return "f2";
        } else if (hour > 8 && hour <= 12) {
            return "f3";
        } else if (hour > 12 && hour <= 16) {
            return "f4";
        } else if (hour > 16 && hour <= 20) {
            return "f5";
        } else {
            return "f6";
        }
    }

    @SuppressLint("CommitPrefEdits")
    @Override
    public void onReceive(Context context, Intent intent) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date now = new Date();
        String todayDateString = dateFormat.format(now);
        String field = getFieldNameCurrentTime();

        JSONObject todaySteps = getStepsForToday();

        if (todaySteps == null) {
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(field, 1);
            values.put(StepDbHelper.COLUMN_DAY, todayDateString);
            db.insert(StepDbHelper.STEPS_TABLE_NAME, "null", values);

            todaySteps = getStepsForToday();
        } else {
            try {
                int fieldValue = todaySteps.getInt(field);
                SQLiteDatabase db = mDBHelper.getReadableDatabase();
                ContentValues values = new ContentValues();
                values.put(field, fieldValue + 1);
                String where = StepDbHelper.COLUMN_DAY + " = ?";
                String[] whereArgs = new String[] {todayDateString};
                db.update(
                        StepDbHelper.STEPS_TABLE_NAME,
                        values,
                        where,
                        whereArgs
                );
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        for (StepBroadcastListener stepListener : mStepListeners) {
            stepListener.onStep(todaySteps);
        }
    }

    private int getStepsTotal(){
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT "+
                "Sum("+StepDbHelper.COLUMN_DAY_STEPS_1+") + Sum("+StepDbHelper.COLUMN_DAY_STEPS_2+") + "+StepDbHelper.COLUMN_DAY_STEPS_3+" + Sum("+StepDbHelper.COLUMN_DAY_STEPS_4+") + Sum("+StepDbHelper.COLUMN_DAY_STEPS_5+") + Sum("+StepDbHelper.COLUMN_DAY_STEPS_6+") AS totalSteps FROM " + StepDbHelper.STEPS_TABLE_NAME, null);
        c.moveToFirst();
        if (c.getCount() < 1) {
            c.close();
            return 0;
        }

        int total = c.getInt(c.getColumnIndex("totalSteps"));

        c.close();
        return total;
    }

    public int addStepListener(StepBroadcastListener sl) {
        int index = mStepListeners.size();
        mStepListeners.add(sl);
        return index;
    }
    public void unregisterListener(int index) {
        mStepListeners.remove(index);
    }

}