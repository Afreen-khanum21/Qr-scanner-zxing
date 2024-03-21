package com.example.qrscanner;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Size;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import java.util.List;
import java.util.Collection;

/**
 * A class which deals with reading, parsing, and setting the camera parameters which are used to
 * configure the camera hardware.
 */
final class CameraConfigurationManager {

    private static final String TAG = "CameraConfiguration";

    private final Context mContext;
    private Size mCameraResolution;
    private int cwRotationFromDisplayToCamera;

    CameraConfigurationManager(Context context) {
        this.mContext = context;
    }

    private static void initializeTorch(Camera.Parameters parameters) {
        boolean currentSetting = false;
        doSetTorch(parameters, currentSetting);
    }

    private static void doSetTorch(Camera.Parameters parameters, boolean newSetting) {
        String flashMode;
        if (newSetting) {
            flashMode = findSettableValue(parameters.getSupportedFlashModes(),
                    Camera.Parameters.FLASH_MODE_TORCH,
                    Camera.Parameters.FLASH_MODE_ON);
        } else {
            flashMode = findSettableValue(parameters.getSupportedFlashModes(),
                    Camera.Parameters.FLASH_MODE_OFF);
        }
        if (flashMode != null) {
            parameters.setFlashMode(flashMode);
        }
    }

    private static String findSettableValue(Collection<String> supportedValues,
                                            String... desiredValues) {
        Log.i(TAG, "Supported values: " + supportedValues);
        String result = null;
        if (supportedValues != null) {
            for (String desiredValue : desiredValues) {
                if (supportedValues.contains(desiredValue)) {
                    result = desiredValue;
                    break;
                }
            }
        }
        Log.i(TAG, "Settable value: " + result);
        return result;
    }

    /**
     * Reads, one time, values from the camera that are needed by the app.
     */
    void initFromCameraParameters(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        WindowManager manager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        int displayRotation = display.getRotation();

        int cwRotationFromNaturalToDisplay;
        switch (displayRotation) {
            case Surface.ROTATION_0:
                cwRotationFromNaturalToDisplay = 0;
                break;
            case Surface.ROTATION_90:
                cwRotationFromNaturalToDisplay = 90;
                break;
            case Surface.ROTATION_180:
                cwRotationFromNaturalToDisplay = 180;
                break;
            case Surface.ROTATION_270:
                cwRotationFromNaturalToDisplay = 270;
                break;
            default:
                // Have seen this return incorrect values like -90
                if (displayRotation % 90 == 0) {
                    cwRotationFromNaturalToDisplay = (360 + displayRotation) % 360;
                } else {
                    throw new IllegalArgumentException("Bad rotation: " + displayRotation);
                }
        }
        Log.i(TAG, "Display at: " + cwRotationFromNaturalToDisplay);
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(0, cameraInfo);

        int cwRotationFromNaturalToCamera = cameraInfo.orientation;
        Log.i(TAG, "Camera at: " + cwRotationFromNaturalToCamera);

        int cwRotationFromDisplayToCamera =
                (360 + cwRotationFromNaturalToCamera - cwRotationFromNaturalToDisplay) % 360;
        Log.i(TAG, "Display orientation: " + cwRotationFromDisplayToCamera);
        camera.setDisplayOrientation(cwRotationFromDisplayToCamera);
    }

    void setDesiredCameraParameters(Camera camera, Size previewSize) {
        Camera.Parameters parameters = camera.getParameters();

        if (parameters == null) {
            Log.w(TAG, "Device error: no camera parameters are available." +
                    "Proceeding without configuration.");
            return;
        }

        initializeTorch(parameters);
        String focusMode = findSettableValue(parameters.getSupportedFocusModes(),
                Camera.Parameters.FOCUS_MODE_AUTO,
                Camera.Parameters.FOCUS_MODE_MACRO);
        if (focusMode != null) {
            parameters.setFocusMode(focusMode);
        }
        mCameraResolution = getBestPreviewSize(parameters, previewSize);
        parameters.setPreviewSize(mCameraResolution.getWidth(), mCameraResolution.getHeight());
        camera.setParameters(parameters);
    }


    Size getCameraResolution() {
        return mCameraResolution;
     }

    void setTorch(Camera camera, boolean newSetting) {
        Camera.Parameters parameters = camera.getParameters();
        doSetTorch(parameters, newSetting);
        camera.setParameters(parameters);
        boolean currentSetting = false;
    }

    private Size getBestPreviewSize(Camera.Parameters parameters, Size windowSize) {
        final double minRatioDiffPercent = 0.1;
        final double winRatio = getRatio(windowSize.getWidth(), windowSize.getHeight());
        double bestChoiceRatio = 0;
        Size bestChoice = new Size(0, 0);
        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            double ratio = getRatio(size.width, size.height);
            if (size.height * size.width > bestChoice.getWidth() * bestChoice.getHeight()
                    && (Math.abs(bestChoiceRatio - winRatio) / winRatio > minRatioDiffPercent
                    || Math.abs(ratio - winRatio) / winRatio <= minRatioDiffPercent)) {
                bestChoice = new Size(size.width, size.height);
                bestChoiceRatio = getRatio(size.width, size.height);
            }
        }
        return bestChoice;
    }

    private double getRatio(double x, double y) {
        return (x < y) ? x / y : y / x;
    }
}

