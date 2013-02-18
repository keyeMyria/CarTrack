package com.nbzs.android.apps.trucktracking;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.nbzs.android.apps.SystemUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 12-11-5
 * Time: 下午8:33
 * To change this template use File | Settings | File Templates.
 */
public class SendTrackPointContext {
    public  SendTrackPointContext(Context context)
    {
        m_parentContext = context;

        // for the MyTracks service
        mytracksIntent = new Intent();
        ComponentName componentName = new ComponentName(
                m_parentContext.getString(R.string.mytracks_service_package),m_parentContext.getString(R.string.mytracks_service_class));
        mytracksIntent.setComponent(componentName);
    }
    private static final String TAG = Constants.TAG;

    private Context m_parentContext;

    // MyTracks service
    private ITrackRecordingService myTracksService;
    // mytracksIntent to access the MyTracks service
    private Intent mytracksIntent;
    // connection to the MyTracks service
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            myTracksService = ITrackRecordingService.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            myTracksService = null;

            m_parentContext.startService(mytracksIntent);
            m_parentContext.bindService(mytracksIntent, serviceConnection, 0);
        }
    };

    public void onStart() {
        // start and bind the MyTracks service
        m_parentContext.startService(mytracksIntent);
        m_parentContext.bindService(mytracksIntent, serviceConnection, 0);
    }
    protected void onStop() {
        //if (myTracksService != null)  // cause leak
        {
            m_parentContext.unbindService(serviceConnection);
        }
        try {
            if (myTracksService == null)// || !myTracksService.isRecording())
            {
                m_parentContext.stopService(mytracksIntent);
            }
        } catch (Exception e) {
            SystemUtils.processException(e);
        }
    }

    public Boolean startRecordingService()
    {
        boolean  ret = false;
        if (myTracksService != null) {
            try {
                long trackId = myTracksService.getRecordingTrackId();
                Log.d(TAG, "getRecordingTrackId: " + Long.toString(trackId));
                if (trackId <= 0)
                {
                    trackId = myTracksService.startNewTrack();
                    ret = true;
                }
                //if (trackId > 0)
                {
                    m_parentContext.startService(new Intent(m_parentContext, SendTrackPointService.class));
                }
            } catch (Exception e) {
                SystemUtils.processException(e);
                Toast.makeText(m_parentContext, m_parentContext.getString(R.string.startRecording) + m_parentContext.getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
        if (ret)
        {
            //Toast.makeText(m_parentContext, m_parentContext.getString(R.string.startRecording), Toast.LENGTH_SHORT).show();
        }
        return ret;
    }
    public Boolean stopRecordingService()
    {
        boolean ret = false;
        if (myTracksService != null) {
            try {
                if (myTracksService.isRecording())
                {
                    myTracksService.endCurrentTrack();
                    ret = true;
                }
                m_parentContext.stopService(new Intent(m_parentContext, SendTrackPointService.class));
            } catch (RemoteException e) {
                SystemUtils.processException(e);
                Toast.makeText(m_parentContext, m_parentContext.getString(R.string.stopRecording) + m_parentContext.getString(R.string.error), Toast.LENGTH_SHORT).show();
            }
        }
        if (ret)
        {
            //Toast.makeText(m_parentContext, m_parentContext.getString(R.string.stopRecording), Toast.LENGTH_SHORT).show();
        }
        return ret;
    }
}
