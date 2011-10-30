package com.hackathon.chasingcars;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class MultiplayerService {
    private static final String TAG = "chasingcars:MultiplayerService";
    final static boolean D = true;

    // Static types
    public static MultiplayerService SERVICE;

    // Instance local
    public BluetoothService BTS;

    private android.os.Handler currentHandler = null;

    public static void createService() {
        SERVICE = new MultiplayerService();
    }

    public void initBTS(Activity a) {
        BTS = new BluetoothService(a, SVCHANDLER);
    }

    public final Handler SVCHANDLER = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (currentHandler == null) {
                Log.w(TAG, "handleMessage is not set");
            } else {
                currentHandler.handleMessage(msg);
            }
        }
    };

    public void setNewHandler(Handler handler) {
        currentHandler = handler;
    }
}
