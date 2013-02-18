package com.nbzs.android.apps.trucktracking;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 11-7-27
 * Time: 下午2:43
 * To change this template use File | Settings | File Templates.
 */

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.Log;
import android.location.Location;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

public class ServiceProcess {
    private static final String TAG = Constants.TAG;

    private MyConfig m_config;

    public MyConfig getMyWebConfig() {
        return m_config;
    }

    private WebServiceClient m_carTrackService;

    public WebServiceClient getWebServiceClient() {
        return m_carTrackService;
    }

    private Context m_context;
    private ServiceProcess(Context context) {
        m_context = context;

        m_config = new MyConfig(context);

        m_carTrackService = new WebServiceClient(context);

        String serverUrl = m_config.getServerUrl();
        if (serverUrl != null && !TextUtils.isEmpty(serverUrl)) {
            m_carTrackService.SetServer(serverUrl);
        }


        //Log.d(TAG, "MyWeb track service address is " + m_carTrackService.Url);
    }
    private void TryCreateCamera()
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
    }
    private void StartRecordingVideo()
    {
        Intent startIntent = new Intent(m_context, CameraViewActivity.class);
        startIntent.putExtra("recording", 1);
        m_context.startActivity(startIntent);
         //TryCreateCamera();
        //m_myCamera.startRecording();
    }
    private void StopRecordingVideo()
    {
        /*Intent startIntent = new Intent(m_context, CameraViewActivity.class);
        startIntent.putExtra("recoding", 2);
        m_context.startActivity(startIntent);*/

        /*if (m_myCamera != null && m_myCamera.isRecording())
        {
            m_myCamera.stopRecording();
        }*/
    }
    private static Object m_lockCreator = new Object();

    public static ServiceProcess Get(Context context) {
        synchronized (m_lockCreator) {
            if (m_instance == null) {
                Log.d(TAG, "Create ServiceProcess Instance.");
                m_instance = new ServiceProcess(context);
            }
        }
        return m_instance;
    }

    private static ServiceProcess m_instance;

    private static SimpleDateFormat m_dataFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private static final NumberFormat LAT_LONG_FORMAT = new DecimalFormat("##,###.00000");
    private static final NumberFormat ALTITUDE_FORMAT = new DecimalFormat("###,###");
    private static final NumberFormat SPEED_FORMAT = new DecimalFormat("#,###,###.00");

    private MyCamera m_myCamera;

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

    private int m_tryCnt = 10;
    private int m_queueCnt = 50;

    private boolean m_startSendGpsData = false;
    public boolean StartRecording() {
        if (!m_config.getIsUseWebService())
            return true;

        resetStat();

        boolean ret = false;
        Date time = new Date(java.lang.System.currentTimeMillis());
        for (int i = 0; i < m_tryCnt; ++i) {
            try {
                Log.d(TAG, "MyWeb:Preparing to StartRecording");
                ret = m_carTrackService.StartRecording(m_config.getCarName(), time);
                if (ret) {
                    Log.d(TAG, "MyWeb:Success to send StartRecording");
                    break;
                } else {
                    Log.d(TAG, "MyWeb:Fail to send StartRecording");
                }
            } catch (Exception ex) {
                Log.e(TAG, "MyWeb:StartRecording error.\n" + ex.getMessage());
            }
        }

        if (ret)
        {
             StartThreadSendGpsData();

            if (m_config.getisStartVideoRecording())
            {
                StartRecordingVideo();
            }

             m_startSendGpsData = true;
            return true;
        }

        return false;
    }

    public boolean StopRecording() {
        //if (!m_config.getIsUseWebService())
        //    return;
        if (!m_startSendGpsData)
            return true;

        StopThreadSendGpsData();

        boolean ret = false;
        Date time = new Date(java.lang.System.currentTimeMillis());
        for (int i = 0; i < m_tryCnt; ++i) {

            try {
                Log.d(TAG, "MyWeb:Preparing to StopRecording");
                ret = m_carTrackService.StopRecording(m_config.getCarName(), time);
                if (ret) {
                    Log.d(TAG, "MyWeb:Success to send StopRecording");
                    break;
                } else {
                    Log.d(TAG, "MyWeb:Fail to send StopRecording");
                }
                break;
            } catch (Exception ex) {
                Log.e(TAG, "MyWeb:StopRecording error.\n" + ex.getMessage());
            }
        }

        if (m_config.getisStartVideoRecording())
        {
            StopRecordingVideo();
        }

         m_startSendGpsData = false;

        return ret;
    }
    private  String m_currentActionData = "Default";

    public boolean SendWayPointData(Location location, String actionData) {
        if (!m_config.getIsUseWebService())
            return true;
        if (location == null)
            return false;

        boolean ret = false;
        String gpsData = LocationToString(location);
        for (int i = 0; i < m_tryCnt; ++i) {
            try {
                Log.d(TAG, "MyWeb:Preparing to SendWayPoint");
                ret = m_carTrackService.SendWayPoint(m_config.getCarName(), gpsData, actionData != null ? actionData : m_currentActionData);
                if (ret) {
                    Log.d(TAG, "MyWeb:Success to SendWayPoint");
                    break;
                } else {
                    Log.d(TAG, "MyWeb:Fail to SendWayPoint");
                }
            } catch (Exception ex) {
                Log.e(TAG, "MyWeb:SendWayPoint error.\n" + ex.getMessage());
            }
        }

        if (ret)
        {
            return true;
        }

        return false;
    }

    java.lang.Thread m_threadSendGpsData;

    int m_gpsDataAll = 0;
    int m_gpsDataSendSuccess = 0;
    int m_gpsDataSendFail = 0;
    int m_gpsDataDrop = 0;
    int getGpsDataQueueCount()
    {
        return m_sendLocations.size();
    }
    private void resetStat() {
        m_gpsDataAll = 0;
        m_gpsDataSendSuccess = 0;
        m_gpsDataSendFail = 0;
        m_gpsDataDrop = 0;
    }

    private synchronized void StartThreadSendGpsData() {
        if (!m_config.getIsUseWebService())
            return;

        if (m_threadSendGpsData != null && m_threadSendGpsData.isAlive()
                && !m_threadSendGpsData.isInterrupted())
            return;
        StopThreadSendGpsData();

        m_threadSendGpsData = new java.lang.Thread() {
            @Override
            public void run() {
                while (!this.isInterrupted()) {
                    try {
                        String s = null;

                        s = m_sendLocations.take();

                        /*if (m_sendLocations.size() > 0) {

                        }*/

                        if (!TextUtils.isEmpty(s)) {
                            SendGpsData(s);
                            m_gpsDataSendSuccess++;
                        } else {
                            java.lang.Thread.sleep(1000);
                        }
                    } catch (java.lang.InterruptedException e) {
                        break;
                    }
                    /*catch (java.nio.channels.ClosedByInterruptException e) {
                        break;
                    }*/ catch (java.lang.SecurityException e) {
                        break;
                    }catch (Exception e) {
                        m_gpsDataSendFail++;
                    }
                }
            }
        };
        m_threadSendGpsData.setDaemon(true);
        m_threadSendGpsData.start();
    }

    private synchronized void StopThreadSendGpsData() {
        if (m_threadSendGpsData != null && m_threadSendGpsData.isAlive()) {
            m_threadSendGpsData.interrupt();
        }
        m_threadSendGpsData = null;
        m_sendLocations.clear();
    }

    private BlockingQueue<String> m_sendLocations = new ArrayBlockingQueue<String>(m_queueCnt, true);

    public void SendTrackPointData(Location location) {
        if (!m_config.getIsUseWebService())
            return;

        StartThreadSendGpsData();
        String gpsData = LocationToString(location);

        m_gpsDataAll++;

         if (m_sendLocations.size() >= m_queueCnt) {
            m_sendLocations.poll();
            m_gpsDataDrop++;
        }

        m_sendLocations.offer(gpsData);

        //Log.d(TAG, "MyWeb:gpsData Queue count is " + Integer.toString(cnt));
    }

    private void SendGpsData(String gpsData) {
        Log.d(TAG, "MyWeb:prepare to Sending GpsData");

        boolean ret = m_carTrackService.SendTrackPoint(m_config.getCarName(), gpsData);
        if (ret) {
            Log.d(TAG, "MyWeb:Success to send GpsData");
        } else {
            Log.d(TAG, "MyWeb:Fail to send GpsData");
        }
    }

    public void SendTrackData(String filePath) {
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
                } catch (IOException ioException) {
                    System.err.println("File Error!");

                }
            }
        }

        if (output != "") {
            boolean ret = m_carTrackService.SendTrack(m_config.getCarName(), output);
        }
    }

    public String GetDebugStat()
    {
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
