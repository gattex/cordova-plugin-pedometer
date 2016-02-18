package de.antwerpes.cordova.pedometer;

import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Iterator;
import java.util.List;

public class Pedometer extends CordovaPlugin implements StepBroadcastListener {
    public static final String TAG = "Pedometer";
    protected Context mContext;
    private StepBroadcastReceiver mBroadCastReciever;
    private int mListenerIndex;
    private CallbackContext stepCallbackContext = null;
    /**
     * Constructor.
     */
    public Pedometer() {

    }

    /**
     * Sets the context of the Command. This can then be used to do things like
     * get file paths associated with the Activity.
     *
     * @param cordova The context of the main Activity.
     * @param webView The CordovaWebView Cordova is running in.
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        mContext = this.cordova.getActivity().getApplicationContext();
        mBroadCastReciever = StepBroadcastReceiver.getInstance(mContext);
        mListenerIndex = mBroadCastReciever.addStepListener(this);
    }

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        if (action.equals("startPedometerUpdates")) {
            if(!isServiceRunning()){
                this.cordova.getActivity().startService(getServiceIntend());
            }
            if (this.stepCallbackContext != null) {
                callbackContext.error( "Battery listener already running.");
                return true;
            }
            this.stepCallbackContext = callbackContext;

            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            pluginResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResult);
            return true;

        }
        else if(action.equals("stop")){
            this.cordova.getActivity().stopService(getServiceIntend());
            this.sendUpdate(new JSONObject(), false);
            this.stepCallbackContext = null;
            callbackContext.success();
        }
        else if(action.equals("isRunning")){
            if(isServiceRunning()){
                callbackContext.success();
            }else{
                callbackContext.error("Service Not running");
            }
        }else if(action.equals("queryData")){
            final JSONObject options = args.getJSONObject(0);
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    JSONArray steps = null;
                    try {
                        steps = mBroadCastReciever.queryData(options.getString("startDate"), options.getString("endDate"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    callbackContext.success(steps);
                }
            });
        }else if(action.equals("getStepsForToday")){
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    JSONObject steps = mBroadCastReciever.getStepsForToday();
                    if (steps != null) {
                        callbackContext.success(steps);
                    } else {
                        callbackContext.error("no steps yet");
                    }
                }
            });

        }
        else {
          callbackContext.error("Error");
            return false;
        }
        return true;
    }

    private Intent getServiceIntend(){
        Intent i = new Intent(this.cordova.getActivity().getApplicationContext(), PedometerService.class);
        i.setAction(PedometerService.ACTION_START_SERVICE);
        return i;
    }

    private boolean isServiceRunning(){
        String serviceName = "de.antwerpes.cordova.pedometer.PedometerService";
        boolean serviceRunning = false;
        ActivityManager am = (ActivityManager) this.cordova.getActivity().getSystemService(this.cordova.getActivity().ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> l = am.getRunningServices(5000);
        Iterator<ActivityManager.RunningServiceInfo> i = l.iterator();
        while (!serviceRunning && i.hasNext()) {
            ActivityManager.RunningServiceInfo runningServiceInfo = i.next();
            if(runningServiceInfo.service.getClassName().equals(serviceName)){
                serviceRunning = true;
            }
        }
        return serviceRunning;
    }

    /**
     * Stop battery receiver.
     */
    public void onDestroy() {
        mBroadCastReciever.unregisterListener(mListenerIndex);
    }


    /**
     * Stop battery receiver.
     */
    public void onReset() {
        mBroadCastReciever.unregisterListener(mListenerIndex);
    }

    private void sendUpdate(JSONObject info, boolean keepCallback) {
        if (this.stepCallbackContext != null) {
            PluginResult result = new PluginResult(PluginResult.Status.OK, info);
            result.setKeepCallback(keepCallback);
            this.stepCallbackContext.sendPluginResult(result);
        }
    }

    @Override
    public void onStep(JSONObject dataset) {
        sendUpdate(dataset, true);
    }
}