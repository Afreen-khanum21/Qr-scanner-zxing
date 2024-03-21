package com.example.qrscanner;

import com.google.zxing.LuminanceSource;

import android.graphics.Bitmap;

/**
 * This class refers to com.google.zxing.client.android.camera.PlanarYUVLuminanceSource.java
 *
 * This object extends LuminanceSource around an array of YUV data returned from the camera driver.
 * This can be used to exclude superfluous pixels around the perimeter and speed up decoding.
 *
 */
public final class PlanarYUVLuminanceSource extends LuminanceSource {

  private final byte[] yuvData;
  private final int dataWidth;
  private final int dataHeight;

  public PlanarYUVLuminanceSource(byte[] yuvData, int dataWidth, int dataHeight) {
    super(dataWidth, dataHeight);

    this.yuvData = yuvData;
    this.dataWidth = dataWidth;
    this.dataHeight = dataHeight;
  }
  @Override
  public byte[] getRow(int y, byte[] row) {
    if (y < 0 || y >= dataHeight) {
      throw new IllegalArgumentException("Requested row is outside the image: " + y);
    }
    if (row == null || row.length < dataWidth) {
      row = new byte[dataWidth];
    }
    System.arraycopy(yuvData, y * dataWidth, row, 0, dataWidth);
    return row;
  }

  @Override
  public byte[] getMatrix() {
    return yuvData;
  }
}

