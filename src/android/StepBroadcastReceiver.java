package de.antwerpes.cordova.pedometer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import de.antwerpes.time2move.MainActivity;
import de.antwerpes.time2move.R;

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
        try {
            mAchievements = getAchievements(context).getJSONArray("achievements");
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

        Cursor c = db.rawQuery("SELECT * FROM " + StepDbHelper.STEPS_TABLE_NAME + " WHERE 1", null);
        JSONArray result = null;
        try {
            result = cursorToJSON(c);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        c.close();
        db.close();
        return result;
    }

    public JSONArray queryData(String startDate, String endDate) {
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + StepDbHelper.STEPS_TABLE_NAME + " WHERE DATE("+ StepDbHelper.COLUMN_DAY + ") BETWEEN DATE(?) AND DATE(?)", new String[] {startDate, endDate});
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
        db.close();
        return result;
    }

    public JSONObject getStepsForToday(){
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date now = new Date();
        String todayDateString = dateFormat.format(now);
        Cursor c = db.rawQuery("SELECT * FROM " + StepDbHelper.STEPS_TABLE_NAME + " WHERE " + StepDbHelper.COLUMN_DAY + " = ?", new String[] {todayDateString});
        if (c.getCount() == 0) {
            c.close();
            db.close();
            return null;
        }

        JSONObject result = null;
        try {
            result = cursorToJSONObject(c);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        c.close();
        db.close();
        return result;
    }

    public JSONObject getAchievements(Context context) throws IOException, JSONException{
        if (achivementsJson != null) {
            return achivementsJson;
        }

        InputStream is = context.getAssets().open("www/assets/achievements.json");

        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line;
        String achievementsString = "";

        while ((line = br.readLine()) != null) {
            achievementsString += line;
        }

        br.close();

        achivementsJson = new JSONObject(achievementsString);

        return achivementsJson;
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
        //String filename = "steps.json";
        //String fullFilename = context.getFilesDir() + "/" + filename; //file:///data/data/pe.pedo.pedo/files/"

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date now = new Date();
        String todayDateString = dateFormat.format(now);
        String field = getFieldNameCurrentTime();

        JSONObject todaySteps = getStepsForToday();
        for (StepBroadcastListener stepListener : mStepListeners) {
            stepListener.onStep(todaySteps);
        }

        if (todaySteps == null) {
            SQLiteDatabase db = mDBHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(field, 1);
            db.insert(StepDbHelper.STEPS_TABLE_NAME, "null", values);
        } else {
            try {
                JSONObject todayObj = getStepsForToday();
                int fieldValue = todayObj != null ? todayObj.getInt(field) : 0;
                SQLiteDatabase db = mDBHelper.getReadableDatabase();
                ContentValues values = new ContentValues();
                values.put(StepDbHelper.COLUMN_DAY, todayDateString);
                values.put(field, fieldValue + 1);
                db.insert(
                        StepDbHelper.STEPS_TABLE_NAME,
                        "null",
                        values
                );
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JSONArray achieved = null;
        try {
            achieved = checkAchivement();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        SharedPreferences preferences = context.getSharedPreferences("MyPreferences", Context.MODE_PRIVATE);
        int lastAchievedIndex = preferences.getInt("var1", 0);
        if (achieved != null && achieved.length() > 0 && lastAchievedIndex < achieved.length()){
            JSONObject  achivementDescription = null;
            try {
                achivementDescription = (JSONObject) achieved.get(achieved.length() - 1);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Intent notificationIntent = new Intent(mContext, MainActivity.class);
            PendingIntent contentIntent = PendingIntent.getActivity(mContext,
                    334, notificationIntent,
                    PendingIntent.FLAG_ONE_SHOT);
            String achievementDescription = null;
            try {
                assert achivementDescription != null;
                achievementDescription = achivementDescription.getString("name");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Notification note =
                    new NotificationCompat.Builder(mContext)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setColor(Color.argb(1, 208, 26, 26))
                            .setContentTitle("New Milestone")
                            .setContentText("You´ve reached a new milestone " + achievementDescription)
                            .setPriority(Notification.PRIORITY_HIGH)
                            .setContentIntent(contentIntent)
                            .setOngoing(false)
                            .build();
            note.flags |= Notification.FLAG_AUTO_CANCEL;

            NotificationManager mNotificationManager =
                    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(334, note);

            int achievedIndex = achieved.length();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt("var1", achievedIndex);
            editor.commit();
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(333);
        }
    }

    private int getStepsTotal(){
        SQLiteDatabase db = mDBHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT "+
                "Sum("+StepDbHelper.COLUMN_DAY_STEPS_1+") + Sum("+StepDbHelper.COLUMN_DAY_STEPS_2+") + "+StepDbHelper.COLUMN_DAY_STEPS_3+" + Sum("+StepDbHelper.COLUMN_DAY_STEPS_4+") + Sum("+StepDbHelper.COLUMN_DAY_STEPS_5+") + Sum("+StepDbHelper.COLUMN_DAY_STEPS_3+") AS totalSteps FROM " + StepDbHelper.STEPS_TABLE_NAME, null);
        c.moveToFirst();
        if (c.getCount() < 1) {
            c.close();
            db.close();
            return 0;
        }

        int total = c.getInt(c.getColumnIndex("totalSteps"));

        c.close();
        db.close();
        return total;
    }

    private JSONArray checkAchivement() throws JSONException {
        JSONArray achieved = new JSONArray();
        int steps = getStepsTotal();
        for (int i = 0; i < mAchievements.length(); i++) {
            JSONObject achivementDescription = mAchievements.getJSONObject(i);

            if(steps >= achivementDescription.getInt("steps") ){
                achieved.put(achivementDescription);
            }
        }
        return achieved;
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