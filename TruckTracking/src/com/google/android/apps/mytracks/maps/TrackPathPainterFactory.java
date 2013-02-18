/*
 * Copyright 2011 Google Inc.
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
package com.google.android.apps.mytracks.maps;

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.Constants;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * A factory for TrackPathPainters.
 *
 * @author Vangelis S.
 */
public class TrackPathPainterFactory {

  private TrackPathPainterFactory() {
  }

  /**
   * Get a new TrackPathPainter.
   * @param context Context to fetch system preferences.
   * @return The TrackPathPainter that corresponds to the track color mode setting.
   */
  public static TrackPathPainter getTrackPathPainter(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(
        Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
    if (prefs == null) {
      return new SingleColorTrackPathPainter(context);
    }

      return new SingleColorTrackPathPainter(context);
  }
}