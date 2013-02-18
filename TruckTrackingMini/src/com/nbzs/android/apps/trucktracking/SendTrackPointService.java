package com.nbzs.android.apps.trucktracking;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.IBinder;
import android.content.Context;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.nbzs.android.apps.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Created by IntelliJ IDEA.
 * User: Zephyrrr
 * Date: 12-3-7
 * Time: 下午8:23
 * To change this template use File | Settings | File Templates.
 */
public class SendTrackPointService extends Service
{
    private static final String TAG = Constants.TAG;
    private AppPreferences m_config;

    NotificationManager mNotificationManager;
    private final Timer timer = new Timer();
    public IBinder onBind(Intent arg0)
    {
        return null;
    }

    private CarTrackingWebServiceClient m_webServiceClient;
    public CarTrackingWebServiceClient getWebServiceClient() {
        return m_webServiceClient;
    }

    // utils to access the MyTracks content provider
    private MyTracksProviderUtils myTracksProviderUtils;

    public void onCreate()
    {
        super.onCreate();
        // for the MyTracks content provider
        myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);

        m_config = AppPreferences.getInstance();

        String serverUrl = m_config.getServerUrl();
        if (serverUrl != null && !TextUtils.isEmpty(serverUrl)) {
            m_webServiceClient = new CarTrackingWebServiceClient(this, serverUrl);
        }

        m_sendLocations = new ArrayBlockingQueue<String>(m_config.getSendMsgQueCnt(), true);

        SystemUtils.acquireWakeLock(this, m_config.enableScreen() ? PowerManager.SCREEN_DIM_WAKE_LOCK : PowerManager.PARTIAL_WAKE_LOCK);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification = new Notification(R.drawable.icon, null, System.currentTimeMillis());

        Context context = getApplicationContext();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        notification.setLatestEventInfo(context, getString(R.string.app_name), getString(R.string.sending_track_point), contentIntent);
        notification.flags += Notification.FLAG_NO_CLEAR;
        startForeground(1, notification);
        //mNotificationManager.notify(NOTIFICATION_ID, notification);

        timer.scheduleAtFixedRate(new sendTrackPointTask(), 0, m_config.getSendInterval());

        resetStat();
        //m_sendLocations.offer("Start");
        StartThreadSendGpsData();
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    private static final int NOTIFICATION_ID = 1;

    private class sendTrackPointTask extends TimerTask
    {
        private Location m_lastLocation;
        public void run()
        {
            Location loc = myTracksProviderUtils.getLastLocation();
            if (loc == null)
                return;
            if (loc.getLatitude() == 0 || loc.getLongitude() == 0)
                return;
            if (m_lastLocation != null && m_lastLocation.getTime() == loc.getTime())
                return;
            sendTrackPointData(loc, null);
            m_lastLocation = loc;
        }
    }

    public void onDestroy()
    {
        super.onDestroy();

        //m_sendLocations.offer("Stop");
        StopThreadSendGpsData(false);

        //if (m_config.getisStartVideoRecording())
        //{
        //    StopRecordingVideo();
        //}

        SystemUtils.releaseWakeLock();
        stopForeground(true);
    }


    /*private void TryCreateCamera()
    {
         if(m_myCamera == null)
         {
            SurfaceView surfaceView = new SurfaceView(m_context);
             SurfaceHolder surfaceHolder = surfaceView.getHolder();
             surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            m_myCamera = MyCamera.Get(m_context, surfaceHolder);

             Camera mCamera = m_myCamera.GetCamera();

             Camera.Parameters p = mCamera.getParameters();
        p.setPreviewSize(320, 240);
        //p.setPreviewFormat(PixelFormat.JPEG);
        mCamera.setParameters(p);
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
            e.printStackTrace();
        }
         }
    }*/
    /*private void StartRecordingVideo()
    {
        Intent startIntent = new Intent(m_context, CameraViewActivity.class);
        startIntent.putExtra("recording", 1);
        m_context.startActivity(startIntent);
         //TryCreateCamera();
        //m_myCamera.startRecording();
    }
    private void StopRecordingVideo()
    {
        //Intent startIntent = new Intent(m_context, CameraViewActivity.class);
        //startIntent.putExtra("recoding", 2);
        //m_context.startActivity(startIntent);

        //if (m_myCamera != null && m_myCamera.isRecording())
        //{
       //     m_myCamera.stopRecording();
        //}
    }*/


    private static SimpleDateFormat m_dataFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final NumberFormat LAT_LONG_FORMAT = new DecimalFormat("##,###.00000");
    private static final NumberFormat ALTITUDE_FORMAT = new DecimalFormat("###,###");
    private static final NumberFormat SPEED_FORMAT = new DecimalFormat("#,###,###.00");

    //private MyCamera m_myCamera;

    private static String LocationToString(Location location) {
        String gpsData = m_dataFormat.format(new Date(location.getTime())) + "," +
                LAT_LONG_FORMAT.format(location.getLatitude()) + "," +
                LAT_LONG_FORMAT.format(location.getLongitude()) + "," +
                SPEED_FORMAT.format(location.getAccuracy()) + "," +
                ALTITUDE_FORMAT.format(location.getAltitude()) + "," +
                SPEED_FORMAT.format(location.getBearing()) + "," +
                SPEED_FORMAT.format(location.getSpeed());
        return gpsData;
    }
    private static String LocationActionToString(Location location, String action)
    {
        String gpsData = LocationToString(location);
        return gpsData + "," + action;
    }
    java.lang.Thread m_threadSendGpsData;

