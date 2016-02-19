package de.antwerpes.cordova.pedometer;

import org.json.JSONObject;

public interface StepBroadcastListener {
    void onStep(JSONObject dataset);
}
