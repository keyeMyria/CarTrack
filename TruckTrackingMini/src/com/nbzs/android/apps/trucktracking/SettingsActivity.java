package com.nbzs.android.apps.trucktracking;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 12-3-2
 * Time: 上午11:32
 * To change this template use File | Settings | File Templates.
 */



import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.content.Context;
/**
 * An activity that let's the user see and edit the settings.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class SettingsActivity extends PreferenceActivity {

    private SharedPreferences preferences;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // Tell it where to read/write preferences
        /*PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(Constants.SETTINGS_NAME);
        preferenceManager.setSharedPreferencesMode(Context.MODE_PRIVATE);*/

        // Load the preferences to be displayed
        addPreferencesFromResource(R.xml.preferences);
    }


    @Override
    protected void onDestroy() {
        AppPreferences.getInstance().Update();
        super.onPause();
    }
}