    int m_gpsDataAll = 0;
    int m_gpsDataSendSuccess = 0;
    int m_gpsDataSendFail = 0;
    int m_gpsDataDrop = 0;

    int getGpsDataQueueCount() {
        return m_sendLocations.size();
    }

    private void resetStat() {
        m_gpsDataAll = 0;
        m_gpsDataSendSuccess = 0;
        m_gpsDataSendFail = 0;
        m_gpsDataDrop = 0;
    }

    private synchronized void StartThreadSendGpsData() {
        if (m_threadSendGpsData != null && m_threadSendGpsData.isAlive()
                && !m_threadSendGpsData.isInterrupted())
            return;

        m_threadSendGpsData = new java.lang.Thread() {
            @Override
            public void run() {
                while (!this.isInterrupted()) {
                    String s = null;
                    try
                    {
                        s = m_sendLocations.take();
                        if (TextUtils.isEmpty(s))
                        {
                            java.lang.Thread.sleep(m_config.getSendSleepTime());
                            continue;
                        }

                        boolean success = false;
                        for (int i = 0; i < m_config.getSendTryCnt(); ++i) {
                            /* if (s == "Start") {
                               Date time = new Date(java.lang.System.currentTimeMillis());
                               success = m_webServiceClient.StartRecording(m_config.getCarName(), time);
                           } else if (s == "Stop") {
                               Date time = new Date(java.lang.System.currentTimeMillis());
                               success = m_webServiceClient.StopRecording(m_config.getCarName(), time);
                           } else*/ {
                                success = SendTrackPoint(s);
                            }
                            if (success){
                                m_gpsDataSendSuccess++;
                                break;
                            }
                            else{
                                java.lang.Thread.sleep(m_config.getSendSleepTime());
                            }
                        }
                        if (!success){
                            m_gpsDataSendFail++;
                            m_sendLocations.put(s);
                        }
                    }
                    catch (java.lang.InterruptedException e) {
                        break;
                    }
                }
            }
        };
        m_threadSendGpsData.setDaemon(true);
        m_threadSendGpsData.start();
    }

    private synchronized void StopThreadSendGpsData(boolean abort) {
        if (m_threadSendGpsData != null && m_threadSendGpsData.isAlive()) {
            if (m_sendLocations.size() != 0) {
                if (!abort) {
                    int cnt = 0;
                    while (cnt < 10) {
                        try {
                            java.lang.Thread.sleep(1000);
                        } catch (InterruptedException ex) {
                        }
                        cnt++;
                    }
                }
            }
            m_threadSendGpsData.interrupt();
        }
        m_threadSendGpsData = null;
        m_sendLocations.clear();
    }

    private BlockingQueue<String> m_sendLocations;

    private boolean OffsetGpsTime(Location loc)
    {
        long nowTime = java.lang.System.currentTimeMillis();
        // samsung bug, 1Day pre
        if (loc.getTime() > nowTime)
            loc.setTime(loc.getTime() - 24 * 60 * 60 * 1000);
        if (nowTime - loc.getTime() > 10 * 60 * 1000)
            return false;
        return true;
    }
    public void sendTrackPointData(Location location, String action) {
        //StartThreadSendGpsData();
        if (!OffsetGpsTime(location))
            return;

        String gpsData;
        if (action == null)
        {
            gpsData = LocationToString(location);
        }
        else
        {
            gpsData = LocationActionToString(location, action);
        }
        m_gpsDataAll++;

        if (m_sendLocations.size() >= m_config.getSendMsgQueCnt()) {
            m_sendLocations.poll();
            m_gpsDataDrop++;
        }

        m_sendLocations.offer(gpsData);

        //Log.d(TAG, "ServiceUtils:gpsData Queue count is " + Integer.toString(cnt));
    }

    private boolean SendTrackPoint(String gpsData) {
        Log.d(TAG, "ServiceUtils:prepare to Sending GpsData");
        boolean ret = m_webServiceClient.SendTrackPoint(m_config.getCarName(), gpsData);
        Log.i(TAG, "ServiceUtils:SendTrackPoint");

        return ret;
    }

    public void sendTrackData(String filePath) {
        String output = "";
        File file = new File(filePath);

        if (file.exists()) {
            if (file.isFile()) {
                try {
                    BufferedReader input = new BufferedReader(new FileReader(file));
                    StringBuffer buffer = new StringBuffer();
                    String text;

                    while ((text = input.readLine()) != null)
                        buffer.append(text + '\n');

                    output = buffer.toString();
                } catch (IOException e) {
                    SystemUtils.processException(e);
                }
            }
        }

        if (output != "") {
            m_webServiceClient.SendTrack(m_config.getCarName(), output);
        }
    }

    public String getDebugStat() {
        StringBuilder sb = new StringBuilder();
        sb.append("GpsData: \n");
        sb.append("\tAll:");
        sb.append(Integer.toString(m_gpsDataAll));
        sb.append("\r\n");
        sb.append("\tDrop:");
        sb.append(Integer.toString(m_gpsDataDrop));
        sb.append("\r\n");
        sb.append("\tSendSuccess:");
        sb.append(Integer.toString(m_gpsDataSendSuccess));
        sb.append("\r\n");
        sb.append("\tSendFail:");
        sb.append(Integer.toString(m_gpsDataSendFail));
        sb.append("\r\n");
        sb.append("\tQueue:");
        sb.append(Integer.toString(getGpsDataQueueCount()));
        sb.append("\r\n");
        return sb.toString();
    }
}
