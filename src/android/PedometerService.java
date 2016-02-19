package de.antwerpes.cordova.pedometer;

import android.app.PendingIntent;
import android.app.Service;
import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;


import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import de.antwerpes.time2move.MainActivity;
import de.antwerpes.time2move.R;


public class PedometerService extends Service implements StepListener {
    public static final String ACTION_STOP_SERVICE = "stop";
    public static final String ACTION_START_SERVICE = "start";
    PowerManager mPowerManager = null;
    PowerManager.WakeLock mWakeLock = null;

    private StepDetector mStepDetector;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private StepBroadcastReceiver mStepBroadcastReceiver;

    public PedometerService() {
        super();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mStepDetector = new StepDetector();
        mStepDetector.addStepListener(this);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mStepBroadcastReceiver =
                StepBroadcastReceiver.getInstance(getApplicationContext());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Called when the Service is started
        if (PedometerService.ACTION_STOP_SERVICE.equals(intent.getAction())) {
            Log.d("PEDOM","called to cancel service");
            stopSelf();

            return 0;
        } else if (PedometerService.ACTION_START_SERVICE.equals(intent.getAction())) {
            start();
            Log.wtf("PEDOM", "onStart Command");
            if( mPowerManager == null )
            {
                mPowerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
            }

            if( mWakeLock == null )
            {
                mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "de.antwerpes.cordova.pedometer.wakelock");
                mWakeLock.acquire();
            }

            // We want this service to continue running until it is explicitly
            // stopped, so return sticky.
            return START_STICKY;
        }
        return START_NOT_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterDetector();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mStepBroadcastReceiver);

        if( mWakeLock != null) {
            mWakeLock.release();
            mWakeLock = null;
        }

        stopForeground( true );

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStep() {
        Intent localIntent =
                new Intent("de.antwerpes.cordova.pedometer.broadcast");

      /*  Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(100);*/

        // Broadcasts the Intent to receivers in this app.
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
    }

    private void start (){
        // Init the step detector
        // Register the Step Detector
        IntentFilter mStatusIntentFilter = new IntentFilter(
                "de.antwerpes.cordova.pedometer.broadcast");



        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                mStepBroadcastReceiver,
                mStatusIntentFilter);
        registerDetector();

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(getApplicationContext(),
                333, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);


        Intent playPauseIntent = new Intent(getApplicationContext(), PedometerService.class);
        playPauseIntent.setAction(ACTION_STOP_SERVICE);
        PendingIntent playPausePendingIntent = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_CANCEL_CURRENT);

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Time2Move")
                .setContentText("is tracking Your Steps")
                .setPriority(Notification.PRIORITY_HIGH)
                .addAction(R.drawable.ic_action_remove, "stop", playPausePendingIntent)
                .setContentIntent(contentIntent)
                .setColor(Color.argb(1, 208, 26, 26))
                .setOngoing(true);


        Notification note = mBuilder.build();
        //note.flags|=Notification.FLAG_NO_CLEAR;

        // Start foreground is the key to make android treat the service at the "App" priority level
        // so the Service does not get killed
        startForeground(1338, note);
    }

    public void registerDetector() {
        mSensor = mSensorManager.getDefaultSensor(
                Sensor.TYPE_ACCELEROMETER);
        int refreshRate = 20000;
        mSensorManager.registerListener(mStepDetector,
                mSensor,
                refreshRate);
    }

    private void unregisterDetector() {
        mSensorManager.unregisterListener(mStepDetector);
    }

}
