package com.nbzs.android.apps.trucktracking;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 12-3-2
 * Time: 上午11:32
 * To change this template use File | Settings | File Templates.
 */



import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setSharedPreferencesName(Constants.SETTINGS_NAME);
        preferenceManager.setSharedPreferencesMode(0);

        // Load the preferences to be displayed
        addPreferencesFromResource(R.xml.preferences);
    }


    @Override
    protected void onDestroy() {
        ServiceProcess.Get(this).getMyWebConfig().Update();
        super.onPause();
    }
}

