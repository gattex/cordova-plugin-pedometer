package de.antwerpes.cordova.pedometer;

import org.json.JSONObject;

public interface StepBroadcastListener {
    public void onStep(JSONObject dataset);
}
