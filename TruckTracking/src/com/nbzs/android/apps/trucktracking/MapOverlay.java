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

import android.view.MotionEvent;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.apps.mytracks.maps.TrackPathPainter;
import com.google.android.apps.mytracks.maps.TrackPathPainterFactory;
import com.google.android.apps.mytracks.maps.TrackPathUtilities;
import com.google.android.apps.mytracks.util.LocationUtils;
import com.google.android.apps.mytracks.util.UnitConversions;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.google.android.maps.Projection;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.util.Log;
import org.osmdroid.api.IMapView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A map overlay that displays a "MyLocation" arrow, an error circle, the
 * currently recording track and optionally a selected track.
 *
 * @author Leif Hendrik Wilden
 */
public class MapOverlay extends Overlay {

    public class GoogleMapOverlay extends com.google.android.maps.Overlay {
        public GoogleMapOverlay(Context context) {
        }

//        @Override
//        public boolean onTap(com.google.android.maps.GeoPoint geoPoint, MapView mapView) {
//            boolean ret = onTapIt(geoPoint, new org.osmdroid.google.wrapper.MapView(mapView));
//            if (ret)
//                return true;
//            return super.onTap(geoPoint, mapView);    //To change body of overridden methods use File | Settings | File Templates.
//        }

        @Override
        public void draw(Canvas canvas, MapView mapView, boolean b) {
            drawit(canvas, new org.osmdroid.google.wrapper.MapView(mapView), b);
        }
    }

    public class OsmdroidOverlay extends org.osmdroid.views.overlay.Overlay {
        public OsmdroidOverlay(Context context) {
            super(context);
        }

        //@Override
        public void draw(Canvas canvas, org.osmdroid.views.MapView mapView, boolean shadow) {
            drawit(canvas, mapView, shadow);
        }

//        @Override
//        public boolean onSingleTapConfirmed(MotionEvent e, org.osmdroid.views.MapView mapView) {
//            com.google.android.maps.GeoPoint p = new com.google.android.maps.GeoPoint((int) e.getX(), (int) e.getY());
//            boolean ret = onTapIt(p, mapView);
//            if (ret)
//                return true;
//            return super.onSingleTapConfirmed(e, mapView);    //To change body of overridden methods use File | Settings | File Templates.
//        }
    }
    com.nbzs.android.apps.trucktracking.GoogleMapChinaOffset m_googleMapChinaOffset;
    private final static String TAG = Constants.TAG;
    Point AddMapOffset(IMapView mapView, org.osmdroid.util.GeoPoint geoPoint, Point pt) {
        if (mapView instanceof org.osmdroid.google.wrapper.MapView && !mapView.isSatellite()) {
            try {
                if (m_googleMapChinaOffset == null)
                {
                    m_googleMapChinaOffset = new com.nbzs.android.apps.trucktracking.GoogleMapChinaOffset(context);
                }
                //    Zoom少了1
                Point offset = m_googleMapChinaOffset.getOffset((double) geoPoint.getLatitudeE6() / 1E6,
                        (double) geoPoint.getLongitudeE6() / 1E6,
                        mapView.getZoomLevel() - 1);
                if (offset != null) {
                    pt.offset(offset.x, offset.y);
                    return offset;
                }
            } catch (Exception e) {
                Log.d(TAG, "googleMapChinaOffset Error: " + e.getMessage());
            }
        }
        return null;
    }

  private final Drawable[] arrows;
  private final int arrowWidth, arrowHeight;
  private final Drawable statsMarker;
  private final Drawable waypointMarker;
  private final Drawable startMarker;
  private final Drawable endMarker;
  private final int markerWidth, markerHeight;
  private final Paint errorCirclePaint;
  private final Context context;
  private final List<Waypoint> waypoints;
  private final List<CachedLocation> points;
  private final BlockingQueue<CachedLocation> pendingPoints;

