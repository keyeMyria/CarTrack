package com.nbzs.android.apps.trucktracking;

import android.location.*;
import android.text.TextUtils;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Menu;
import android.widget.Toast;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.TrackPointsColumns;
import com.google.android.apps.mytracks.services.ITrackRecordingService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.database.ContentObserver;
import android.os.Handler;
import android.widget.EditText;
import com.nbzs.android.apps.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main2Activity extends Activity
{
    private static final String TAG = Constants.TAG;

    private final Handler contentHandler;
    private TrackObserver trackObserver;
    class TrackObserver extends ContentObserver {

        public TrackObserver() {
            super(contentHandler);
        }

        public void onChange(boolean selfChange) {
            //onNewTrackPoint();
        }
    }
    private void onNewTrackPoint(Location loc)
    {
        if (loc == null)
            return;

        //sendTrackPointContext.sendTrackPointData(loc, null);

        showMessage(getString(R.string.send_track_point));

        if (m_geoCoder != null)
        {
            try
            {
                List<Address> ret = m_geoCoder.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);
                if (ret != null && ret.size() > 0)
                {
                    showMessage(getString(R.string.now_position) + ret.get(0).toString());
                }
            }
            catch (IOException e)
            {
                SystemUtils.processException(e);
            }
        }
    }

    private Geocoder m_geoCoder;
    // utils to access the MyTracks content provider
    private MyTracksProviderUtils myTracksProviderUtils;

    // display output from the MyTracks content provider
    private TextView outputTextView;

    public Main2Activity()
    {
        super();
        contentHandler = new Handler();
        sendTrackPointContext = new SendTrackPointContext(this);
    }

    Button dotaskButton;
    LocationManagerUtils locationManagerUtils;
    TtsUtils ttsUtils;
    SendTrackPointContext sendTrackPointContext;
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main2);

        locationManagerUtils = new LocationManagerUtils(this);
        if (!locationManagerUtils.checkLocationManager())
        {
            Toast.makeText(this, R.string.please_enable_gps, Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }
        // for the MyTracks content provider
        myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);

        AppPreferences config = AppPreferences.getInstance();
        if (config.enableTts())
        {
            ttsUtils = new TtsUtils(this);
        }
        if (config.enableReverseGeocode())
        {
            m_geoCoder = new Geocoder(this, Locale.getDefault());
        }
        
        trackObserver = new TrackObserver();
        this.getContentResolver().registerContentObserver(TrackPointsColumns.CONTENT_URI, true, trackObserver);

        outputTextView = (TextView) findViewById(R.id.output);

        dotaskButton = (Button) findViewById(R.id.dotask_button);
        dotaskButton.setText(R.string.startRecording);
        dotaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dotaskButton.getText() == getString(R.string.startRecording))
                {
                    if (sendTrackPointContext.startRecordingService())
                    {
                        dotaskButton.setText(R.string.stopRecording);
                    }
                }
                else if (dotaskButton.getText() == getString(R.string.stopRecording))
                {
                    if (sendTrackPointContext.stopRecordingService())
                    {
                        dotaskButton.setText(R.string.startRecording);
                    }
                }
                else if (dotaskButton.getText() == getString(R.string.send_current_gpsloc))
                {
                    onNewTrackPoint(locationManagerUtils.getLastLocation());
                }
            }
        });
        
        Button sosButton =  (Button) findViewById(R.id.send_button);
        sosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (myTracksService != null)
//                {
//                    Location nbLoc = new Location("GPS");
//                    nbLoc.setLatitude(29.5);
//                    nbLoc.setLongitude(121.5);
//                    onNewTrackPoint(nbLoc);
//                    //mySericeUtils.sendWayPointData(myTracksProviderUtils.getLastLocation(), "SOS");
//                } else
                {
                    Location loc = myTracksProviderUtils.getLastLocation();
                    if (loc == null)
                        return;
                    String action = ((EditText)findViewById(R.id.send_text)).getText().toString().trim();
                    if (TextUtils.isEmpty(action))
                        return;
                    //"SOS";
                    //sendTrackPointContext.getServiceUtils().sendTrackPointData(loc, action);
                    showMessage(action);
                }
            }
        });

        Button debugButton =  (Button) findViewById(R.id.show_debug_button);
        debugButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    String msg = "";//sendTrackPointContext.getServiceUtils().getDebugStat();
                    showMessage(msg);
                    msg = SystemUtils.getExceptionLog();
                    showMessage(msg);
                }
        });
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (trackObserver != null) {
            this.getContentResolver().unregisterContentObserver(trackObserver);
        }
        if (sendTrackPointContext != null)
        {
            sendTrackPointContext.onStop();
        }
    }

    public void showMessage(String str)
    {
        outputTextView.append(str);
        outputTextView.append("\r\n");
        if (ttsUtils != null)
        {
            ttsUtils.saySomething(str);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


}
