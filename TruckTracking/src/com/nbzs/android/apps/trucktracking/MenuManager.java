/*
 * Copyright 2010 Google Inc.
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

import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Manage the application menus.
 *
 * @author Sandor Dornbush
 */
class MenuManager {

  private final MyTracks activity;

  public MenuManager(MyTracks activity) {
    this.activity = activity;
  }

  public boolean onCreateOptionsMenu(Menu menu) {
    activity.getMenuInflater().inflate(R.menu.main, menu);

    // TODO: Replace search button with search widget if API level >= 11
    return true;
  }

  public void onPrepareOptionsMenu(Menu menu, boolean hasRecorded,
      boolean isRecording, boolean hasSelectedTrack) {
  }

  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {

    }
    return false;
  }

  private boolean startActivity(Class<? extends Activity> activityClass) {
    activity.startActivity(new Intent(activity, activityClass));
    return true;
  }
}