  private boolean trackDrawingEnabled;
  private int lastHeading = 0;
  private Location myLocation;
  private boolean showEndMarker = true;
  // TODO: Remove it completely after completing performance tests.
  private boolean alwaysVisible = true;

  private GeoPoint lastReferencePoint;
  private Rect lastViewRect;
  private boolean lastPathExists;
  private TrackPathPainter trackPathPainter;

  /**
   * Represents a pre-processed {@code Location} to speed up drawing.
   * This class is more like a data object and doesn't provide accessors.
   */
  public static class CachedLocation {
    public final boolean valid;
    public final GeoPoint geoPoint;
    public final int speed;
    
    /**
     * Constructor for an invalid cached location.
     */
    public CachedLocation() {
      this.valid = false;
      this.geoPoint = null;
      this.speed = -1;
    }

    /**
     * Constructor for a potentially valid cached location.
     */
    public CachedLocation(Location location) {
      this.valid = LocationUtils.isValidLocation(location);
      this.geoPoint = valid ? LocationUtils.getGeoPoint(location) : null;
      this.speed = (int) Math.floor(location.getSpeed() * UnitConversions.MS_TO_KMH);
    }
  };

  public MapOverlay(Context context) {
    this.context = context;

    this.waypoints = new ArrayList<Waypoint>();
    this.points = new ArrayList<CachedLocation>(1024);
    this.pendingPoints = new ArrayBlockingQueue<CachedLocation>(
        Constants.MAX_DISPLAYED_TRACK_POINTS, true);

    // TODO: Can we use a FrameAnimation or similar here rather
    // than individual resources for each arrow direction?
    final Resources resources = context.getResources();
    arrows = new Drawable[] {
        resources.getDrawable(R.drawable.arrow_0),
        resources.getDrawable(R.drawable.arrow_20),
        resources.getDrawable(R.drawable.arrow_40),
        resources.getDrawable(R.drawable.arrow_60),
        resources.getDrawable(R.drawable.arrow_80),
        resources.getDrawable(R.drawable.arrow_100),
        resources.getDrawable(R.drawable.arrow_120),
        resources.getDrawable(R.drawable.arrow_140),
        resources.getDrawable(R.drawable.arrow_160),
        resources.getDrawable(R.drawable.arrow_180),
        resources.getDrawable(R.drawable.arrow_200),
        resources.getDrawable(R.drawable.arrow_220),
        resources.getDrawable(R.drawable.arrow_240),
        resources.getDrawable(R.drawable.arrow_260),
        resources.getDrawable(R.drawable.arrow_280),
        resources.getDrawable(R.drawable.arrow_300),
        resources.getDrawable(R.drawable.arrow_320),
        resources.getDrawable(R.drawable.arrow_340)
    };
    arrowWidth = arrows[lastHeading].getIntrinsicWidth();
    arrowHeight = arrows[lastHeading].getIntrinsicHeight();
    for (Drawable arrow : arrows) {
      arrow.setBounds(0, 0, arrowWidth, arrowHeight);
    }

    statsMarker = resources.getDrawable(R.drawable.ylw_pushpin);
    markerWidth = statsMarker.getIntrinsicWidth();
    markerHeight = statsMarker.getIntrinsicHeight();
    statsMarker.setBounds(0, 0, markerWidth, markerHeight);

    startMarker = resources.getDrawable(R.drawable.green_dot);
    startMarker.setBounds(0, 0, markerWidth, markerHeight);

    endMarker = resources.getDrawable(R.drawable.red_dot);
    endMarker.setBounds(0, 0, markerWidth, markerHeight);

    waypointMarker = resources.getDrawable(R.drawable.blue_pushpin);
    waypointMarker.setBounds(0, 0, markerWidth, markerHeight);

    errorCirclePaint = TrackPathUtilities.getPaint(R.color.blue, context);
    errorCirclePaint.setAlpha(127);
    
    trackPathPainter = TrackPathPainterFactory.getTrackPathPainter(context);

  }

