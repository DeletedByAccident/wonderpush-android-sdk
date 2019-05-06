package com.wonderpush.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class ConfigurationChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {
            Log.i(WonderPush.TAG, getClass().getSimpleName() + ".onReceive() intent: " + intent);
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Log.i(WonderPush.TAG, getClass().getSimpleName() + ".onReceive() extras: " + extras);
                for (String key : extras.keySet()) {
                    Log.i(WonderPush.TAG, getClass().getSimpleName() + ".onReceive() extra " + key + ": " + extras.get(key));
                }
            }

            // Initialize the SDK, it will take care of refreshing relevant properties
            WonderPush.ensureInitialized(context);
        } catch (Exception ex) {
            Log.e(WonderPush.TAG, "Unexpected error while handling onReceive(" + intent + ")", ex);
        }
    }

}
