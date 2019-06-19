package com.zebra.jamesswinton.contextualvoiceec30;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.support.v7.app.AppCompatActivity;

import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.ui.AIButton;

import com.symbol.emdk.barcode.ScanDataCollection;
import com.symbol.emdk.barcode.Scanner;
import com.symbol.emdk.barcode.ScannerException;
import com.zebra.jamesswinton.contextualvoiceec30.databinding.ActivityContextualVoiceDemoBinding;

public class ContextualVoiceDemo extends AppCompatActivity implements AIButton.AIButtonListener, Scanner.DataListener {

  // Debugging
  private static final String TAG = "ContextualVoiceDemo";

  // Constants
  private static final String SCAN = "scan";
  private static final String CUSTOMER_IDENTIFIER = "<b>You: </b>";
  private static final String ASSOCIATE_IDENTIFIER = "<b>Assistant: </b>";
  private static final String ERROR_IDENTIFIER = "<font color='red'><b>Error: </b></font><i>";

  // Variables
  private ActivityContextualVoiceDemoBinding mDataBinding;
  private AIConfiguration mAiConfig;
  private AIDataService mAiDataService;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_contextual_voice_demo);

    // Init DataBinding
    mDataBinding = DataBindingUtil.setContentView(this, R.layout.activity_contextual_voice_demo);

    // Init Toolbar
    setSupportActionBar(mDataBinding.toolbar);
    if (getSupportActionBar() != null) {
      getSupportActionBar().setTitle("Co-Op Voice");
    }

    // Init Scrolling Chat Log
    mDataBinding.voiceAssistantChatLog.setMovementMethod(new ScrollingMovementMethod());

    // Init Dialog Flow
    initDialogFlow();
  }

  @Override
  protected void onPause() {
    super.onPause();
    // Disable Scanner
    if (App.mScanner != null) {
      disableScanner();
    }

    // Pause AI Listener, if visible
    if (mDataBinding.contextualVoiceButton.getVisibility() == View.VISIBLE) {
      mDataBinding.contextualVoiceButton.pause();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    // Enable Scanner
    enableScanner();

    // Resume AI Listener, if visible
    if (mDataBinding.contextualVoiceButton.getVisibility() == View.VISIBLE) {
      mDataBinding.contextualVoiceButton.resume();
    }
  }

  private void enableScanner() {
    mDataBinding.toolbar.postDelayed(() -> {
      final Scanner.DataListener dataListener = ContextualVoiceDemo.this;
      try {
        ((App) getApplicationContext()).enableScanner(dataListener);
      } catch (ScannerException e) {
        Log.e(TAG, "ScannerException: " + e.getMessage());
      }
    }, 500);
  }

  private void disableScanner() {
    try {
      ((App) getApplicationContext()).disableScanner(this);
    } catch (ScannerException e) {
      Log.e(TAG, "ScannerException: " + e.getMessage());
    }
  }

  private void initDialogFlow() {
    // Get Access token
    String aiAccessToken = getString(R.string.default_ai_access_token);

    // Log
    Log.i(TAG, "Initialising DialogFlow with access token: " + aiAccessToken);

    // Init Ai Button
    mAiConfig = new AIConfiguration(aiAccessToken,
        AIConfiguration.SupportedLanguages.English,
        AIConfiguration.RecognitionEngine.System);
    mDataBinding.contextualVoiceButton.initialize(mAiConfig);
    mDataBinding.contextualVoiceButton.setResultsListener(this);
    // Init AI Data Service
    mAiDataService = new AIDataService(this, mAiConfig);
  }

  /*
   * DialogFlow Methods
   */
  @Override
  public void onResult(AIResponse response) {
    runOnUiThread(() -> {
      // Log Response Info
      Log.d(TAG, "AIResponse Result Successful");
      Log.i(TAG, "Status code: " + response.getStatus().getCode());
      Log.i(TAG, "Resolved query: " + response.getResult().getResolvedQuery());
      Log.i(TAG, "Action: " + response.getResult().getAction());
      Log.i(TAG, "Speech: " + response.getResult().getFulfillment().getSpeech());

      // Get Speech
      String query = response.getResult().getResolvedQuery();
      String speech = response.getResult().getFulfillment().getSpeech();

      // Vocalise Result
      TTS.speak(speech);

      // Handle Scan
      if (query.equalsIgnoreCase(SCAN)) {
        // Start Scan
        startScan();
      } else {
        // Log Speech to TextView
        mDataBinding.voiceAssistantChatLog.append(
            Html.fromHtml(CUSTOMER_IDENTIFIER + response.getResult().getResolvedQuery() + "<br>"));
        mDataBinding.voiceAssistantChatLog.append(
            Html.fromHtml(ASSOCIATE_IDENTIFIER + speech + "<br><br>"));
      }
    });
  }

  @Override
  public void onError(AIError error) {
    runOnUiThread(() -> {
      // Log Error
      Log.e(TAG, "AI Error: " + error.getMessage());

      // Log Speech to TextView
      mDataBinding.voiceAssistantChatLog.append(
        Html.fromHtml(ERROR_IDENTIFIER + error.getMessage() + "</i><br><br>"));
    });
  }

  @Override
  public void onCancelled() {
      runOnUiThread(() -> Log.e(TAG, "AI Cancelled"));
  }

  private void startScan() {
    try {
      Log.i(TAG, "Voice Activated - Init Soft Scan");
      // Cancel Pending Read
      if (App.mScanner.isReadPending()) { App.mScanner.cancelRead(); }
      // Update Trigger Type
      App.mScanner.triggerType = Scanner.TriggerType.SOFT_ONCE;
      // Start Read
      App.mScanner.read();
    } catch (ScannerException e) {
      Log.e(TAG, "ScannerException: " + e.getMessage(), e);
    }
  }

  /*
   * Scan Methods
   */
  @Override
  public void onData(ScanDataCollection scanDataCollection) {
    // Get Scanner Data as []
    ScanDataCollection.ScanData[] scannedData = scanDataCollection.getScanData().toArray(
        new ScanDataCollection.ScanData[scanDataCollection.getScanData().size()]);

    // Debugging
    for (ScanDataCollection.ScanData scanData : scannedData) {
      Log.i(TAG, "Label Type: " + scanData.getLabelType().name());
      Log.i(TAG, "Barcode: " + scanData.getData());
      Log.i(TAG, "Label Type: " + scanData.getLabelType().toString());
      mDataBinding.voiceAssistantChatLog.append(
          Html.fromHtml(ASSOCIATE_IDENTIFIER + scanData.getData() + "<br><br>"));
    }

  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    switch(keyCode) {
      case KeyEvent.KEYCODE_VOLUME_UP:
        Log.i(TAG, "Up Button Pressed");
        break;
      case KeyEvent.KEYCODE_VOLUME_DOWN:
        Log.i(TAG, "Down Button Pressed");
        break;
    }

    return super.onKeyDown(keyCode, event);
  }
}
