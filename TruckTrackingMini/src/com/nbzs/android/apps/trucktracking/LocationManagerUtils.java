package com.nbzs.android.apps.trucktracking;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * Created by IntelliJ IDEA.
 * User: Zephyrrr
 * Date: 12-3-7
 * Time: 下午7:40
 * To change this template use File | Settings | File Templates.
 */
public class LocationManagerUtils {

    public LocationManagerUtils(Context ctx)
    {
        _ctx = ctx;    
    }
    private Context _ctx;
    private Location lastLoc;
    private LocationManager locationManager;
    private String locProvider;

    public boolean checkLocationManager()
    {
        locationManager = (LocationManager) _ctx.getSystemService(Context.LOCATION_SERVICE);

        Criteria crta = new Criteria();
        crta.setAccuracy(Criteria.ACCURACY_FINE);
        crta.setAltitudeRequired(false);
        crta.setBearingRequired(false);
        crta.setCostAllowed(true);
        crta.setPowerRequirement(Criteria.POWER_LOW);
        locProvider = locationManager.getBestProvider(crta, true);
        
        return locProvider != null && locProvider != "";
    }

    public void initUpdate()
    {
        if (locationManager == null)
        {
            if (!checkLocationManager())
                return;
        }
        if (locProvider != null) {
            //Location location = locationManager.getLastKnownLocation(provider);
            //updateNewLocationWithLocationManager(location);

            //dotaskButton.setText(R.string.send_current_gpsloc);
            locationManager.requestLocationUpdates(locProvider, 1000, 0, locationListener);
        }
    }

    public void deinitUpdate()
    {
        if (locationManager != null)
        {
            locationManager.removeUpdates(locationListener);
        }
    }

    public  Location getLastLocation()
    {
        return lastLoc;
    }
    private final LocationListener locationListener = new LocationListener()
    {
        @Override
        public void onLocationChanged(Location location) {
            lastLoc = location;
        }

        @Override
        public void onProviderDisabled(String provider) {
            lastLoc = null;
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };
}
