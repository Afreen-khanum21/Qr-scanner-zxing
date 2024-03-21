package com.example.qrscanner;

import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import static com.example.qrscanner.CameraManager.AUTO_FOCUS;

final class AutoFocusCallback implements Camera.AutoFocusCallback {

    private static final String TAG = AutoFocusCallback.class.getSimpleName();

    private static final long AUTOFOCUS_INTERVAL_MS = 1500L;

    private Handler mAutoFocusHandler;

    void setHandler(Handler autoFocusHandler) {
        this.mAutoFocusHandler = autoFocusHandler;
    }

    /**
     * Autofocus callbacks arrive here, and are dispatched to the Handler which requested them.
     */
    public void onAutoFocus(boolean success, Camera camera) {
        if (mAutoFocusHandler != null) {
            Message message = mAutoFocusHandler.obtainMessage(AUTO_FOCUS, success);
            // Simulate continuous autofocus by sending a focus request every
            // AUTOFOCUS_INTERVAL_MS milliseconds.
            Log.d(TAG, "Got auto-focus callback; requesting another");
            mAutoFocusHandler.sendMessageDelayed(message, AUTOFOCUS_INTERVAL_MS);
            mAutoFocusHandler = null;
        } else {
            Log.d(TAG, "Got auto-focus callback, but no handler for it");
        }
    }

}

