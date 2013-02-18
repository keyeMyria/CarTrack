/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.nbzs.android.apps.trucktracking;

//import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import android.content.*;
import android.os.IBinder;
import com.google.android.apps.mytracks.content.*;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.apps.mytracks.NavControls;

import android.app.Activity;
import android.app.TabActivity;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.RelativeLayout;
import android.widget.TabHost;
import android.widget.Toast;

/**
 * The super activity that embeds our sub activities.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
@SuppressWarnings("deprecation")
public class MyTracks extends TabActivity implements OnTouchListener {
    private static  final String TAG  = Constants.TAG;

  private static final int DIALOG_EULA_ID = 0;

  /**
   * Menu manager.
   */
  private MenuManager menuManager;

  /**
   * Preferences.
   */
  private SharedPreferences preferences;

  /**
   * True if a new track should be created after the track recording service
   * binds.
   */
  private boolean startNewTrackRequested = false;

  /**
   * Utilities to deal with the database.
   */
  private MyTracksProviderUtils providerUtils;
    // intent to access the MyTracks service
    private Intent intent;
  /**
   * Google Analytics tracker
   */
  //private GoogleAnalyticsTracker tracker;

  /*
   * Tabs/View navigation:
   */

  private NavControls navControls;

  private final Runnable changeTab = new Runnable() {
    public void run() {
      getTabHost().setCurrentTab(navControls.getCurrentIcons());
    }
  };

    // MyTracks service
    private ITrackRecordingService myTracksService;

    // connection to the MyTracks service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            myTracksService = ITrackRecordingService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            myTracksService = null;
        }
    };

  /*
   * Application lifetime events:
   * ============================
   */

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.d(Constants.TAG, "MyTracks.onCreate");
    super.onCreate(savedInstanceState);

//    tracker = GoogleAnalyticsTracker.getInstance();
//    // Start the tracker in manual dispatch mode...
//    tracker.start(getString(R.string.my_tracks_analytics_id), getApplicationContext());
//    tracker.setProductVersion("android-mytracks", SystemUtils.getMyTracksVersion(this));
//    tracker.trackPageView("/appstart");
//    tracker.dispatch();

    providerUtils = MyTracksProviderUtils.Factory.get(this);
    preferences = getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);

    menuManager = new MenuManager(this);

    // We don't need a window title bar:
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    // If the user just starts typing (on a device with a keyboard), we start a search.
    setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

    final Resources res = getResources();
    final TabHost tabHost = getTabHost();
    tabHost.addTab(tabHost.newTabSpec("tab1")
        .setIndicator("Map", res.getDrawable(
            android.R.drawable.ic_menu_mapmode))
        .setContent(new Intent(this, MapActivity.class)));
//    tabHost.addTab(tabHost.newTabSpec("tab2")
//        .setIndicator("Stats", res.getDrawable(R.drawable.menu_stats))
//        .setContent(new Intent(this, CameraViewActivity.class)));
//    tabHost.addTab(tabHost.newTabSpec("tab3")
//        .setIndicator("Chart", res.getDrawable(R.drawable.menu_elevation))
//        .setContent(new Intent(this, DebugStatActivity.class)));

    // Hide the tab widget itself. We'll use overlayed prev/next buttons to
    // switch between the tabs:
    tabHost.getTabWidget().setVisibility(View.GONE);

    RelativeLayout layout = new RelativeLayout(this);
    LayoutParams params =
        new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
    layout.setLayoutParams(params);
    navControls =
        new NavControls(this, layout,
            getResources().obtainTypedArray(R.array.left_icons),
            getResources().obtainTypedArray(R.array.right_icons),
            changeTab);
    navControls.show();
    tabHost.addView(layout);
    layout.setOnTouchListener(this);
  }

  @Override
  protected void onStart() {
    Log.d(TAG, "MyTracks.onStart");
    super.onStart();

      // start and bind the MyTracks service
      startService(intent);
      bindService(intent, serviceConnection, 0);
  }


  @Override
  protected void onStop() {
    Log.d(TAG, "MyTracks.onStop");
      // unbind and stop the MyTracks service
      if (myTracksService != null) {
          unbindService(serviceConnection);
      }
      stopService(intent);
    super.onStop();
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    return menuManager.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menuManager.onPrepareOptionsMenu(menu, providerUtils.getLastTrack() != null,
            myTracksService.isRecording(),  true);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return menuManager.onOptionsItemSelected(item)
        ? true
        : super.onOptionsItemSelected(item);
  }

  private void showWaypoint(long trackId, long waypointId) {
    MapActivity map =
        (MapActivity) getLocalActivityManager().getActivity("tab1");
    if (map != null) {
      getTabHost().setCurrentTab(0);
      map.showWaypoint(trackId, waypointId);
    } else {
      Log.e(TAG, "Couldnt' get map tab");
    }
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    if (event.getAction() == MotionEvent.ACTION_DOWN) {
      navControls.show();
    }
    return false;
  }
}