  /**
   * Add a location to the map overlay.
   *
   * NOTE: This method doesn't take ownership of the given location, so it is
   * safe to reuse the same location while calling this method.
   *
   * @param l the location to add.
   */
  public void addLocation(Location l) {
    // Queue up in the pending queue until it's merged with {@code #points}.
    if (!pendingPoints.offer(new CachedLocation(l))) {
      Log.e(TAG, "Unable to add pending points");
    }
  }

  /**
   * Adds a segment split to the map overlay.
   */
  public void addSegmentSplit() {
    if (!pendingPoints.offer(new CachedLocation())) {
      Log.e(TAG, "Unable to add pending points");
    }
  }

  public void addWaypoint(Waypoint wpt) {
    // Note: We don't cache waypoints, because it's not worth the effort.
    if (wpt != null && wpt.getLocation() != null) {
      synchronized (waypoints) {
        waypoints.add(wpt);
      }
    }
  }

  public int getNumLocations() {
    synchronized (points) {
      return points.size() + pendingPoints.size();
    }
  }

  // Visible for testing.
  public int getNumWaypoints() {
    synchronized (waypoints) {
      return waypoints.size();
    }
  }

  public void clearPoints() {
    synchronized (getPoints()) {
      getPoints().clear();
      pendingPoints.clear();
      lastPathExists = false;
      lastViewRect = null;
      trackPathPainter.clear();
    }
  }

  public void clearWaypoints() {
    synchronized (waypoints) {
      waypoints.clear();
    }
  }

  public void setTrackDrawingEnabled(boolean trackDrawingEnabled) {
    this.trackDrawingEnabled = trackDrawingEnabled;
  }

  public void setShowEndMarker(boolean showEndMarker) {
    this.showEndMarker = showEndMarker;
  }

  public void drawit(Canvas canvas, org.osmdroid.api.IMapView mapView, boolean shadow) {
    if (shadow) {
      return;
    }

    // It's safe to keep projection within a single draw operation.
    final Projection projection = getMapProjection(mapView);
    if (projection == null) {
      Log.w(TAG, "No projection, unable to draw");
      return;
    }

    // Get the current viewing window.
    if (trackDrawingEnabled) {
      Rect viewRect = getMapViewRect(mapView);

      // Draw the selected track:
      drawTrack(canvas, projection, viewRect);

      // Draw the "Start" and "End" markers:
      drawMarkers(canvas, projection);
      
      // Draw the waypoints:
      drawWaypoints(canvas, projection);
    }

    // Draw the current location
    drawMyLocation(canvas, projection);
  }

  private void drawMarkers(Canvas canvas, Projection projection) {
    // Draw the "End" marker.
    if (showEndMarker) {
      for (int i = getPoints().size() - 1; i >= 0; --i) {
        if (getPoints().get(i).valid) {
          drawElement(canvas, projection, getPoints().get(i).geoPoint, endMarker,
              -markerWidth / 2, -markerHeight);
          break;
        }
      }
    }
    
    // Draw the "Start" marker.
    for (int i = 0; i < getPoints().size(); ++i) {
      if (getPoints().get(i).valid) {
        drawElement(canvas, projection, getPoints().get(i).geoPoint, startMarker,
            -markerWidth / 2, -markerHeight);
        break;
      }
    }
  }
  
  // Visible for testing.
  Projection getMapProjection(MapView mapView) {
    return mapView.getProjection();
  }

  // Visible for testing.
  Rect getMapViewRect(MapView mapView) {
    int w = mapView.getLongitudeSpan();
    int h = mapView.getLatitudeSpan();
    int cx = mapView.getMapCenter().getLongitudeE6();
    int cy = mapView.getMapCenter().getLatitudeE6();
    return new Rect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
  }
 
