package com.zebra.jamesswinton.contextualvoiceec30;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

public class TTS {

    private static TextToSpeech textToSpeech;

    public static void init(final Context context) {
        if (textToSpeech == null) {
            textToSpeech = new TextToSpeech(context, i -> Log.i("TTS", "TTS Initialised"));
        }
    }

    public static void speak(final String text) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }
}
