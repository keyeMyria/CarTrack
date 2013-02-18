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

import static com.google.android.apps.mytracks.Constants.TAG;

import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.TracksColumns;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.stats.TripStatistics;
import com.google.android.apps.mytracks.util.GeoRect;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;

import android.app.Dialog;
import android.content.ContentUris;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.MyLocationOverlay;

import java.util.EnumSet;

/**
 * The map view activity of the MyTracks application.
 *
 * @author Leif Hendrik Wilden
 * @author Rodrigo Damazio
 */
public class MapActivity extends com.google.android.maps.MapActivity
    implements View.OnTouchListener, View.OnClickListener {
    private int m_oldMapmode = -2;
    private int m_nowMapmode = -1;

    // Application life cycle:
    // ------------------------
    private void setMap() {
        setMap(m_nowMapmode);
    }
    private com.google.android.maps.MapView mapViewGoogle = null;
    private org.osmdroid.views.MapView mapViewOsmdroid = null;
    private org.osmdroid.api.IMapView mapViewInterface;
    private android.view.ViewGroup mapView;
    private MapOverlay mapOverlay;
    private org.osmdroid.api.IMyLocationOverlay mLocationOverlay;
    private org.osmdroid.ResourceProxy mResourceProxy;
    SharedPreferences mPrefs;

    private void setMap(int mapMode) {
        if (mapMode == m_oldMapmode)
            return;

        //RelativeLayout mapLayout = (RelativeLayout)findViewById(R.id.map);
        RelativeLayout mapLayout = screen;
        boolean useGoogleMap = mapMode == -1;
        if (useGoogleMap) {
            // F0:70:CB:ED:9C:7F:7B:13:74:C7:0D:69:F2:05:1A:7C
            if (mapViewGoogle == null) {
                mapViewGoogle = new com.google.android.maps.MapView(this, getString(R.string.GOOGLE_MAPS_API_KEY));
            }
            if (mapViewOsmdroid != null && m_oldMapmode != -1 && m_oldMapmode != -2) {
                mapLayout.removeView(mapViewOsmdroid);
            }
            mapViewInterface = new org.osmdroid.google.wrapper.MapView(mapViewGoogle);
            mapLayout.addView(mapViewGoogle, 0, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,
                    RelativeLayout.LayoutParams.FILL_PARENT));
            mapView = mapViewGoogle;

            MapOverlay.GoogleMapOverlay mo = mapOverlay.new GoogleMapOverlay(this);
            mapViewGoogle.getOverlays().add(mo);

            final org.osmdroid.google.wrapper.MyLocationOverlay mlo = new org.osmdroid.google.wrapper.MyLocationOverlay(this, mapViewGoogle);
            mapViewGoogle.getOverlays().add(mlo);
            mLocationOverlay = mlo;

            mapView.setClickable(true);
            mapView.setEnabled(true);
            mapView.setFocusable(true);
            mapView.setFocusableInTouchMode(true);

            Log.d(TAG, "TileSource selected as googlemap ");
        } else {
            if (m_oldMapmode == -1 || m_oldMapmode == -2) {
                mapViewOsmdroid = new org.osmdroid.views.MapView(this, 256, mResourceProxy);
                if (mapViewGoogle != null) {
                    mapLayout.removeView(mapViewGoogle);
                }
                mapViewInterface = mapViewOsmdroid;
                mapLayout.addView(mapViewOsmdroid, 0, new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT,
                        RelativeLayout.LayoutParams.FILL_PARENT));
                MapOverlay.OsmdroidOverlay mo = mapOverlay.new OsmdroidOverlay(this);
                mapViewOsmdroid.getOverlays().add(mo);
                final MyLocationOverlay mlo = new MyLocationOverlay(this.getBaseContext(), mapViewOsmdroid,
                        mResourceProxy);
                mapViewOsmdroid.getOverlays().add(mlo);
                this.mLocationOverlay = mlo;
                mapView = mapViewOsmdroid;

                mapViewOsmdroid.setMultiTouchControls(true);
            }

            try {
                org.osmdroid.tileprovider.tilesource.ITileSource ts = org.osmdroid.tileprovider.tilesource.TileSourceFactory.getTileSource(mapMode);
                Log.d(TAG, "TileSource selected as " + ts.name());
                mapViewOsmdroid.setTileSource(ts);
            } catch (Exception e) {
                mapViewOsmdroid.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                Log.d(TAG, "TileSource selected as default.");
            }

        }

        m_oldMapmode = mapMode;
    }
    private void InitOsmMaps()
    {
        mResourceProxy = new org.osmdroid.DefaultResourceProxyImpl(getApplicationContext());

        try {
            org.osmdroid.tileprovider.tilesource.TileSourceFactory.getTileSource("BingMap");
        } catch (Exception ex) {
            org.osmdroid.tileprovider.util.CloudmadeUtil.retrieveCloudmadeKey(getApplicationContext());
            org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource.retrieveBingKey(getApplicationContext());
            org.osmdroid.tileprovider.tilesource.TileSourceFactory.addTileSource(new org.osmdroid.tileprovider.tilesource.bing.BingMapTileSource(null));
        }
    }
    private static  final String TAG = Constants.TAG;

  private static final int DIALOG_INSTALL_EARTH = 0;

  // Saved instance state keys:
  // ---------------------------

  private static final String KEY_CURRENT_LOCATION = "currentLocation";
  private static final String KEY_KEEP_MY_LOCATION_VISIBLE = "keepMyLocationVisible";

  /**
   * True if the map should be scrolled so that the pointer is always in the
   * visible area.
   */
  private boolean keepMyLocationVisible;

  /**
   * The ID of a track on which we want to show a waypoint.
   * The waypoint will be shown as soon as the track is loaded.
   */
  private long showWaypointTrackId;

  /**
   * The ID of a waypoint which we want to show.
   * The waypoint will be shown as soon as its track is loaded.
   */
  private long showWaypointId;

  /**
   * The track that's currently selected.
   * This differs from {@link TrackDataHub#getSelectedTrackId} in that this one is only set after
   * actual track data has been received.
   */
  private long selectedTrackId;

  /**
   * The current pointer location.
   * This is kept to quickly center on it when the user requests.
   */
  private Location currentLocation;

  // UI elements:
  // -------------

  private RelativeLayout screen;