  // For use in testing only.
  public TrackPathPainter getTrackPathPainter() {
    return trackPathPainter;
  }
  
  // For use in testing only.
  public void setTrackPathPainter(TrackPathPainter trackPathPainter) {
    this.trackPathPainter = trackPathPainter;
  }

  private void drawWaypoints(Canvas canvas, Projection projection) {
    synchronized (waypoints) {;
      for (Waypoint wpt : waypoints) {
        Location loc = wpt.getLocation();
        drawElement(canvas, projection, LocationUtils.getGeoPoint(loc),
            wpt.getType() == Waypoint.TYPE_STATISTICS ? statsMarker
                : waypointMarker, -(markerWidth / 2) + 3, -markerHeight);
      }
    }
  }

  private void drawMyLocation(Canvas canvas, Projection projection) {
    // Draw the arrow icon.
    if (myLocation == null) {
      return;
    }

    Point pt = drawElement(canvas, projection,
        LocationUtils.getGeoPoint(myLocation), arrows[lastHeading],
        -(arrowWidth / 2) + 3, -(arrowHeight / 2));
    // Draw the error circle.
    float radius = projection.metersToEquatorPixels(myLocation.getAccuracy());
    canvas.drawCircle(pt.x, pt.y, radius, errorCirclePaint);
  }

  private void drawTrack(Canvas canvas, Projection projection, Rect viewRect)
  {
    boolean draw;

    synchronized (points) {
      // Merge the pending points with the list of cached locations.
      final GeoPoint referencePoint = projection.fromPixels(0, 0);
      int newPoints = pendingPoints.drainTo(points); 
      boolean newProjection = !viewRect.equals(lastViewRect) ||
          !referencePoint.equals(lastReferencePoint); 
      if (newPoints == 0 && lastPathExists && !newProjection) {
        // No need to recreate path (same points and viewing area).
        draw = true;
      } else {
        int numPoints = points.size();
        if (numPoints < 2) {
          // Not enough points to draw a path.
          draw = false;
        } else if (!trackPathPainter.needsRedraw() && lastPathExists && !newProjection) {
          // Incremental update of the path, without repositioning the view.
          draw = true;
          trackPathPainter.updatePath(projection, viewRect, numPoints - newPoints, alwaysVisible, points);  
        } else {
          // The view has changed so we have to start from scratch.
          draw = true;
          trackPathPainter.updatePath(projection, viewRect, 0, alwaysVisible, points);  
        }
      }
      lastReferencePoint = referencePoint;
      lastViewRect = viewRect;
    }
    if (draw) {
      trackPathPainter.drawTrack(canvas);
    }
  }

  // Visible for testing.
  Point drawElement(Canvas canvas, Projection projection, GeoPoint geoPoint,
      Drawable element, int offsetX, int offsetY) {
    Point pt = new Point();
    projection.toPixels(geoPoint, pt);
      //AddMapOffset(mapView, geoPoint, pt);
    canvas.save();
    canvas.translate(pt.x + offsetX, pt.y + offsetY);
    element.draw(canvas);
    canvas.restore();
    return pt;
  }

  /**
   * Sets the pointer location (will be drawn on next invalidate).
   */
  public void setMyLocation(Location myLocation) {
    this.myLocation = myLocation;
  }

  /**
   * Sets the pointer heading in degrees (will be drawn on next invalidate).
   *
   * @return true if the visible heading changed (i.e. a redraw of pointer is
   *         potentially necessary)
   */
  public boolean setHeading(float heading) {
    int newhdg = Math.round(-heading / 360 * 18 + 180);
    while (newhdg < 0)
      newhdg += 18;
    while (newhdg > 17)
      newhdg -= 18;
    if (newhdg != lastHeading) {
      lastHeading = newhdg;
      return true;
    } else {
      return false;
    }
  }
  
  /**
   * @return the points
   */
  public List<CachedLocation> getPoints() {
    return points;
  }
}
