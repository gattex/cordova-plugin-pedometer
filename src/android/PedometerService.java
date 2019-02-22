package de.antwerpes.cordova.pedometer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorManager;


import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


public class PedometerService extends Service implements StepListener {
    public static final String ACTION_STOP_SERVICE = "stop";
    public static final String ACTION_START_SERVICE = "start";
    PowerManager mPowerManager = null;
    PowerManager.WakeLock mWakeLock = null;

    private StepDetector mStepDetector;
    private SensorManager mSensorManager;
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
        if (null == intent || null == intent.getAction ()) {
            return START_STICKY;
        }
        if (PedometerService.ACTION_STOP_SERVICE.equals(intent.getAction())) {
            Log.d("PEDOM","called to cancel service");
            stopSelf();
            return START_STICKY_COMPATIBILITY;
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

    }

    public void registerDetector() {
        Sensor mSensor = mSensorManager.getDefaultSensor(
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
