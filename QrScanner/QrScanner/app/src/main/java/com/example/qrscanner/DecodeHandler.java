package com.example.qrscanner;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.ReaderException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;

import java.util.Hashtable;
import java.util.Vector;

import static com.example.qrscanner.CameraManager.AUTO_FOCUS;
import static com.example.qrscanner.CameraManager.DECODE;

/**
 * Handler class to decode the scanned data.
 */

public class DecodeHandler extends Handler {
    private final MultiFormatReader mMultiFormatReader;
    private final Hashtable<DecodeHintType, Object> mHints;
    private final String TAG = "DecodeHandler";
    private QrScanActivity mActivity;

    public DecodeHandler(Looper looper, QrScanActivity activity) {
        this.mActivity = activity;
        mMultiFormatReader = new MultiFormatReader();
        mHints = new Hashtable<DecodeHintType, Object>();
        Vector<BarcodeFormat> formats = new Vector<BarcodeFormat>();
        formats.add(BarcodeFormat.QR_CODE);
        mHints.put(DecodeHintType.POSSIBLE_FORMATS, formats);
        mMultiFormatReader.setHints(mHints);
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        if (msg.what == DECODE) {
            decode((byte[]) msg.obj, msg.arg1, msg.arg2);
        } else if (msg.what == AUTO_FOCUS) {
            mActivity.getCameraManager().requestAutoFocus();
        }
    }

    /**
     * Decode the QR from image.
     */
    private void decode(byte[] data, int width, int height) {
        Result rawResult = null;
        PlanarYUVLuminanceSource source = mActivity.getCameraManager()
                .buildLuminanceSource(data, width, height);
        if (source != null) {
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            try {
                if(bitmap !=null) {
                    rawResult = mMultiFormatReader.decode(bitmap);
                }
            } catch (ReaderException re) {
                // continue
            } finally {
                mMultiFormatReader.reset();
            }
        }
        if (rawResult != null) {
            mActivity.getCameraManager().stopPreview();
            mActivity.handleDecode(rawResult.getText());
        } else {
            //valid QR not found continue to sacn and decode.
            mActivity.getCameraManager().previewAndDecode();
        }

    }
}

