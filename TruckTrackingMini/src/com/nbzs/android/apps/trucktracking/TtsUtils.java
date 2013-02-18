package com.nbzs.android.apps.trucktracking;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import com.nbzs.android.apps.SystemUtils;

import java.util.Locale;

/**
 * Created by IntelliJ IDEA.
 * User: Zephyrrr
 * Date: 12-3-7
 * Time: 下午7:46
 * To change this template use File | Settings | File Templates.
 */
public class TtsUtils implements TextToSpeech.OnInitListener{
    private final String TAG = Constants.TAG;

    private TextToSpeech mTts;
    private boolean mTtsEnabled = false;
    private Context _ctx;
    public  TtsUtils(Context ctx)
    {
        _ctx = ctx;
        mTts = new TextToSpeech(ctx, this);
    }

    // Implements TextToSpeech.OnInitListener.
    public void onInit(int status) {
        // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
        if (status == TextToSpeech.SUCCESS) {
            // Set preferred language to US english.
            // Note that a language may not be available, and the result will indicate this.
            int result = mTts.setLanguage(Locale.getDefault());
            // Try this someday for some interesting results.
            // int result mTts.setLanguage(Locale.FRANCE);
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Lanuage data is missing or the language is not supported.
                Log.e(TAG, "Language is not available.");
                mTts = null;
            }
            else
            {
                mTtsEnabled = true;
            }
        } else {
            // Initialization failed.
            Log.e(TAG, "Could not initialize TextToSpeech.");
            mTts = null;
        }
    }

    public void saySomething(String str)
    {
        if (mTts != null && mTtsEnabled)
        {
            try
            {
                mTts.speak(str,TextToSpeech.QUEUE_FLUSH, null);
            }
            catch (Exception e)
            {
                SystemUtils.processException(e);
            }
        }
    }
}
