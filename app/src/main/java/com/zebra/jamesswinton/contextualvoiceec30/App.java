package com.zebra.jamesswinton.contextualvoiceec30;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.symbol.emdk.EMDKManager;
import com.symbol.emdk.EMDKManager.EMDKListener;
import com.symbol.emdk.barcode.BarcodeManager;
import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.Scanner.DataListener;
import com.symbol.emdk.barcode.Scanner.StatusListener;
import com.symbol.emdk.barcode.ScannerConfig;
import com.symbol.emdk.barcode.ScannerException;
import com.symbol.emdk.barcode.StatusData;
import java.util.ArrayList;
import java.util.List;

public class App extends Application implements EMDKListener, StatusListener, DataListener {

  // Debugging
  private static final String TAG = "ApplicationClass";

  // Constants
  public static final Handler mUiThread = new Handler(Looper.getMainLooper());

  // Static Variables
  public static EMDKManager mEmdkManager;

  public static Context mContext;
  public static Scanner mScanner;

  private static boolean mIsScanning = false;
  private static BarcodeManager mBarcodeManager;

  // Non-Static Variables
  private List<DataListener> mDataListeners = new ArrayList<>();

  @Override
  public void onCreate() {
    super.onCreate();

    // Set Context
    mContext = getApplicationContext();

    // Initialise TTS Engine
    TTS.init(this);

    // Get EMDK Manager
    EMDKManager.getEMDKManager(this, this);
  }

  /*
   * Called to notify the client when the EMDKManager object has been opened and its ready to use.
   */
  @Override
  public void onOpened(EMDKManager emdkManager) {
    // Log EMDK Open
    Log.i(TAG, "EMDK: Open");

    // Init EMDK Manager
    mEmdkManager = emdkManager;

    // Init Barcode Manager
    Log.i(TAG, "Init Barcode Manager");
    mBarcodeManager = (BarcodeManager) mEmdkManager.getInstance(EMDKManager.FEATURE_TYPE.BARCODE);

    // Init Scanner
    try {
      initScanner();
    } catch (ScannerException e) {
      Log.e(TAG, "ScannerException: " + e.getMessage(), e);
    }
  }

  /*
   * Notifies user upon a abrupt closing of EMDKManager.
   */
  @Override
  public void onClosed() {
    // Log EMDK Closed
    Log.i(TAG, "EMDK: Closed");

    // Release EMDK Manager
    if (mEmdkManager != null) {
      mEmdkManager.release();
      mEmdkManager = null;
    }
  }

  void initScanner() throws ScannerException {
    Log.i(TAG, "Init Scanner");
    // Init Scanner
    mScanner = mBarcodeManager.getDevice(BarcodeManager.DeviceIdentifier.DEFAULT);
    // Set Scanner Listeners
    mScanner.addDataListener(this);
    mScanner.addStatusListener(this);
    // Enable Scanner if needed
    if (mIsScanning) {
      enableScanner(null);
    }
  }

  public void enableScanner(DataListener dataListener) throws ScannerException {
    Log.i(TAG, "Enable Scanner");

    // Add DataListener to List if Exists
    if (dataListener != null && !mDataListeners.contains(dataListener)) {
      mDataListeners.add(dataListener);
    }

    // Enable Scanner
    mScanner.enable();
    mIsScanning = true;

    // Build & Set Scanner Meta (Can only be done after Scanner is Enabled)
    ScannerConfig config = mScanner.getConfig();
    config.readerParams.readerSpecific.imagerSpecific.pickList = ScannerConfig.PickList.ENABLED;
    config.readerParams.readerSpecific.imagerSpecific.digimarcDecoding = true;
    config.scanParams.decodeAudioFeedbackUri = "system/media/audio/notifications/decode-short.wav";
    config.scanParams.decodeHapticFeedback = true;
    config.decoderParams.code128.enabled = true;
    config.decoderParams.code39.enabled = true;
    config.decoderParams.upca.enabled = true;
    mScanner.setConfig(config);
  }

  public void disableScanner(DataListener dataListener) throws ScannerException {
    Log.i(TAG, "Disable Scanner");

    // Remove DataListener from List if Exists
    if (mDataListeners.contains(dataListener)) {
      mDataListeners.remove(dataListener);
      mScanner.removeDataListener(dataListener);
    }

    // Disable Scanner
    mScanner.disable();
    mIsScanning = false;
  }

  /*
   * This is the callback method upon data availability.
   */
  @Override
  public void onData(ScanDataCollection scanDataCollection) {
    mUiThread.post(() -> {
      // Handle Data
      for (DataListener dataListener : mDataListeners) {
        dataListener.onData(scanDataCollection);
      }

      // Restart Scanner
      if (mScanner != null) {
        try {
          if (!mScanner.isReadPending()) mScanner.read();
        } catch (ScannerException e) {
          Log.e(TAG, "ScannerException: " + e.getMessage(), e);
        }
      }
    });
  }

  /*
   * This is the callback method upon scan status event occurs.
   */
  @Override
  public void onStatus(StatusData statusData) {
    switch (statusData.getState()) {
      case IDLE:
        try {
          try { Thread.sleep(100); }
          catch (InterruptedException e) { e.printStackTrace(); }
          mScanner.read();
        } catch (ScannerException e) {
          Log.e(TAG, "ScannerException: " + e.getMessage(), e);
        }
        break;
      case WAITING:
        Log.i(TAG, "Scanner waiting...");
        break;
      case SCANNING:
        Log.i(TAG, "Scanner scanning...");
        break;
      case DISABLED:
        Log.i(TAG, "Scanner Disabled...");
        break;
      case ERROR:
        Log.i(TAG, "Scanner Error!");
        break;
      default:
        break;
    }
  }

}
