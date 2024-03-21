package com.example.qrscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;

import java.lang.Exception;

import com.example.qrscanner.CameraConfigurationManager;

import java.io.IOException;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 */
public final class CameraManager {

    public static final int DECODE = 1;
    public static final int AUTO_FOCUS = 2;
    private static final String TAG = CameraManager.class.getSimpleName();
    private final Context context;
    private final PreviewCallback previewCallback;
    private final AutoFocusCallback autoFocusCallback;
    private Camera camera;
    private boolean initialized;
    private boolean previewing;
    private int requestedFramingRectWidth;
    private int requestedFramingRectHeight;
    private Handler mDecodeHandler;
    private CameraConfigurationManager configManager;
    private Size mPreviewSize;

    public CameraManager(Context context, Handler handler) {
        this.context = context;
        this.mDecodeHandler = handler;
        previewCallback = new PreviewCallback();
        autoFocusCallback = new AutoFocusCallback();
        this.configManager = new CameraConfigurationManager(context);
    }

    public void openDriver(SurfaceHolder holder) throws IOException {
        if (camera == null) {
            camera = Camera.open();
            if (camera == null) {
                throw new IOException();
            }
        }
        if (!initialized) {
            initialized = true;
            configManager.initFromCameraParameters(camera);
        }
        configManager.setDesiredCameraParameters(camera, mPreviewSize);
        camera.setPreviewDisplay(holder);
        startPreview(holder);
    }

    /**
     * Closes the camera driver if still in use.
     */
    public void closeDriver() {
        if (camera != null) {
            camera.release();
            camera = null;
            initialized = false;
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    public void stopPreview() {
        if (previewing) {
            Log.d(TAG, "stoping camera preview");
            camera.stopPreview();
            previewing = false;
        }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    public void startPreview(SurfaceHolder holder) {
        Log.d(TAG, "starting camera preview");
        if (camera != null && !previewing) {
            try {
                camera.startPreview();
            } catch (Exception e) {
                Log.d(TAG, "Could not start camera preview : " + e);
            }
            previewing = true;
            requestAutoFocus();
            previewAndDecode();
        }
    }

    public Camera.Parameters getParam() {
        return camera.getParameters();
    }

    public void setParam(Camera.Parameters param) {
        camera.setParameters(param);
    }

    public void setDisplayOrientation(int val) {
        camera.setDisplayOrientation(val);
    }

    public void previewAndDecode() {
        if (camera != null && previewing) {
            previewCallback.setHandler(mDecodeHandler);
            camera.setOneShotPreviewCallback(previewCallback);
        }
    }

    protected void setPreviewSize(int width, int height) {
        mPreviewSize =  new Size(width, height);
    }


    /**
     * This method is copied from com.google.zxing.client.android.camera.CameraManager class.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        return new PlanarYUVLuminanceSource(data, width, height);
    }

    public void requestAutoFocus() {
        if (camera != null && previewing) {
            autoFocusCallback.setHandler(mDecodeHandler);
            camera.autoFocus(autoFocusCallback);
        }
    }

    /**
     * Preview callbacks arrive here, and data is passed to DecodeHandler.
     */
    private class PreviewCallback implements Camera.PreviewCallback {
        private Handler decodeHandler;

        void setHandler(Handler decodeHandler) {
            this.decodeHandler = decodeHandler;
        }

        public void onPreviewFrame(byte[] data, Camera camera) {
            Size cameraResolution = configManager.getCameraResolution();
            if (decodeHandler != null) {
                Message message = new Message();
                message.obj = data;
                message.arg1 = cameraResolution.getWidth();
                message.arg2 = cameraResolution.getHeight();
                message.what = DECODE;
                decodeHandler.sendMessage(message);
                decodeHandler = null;
            } else {
                Log.d(TAG, "Got preview callback, but no handler for it");
            }
        }
    }
}