//  private MapView mapView;
//  private MapOverlay mapOverlay;
  private LinearLayout messagePane;
  private TextView messageText;
  private LinearLayout busyPane;
  private ImageButton optionsBtn;

  private MenuItem myLocation;
  private MenuItem toggleLayers;

  /**
   * We are not displaying driving directions. Just an arbitrary track that is
   * not associated to any licensed mapping data. Therefore it should be okay to
   * return false here and still comply with the terms of service.
   */
  @Override
  protected boolean isRouteDisplayed() {
    return false;
  }

  /**
   * We are displaying a location. This needs to return true in order to comply
   * with the terms of service.
   */
  @Override
  protected boolean isLocationDisplayed() {
    return true;
  }
    
  // Application life cycle:
  // ------------------------

  @Override
  protected void onCreate(Bundle bundle) {
    Log.d(TAG, "MapActivity.onCreate");
    super.onCreate(bundle);

    // The volume we want to control is the Text-To-Speech volume
    setVolumeControlStream(TextToSpeech.Engine.DEFAULT_STREAM);

    // We don't need a window title bar:
    requestWindowFeature(Window.FEATURE_NO_TITLE);

    // Inflate the layout:
    setContentView(R.layout.mytracks_layout);

    // Remove the window's background because the MapView will obscure it
    getWindow().setBackgroundDrawable(null);

    // Set up a map overlay:
    screen = (RelativeLayout) findViewById(R.id.screen);
    mapView = (MapView) findViewById(R.id.map);
    mapView.requestFocus();
    mapOverlay = new MapOverlay(this);
      mapViewInterface.getOverlays().add(mapOverlay);
    mapView.setOnTouchListener(this);
      mapViewInterface.setBuiltInZoomControls(true);

    optionsBtn.setOnClickListener(this);

      mPrefs = this.getSharedPreferences(Constants.SETTINGS_NAME, 0);
      m_nowMapmode = mPrefs.getInt("mapmode_key", -1);
      setMap();
      boolean showCompass = mPrefs.getBoolean("ShowCompassInMap", false);
      if (mLocationOverlay != null) {
          if (!showCompass) {
              mLocationOverlay.disableCompass();
              Log.d(TAG, "disable compass in map.");
          } else {
              mLocationOverlay.enableCompass();
              Log.d(TAG, "enable compass in map.");
          }
      }
  }

  @Override
  protected void onRestoreInstanceState(Bundle bundle) {
    Log.d(TAG, "MapActivity.onRestoreInstanceState");
    if (bundle != null) {
      super.onRestoreInstanceState(bundle);
      keepMyLocationVisible =
          bundle.getBoolean(KEY_KEEP_MY_LOCATION_VISIBLE, false);
      if (bundle.containsKey(KEY_CURRENT_LOCATION)) {
        currentLocation = (Location) bundle.getParcelable(KEY_CURRENT_LOCATION);
        if (currentLocation != null) {
          showCurrentLocation();
        }
      } else {
        currentLocation = null;
      }
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    Log.d(TAG, "MapActivity.onSaveInstanceState");
    outState.putBoolean(KEY_KEEP_MY_LOCATION_VISIBLE, keepMyLocationVisible);
    if (currentLocation != null) {
      outState.putParcelable(KEY_CURRENT_LOCATION, currentLocation);
    }
    super.onSaveInstanceState(outState);
  }


  // Utility functions:
  // -------------------

  /**
   * Shows the options button if a track is selected, or hide it if not.
   */
  private void updateOptionsButton(boolean trackSelected) {
    optionsBtn.setVisibility(
        trackSelected ? View.VISIBLE : View.INVISIBLE);
  }

  /**
   * Tests if a location is visible.
   *
   * @param location a given location
   * @return true if the given location is within the visible map area
   */
  private boolean locationIsVisible(Location location) {
    if (location == null || mapView == null) {
      return false;
    }
    GeoPoint center = mapViewInterface.getMapCenter();
    int latSpan = mapViewInterface.getLatitudeSpan();
    int lonSpan = mapViewInterface.getLongitudeSpan();

    // Bottom of map view is obscured by zoom controls/buttons.
    // Subtract a margin from the visible area:
    GeoPoint marginBottom = mapViewInterface.getProjection().fromPixels(
        0, mapView.getHeight());
    GeoPoint marginTop = mapViewInterface.getProjection().fromPixels(0,
        mapView.getHeight()
            - mapViewInterface.getZoomButtonsController().getZoomControls().getHeight());
    int margin =
        Math.abs(marginTop.getLatitudeE6() - marginBottom.getLatitudeE6());
    GeoRect r = new GeoRect(center, latSpan, lonSpan);
    r.top += margin;

    GeoPoint geoPoint = LocationUtils.getGeoPoint(location);
    return r.contains(geoPoint);
  }

  /**
   * Moves the location pointer to the current location and center the map if
   * the current location is outside the visible area.
   */
  private void showCurrentLocation() {
    if (mapOverlay == null || mapView == null) {
      return;
    }

    mapOverlay.setMyLocation(currentLocation);
    mapView.postInvalidate();

    if (currentLocation != null && keepMyLocationVisible && !locationIsVisible(currentLocation)) {
      GeoPoint geoPoint = LocationUtils.getGeoPoint(currentLocation);
      MapController controller = mapViewInterface.getController();
      controller.animateTo(geoPoint);
    }
  }

  /**
   * Zooms and pans the map so that the given track is visible.
   *
   * @param track the track
   */
  private void zoomMapToBoundaries(Track track) {
    if (mapView == null) {
      return;
    }

    if (track == null || track.getNumberOfPoints() < 2) {
      return;
    }

    TripStatistics stats = track.getStatistics();
    int bottom = stats.getBottom();
    int left = stats.getLeft();
    int latSpanE6 = stats.getTop() - bottom;
    int lonSpanE6 = stats.getRight() - left;
    if (latSpanE6 > 0
        && latSpanE6 < 180E6
        && lonSpanE6 > 0
        && lonSpanE6 < 360E6) {
      keepMyLocationVisible = false;
      GeoPoint center = new GeoPoint(
          bottom + latSpanE6 / 2,
          left + lonSpanE6 / 2);
      if (LocationUtils.isValidGeoPoint(center)) {
          mapViewInterface.getController().setCenter(new org.osmdroid.google.wrapper.GeoPoint(center));
          mapViewInterface.getController().zoomToSpan(latSpanE6, lonSpanE6);
      }
    }
  }

  /**
   * Zooms and pans the map so that the given waypoint is visible.
   */
  public void showWaypoint(long waypointId) {
    MyTracksProviderUtils providerUtils = MyTracksProviderUtils.Factory.get(this);
    Waypoint wpt = providerUtils.getWaypoint(waypointId);
    if (wpt != null && wpt.getLocation() != null) {
      keepMyLocationVisible = false;
      GeoPoint center = new GeoPoint(
          (int) (wpt.getLocation().getLatitude() * 1E6),
          (int) (wpt.getLocation().getLongitude() * 1E6));
        mapViewInterface.getController().setCenter(new org.osmdroid.google.wrapper.GeoPoint(center));
        mapViewInterface.getController().setZoom(20);
      mapView.invalidate();
    }
  }

  /**
   * Zooms and pans the map so that the given waypoint is visible, when the given track is loaded.
   * If the track is already loaded, it does that immediately.
   *
   * @param trackId the ID of the track on which to show the waypoint
   * @param waypointId the ID of the waypoint to show
   */
  public void showWaypoint(long trackId, long waypointId) {
    synchronized (this) {
      if (trackId == selectedTrackId) {
        showWaypoint(waypointId);
        return;
      }

      showWaypointTrackId = trackId;
      showWaypointId = waypointId;
    }
  }

  /**
   * Does the proper zooming/panning for a just-loaded track.
   * This may be either zooming to a waypoint that has been previously selected, or
   * zooming to the whole track.
   *
   * @param track the loaded track
   */
  private void zoomLoadedTrack(Track track) {
    synchronized (this) {
      if (track.getId() == showWaypointTrackId) {
        // There's a waypoint to show in this track.
        showWaypoint(showWaypointId);

        showWaypointId = 0L;
        showWaypointTrackId = 0L;
      } else {
        // Zoom out to show the whole track.
        zoomMapToBoundaries(track);
      }
    }
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    myLocation = menu.add(
        Menu.NONE, Constants.MENU_MY_LOCATION, Menu.NONE, R.string.menu_map_view_my_location);
    myLocation.setIcon(android.R.drawable.ic_menu_mylocation);
    toggleLayers = menu.add(
        Menu.NONE, Constants.MENU_TOGGLE_LAYERS, Menu.NONE, R.string.menu_map_view_satellite_mode);
    toggleLayers.setIcon(android.R.drawable.ic_menu_mapmode);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    toggleLayers.setTitle(mapViewInterface.isSatellite() ?
        R.string.menu_map_view_map_mode : R.string.menu_map_view_satellite_mode);
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case Constants.MENU_MY_LOCATION: {
        keepMyLocationVisible = true;
        if (mapViewInterface.getZoomLevel() < 18) {
          mapViewInterface.getController().setZoom(18);
        }
        if (currentLocation != null) {
          showCurrentLocation();
        }
        return true;
      }
      case Constants.MENU_TOGGLE_LAYERS: {
       // mapView.setSatellite(!mapView.isSatellite());
        return true;
      }
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  public void onClick(View v) {
    if (v == messagePane) {
      startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
    } else if (v == optionsBtn) {
      optionsBtn.performLongClick();
    }
  }

  /**
   * We want the pointer to become visible again in case of the next location
   * update:
   */
  @Override
  public boolean onTouch(View view, MotionEvent event) {
    if (keepMyLocationVisible && event.getAction() == MotionEvent.ACTION_MOVE) {
      if (!locationIsVisible(currentLocation)) {
        keepMyLocationVisible = false;
      }
    }
    return false;
  }

}
