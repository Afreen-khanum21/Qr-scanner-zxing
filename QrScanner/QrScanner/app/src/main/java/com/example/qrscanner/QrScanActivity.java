package com.example.qrscanner;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;

public class QrScanActivity extends Activity implements SurfaceHolder.Callback {

    private final int CAMERA_REQUEST_CODE = 101;

    private CameraManager mCameraManager;
    private TextView mStatusView;
    private boolean mHasSurface;
    private String LOG_TAG = "QrScanActivity";
    private Handler mDecodeHandler;
    private HandlerThread mHandlerThread;
    private Context mContext;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private int mSurfaceWidth, mSurfaceHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_qr_scan);

        mStatusView = findViewById(R.id.status_view);
        mSurfaceView = findViewById(R.id.preview_view);

        mHasSurface = false;
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mStatusView.setText("Scan a valid QR");
        mStatusView.setVisibility(View.VISIBLE);
    }

    private void startQrScan() {
        if (checkSelfPermission(Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            mHandlerThread = new HandlerThread("DecodeHandlerThread");
            mHandlerThread.start();
            mDecodeHandler = new DecodeHandler(mHandlerThread.getLooper(), this);
            mCameraManager = new CameraManager(this, mDecodeHandler);

            if (mHasSurface) {
                // The activity was paused but not stopped, so the surface still exists.
                // Therefore surfaceCreated() won't be called, so init the camera here.
                initCamera(mSurfaceHolder);
            }
        } else {
            requestPermissions(new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    int[] grantResults) {
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Log.e(LOG_TAG, "Camera permission denied");
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    protected CameraManager getCameraManager() {
        return mCameraManager;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(LOG_TAG, "onResume() called");
        startQrScan();
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG, "onPause() called");
        stopScan();
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_FOCUS || keyCode == KeyEvent.KEYCODE_CAMERA) {
              // Handle these events so they don't launch the Camera app
              return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onCancel(View v) {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void setActionBarTitle(String title) {
        ActionBar actionBar = getActionBar();
        actionBar.setTitle(title);
        actionBar.show();
    }

    /**
   * SurfaceHandler callbacks arrives here, when surface is first created.
   */
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(LOG_TAG, "Surface Created");
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mHasSurface = false;
        Log.d(LOG_TAG, "Surface Destroyed");
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(LOG_TAG, "Surface Changed + w:" + width + ", h:"+height);
        if (holder == null) {
            Log.e(LOG_TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
        }
        if (!mHasSurface) {
            mHasSurface = true;
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            initCamera(holder);
        }
    }

    private void initCamera(SurfaceHolder surfaceHolder) {
        try {
            if (mCameraManager != null) {
                mCameraManager.setPreviewSize(mSurfaceWidth, mSurfaceHeight);
                mCameraManager.openDriver(surfaceHolder);
                // Starts the preview, which can also throw a
                // RuntimeException.
            }
        } catch (IOException ioe) {
            Log.e(LOG_TAG, "Exception: " + ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.e(LOG_TAG, "Unexpected error initializing camera " + e);
        }
    }

    protected void handleDecode(String data) {
        stopScan();
        Log.d("hii",data);
        Intent result = new Intent(this,ScanResult.class);
        result.putExtra("res",data);
        startActivity(result);
        finish();
    }

    private void stopScan() {
        if (mCameraManager != null) mCameraManager.closeDriver();
        if (mDecodeHandler != null) mDecodeHandler.removeCallbacksAndMessages(null);
        if (mHandlerThread != null) mHandlerThread.quit();
    }
}

