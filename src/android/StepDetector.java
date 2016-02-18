package de.antwerpes.cordova.pedometer;

import java.util.ArrayList;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Detects steps and notifies all listeners (that implement StepListener).
 *
 */
public class StepDetector implements SensorEventListener {
    private final static String TAG = "StepDetector";
    // Arrays holding the samples and derivates
    private ArrayList<Double> accSamples = new ArrayList<Double>();
    private ArrayList<Double> samplesAverages = new ArrayList<Double>();
    private ArrayList<Double> samplesDerivates = new ArrayList<Double>();
    private ArrayList<Double> samplesDerivatesAverages = new ArrayList<Double>();
    // Step listeners implementing the onStep interface/callback
    private ArrayList<StepListener> mStepListeners = new ArrayList<StepListener>();

    private int mSteps = 0;
    private int mSampleCount = 16;
    long lastStepTimestamp = 0;
    boolean thresholdPassed = false;
    private int stepCache = 0;

    public StepDetector() {
    }

    public void addStepListener(StepListener sl) {
        mStepListeners.add(sl);
    }

    //public void onSensorChanged(int sensor, float[] values) {
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        synchronized (this) {
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    double magnitude = Math.sqrt(Math.pow(event.values[0], 2) + Math.pow(event.values[1], 2) + Math.pow(event.values[2], 2)) / SensorManager.STANDARD_GRAVITY;
                    addAcc(magnitude);
                }
            }
    }

    private void addAcc (double acc){
        accSamples.add(acc);
        if(accSamples.size() > mSampleCount){
            double avg = average(accSamples);
           // Log.wtf("accSamples", String.valueOf(avg));
            addAvg(avg);
            accSamples.remove(0);
        }
    }

    private void addAvg(double avg){
        samplesAverages.add(avg);
        if(samplesAverages.size() >= 2){
            double derivate = samplesAverages.get(samplesAverages.size()-1) - samplesAverages.get(samplesAverages.size()-2);
           // Log.wtf("derivateCalc", String.valueOf(derivate));
            addDerivate(derivate);
            samplesAverages.remove(0);
        }
    }

    private void addDerivate(double dervivate){
        samplesDerivates.add(dervivate);
        if(samplesDerivates.size() > mSampleCount){
            double derivate = average(samplesDerivates);
           // Log.wtf("derivateavg", String.valueOf(derivate));
            addDerivateAverage(derivate);
            samplesDerivates.remove(0);
        }
    }

    private void addDerivateAverage(double derivateAvg){
        samplesDerivatesAverages.add(derivateAvg);
        if(samplesDerivatesAverages.size() > 3){
          //  Log.wtf("samplesDerivatesAverages", String.valueOf(samplesDerivatesAverages));
            checkStep();
            samplesDerivatesAverages.remove(0);
        }

    }

    private void checkStep(){
        double lastVal = samplesDerivatesAverages.get(samplesDerivatesAverages.size() - 1);
       // String debugString = String.valueOf(lastVal);
        if(thresholdPassed && samplesDerivatesAverages.get(samplesDerivatesAverages.size()-2) > 0 &&  lastVal <= 0){
            long now = System.currentTimeMillis();
            long delta =  now - lastStepTimestamp;
            if(delta < 2850 && delta > 300){
                if(stepCache == -1){
                    notifyStepListeners();
                }else{
                    stepCache++;
                }
                if(stepCache > 3){
                    for (int i = 0; i < stepCache; i++) {
                        notifyStepListeners();
                    }
                    stepCache = -1;
                }
            } else {
                stepCache = 0;
            }
            lastStepTimestamp = now;
        }
        double thresholdCountDerivate = 0.01;
        if(lastVal > thresholdCountDerivate){
            thresholdPassed = true;
        }
        else if(lastVal < 0){
            thresholdPassed = false;
        }
        //Log.wtf("CHECK_STEP", debugString);
    }
    private void notifyStepListeners(){
        mSteps++;
        for (StepListener stepListener : mStepListeners) {
            stepListener.onStep();
        }
    }
    private double average(ArrayList<Double> list){
        double sum = 0;
        for(int i = 0; i < list.size(); i++){
            sum += list.get(i);
        }
        return sum/list.size();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
}