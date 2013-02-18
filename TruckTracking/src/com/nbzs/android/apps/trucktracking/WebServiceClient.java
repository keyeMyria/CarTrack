package com.nbzs.android.apps.trucktracking;

import java.io.*;
import java.util.Date;

import java.text.SimpleDateFormat;

import android.content.Context;
import android.graphics.Point;
import android.text.TextUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.entity.StringEntity;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.BasicHttpParams;

import org.json.JSONObject;

import android.util.Log;
import org.osmdroid.tileprovider.modules.INetworkAvailablityCheck;
import org.osmdroid.tileprovider.modules.NetworkAvailabliltyCheck;

public class WebServiceClient {
    private final INetworkAvailablityCheck mNetworkAvailablityCheck;

    public WebServiceClient(Context context) {
        mNetworkAvailablityCheck = new NetworkAvailabliltyCheck(context);
    }

    private String m_serverAddr;
    private final String m_serviceAddr = "/CarTrackService/GpsDataService.svc";

    public void SetServer(String serverAddr) {
        m_serverAddr = serverAddr;
    }

    SimpleDateFormat m_dataFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private String PostHttp(final String url, final StringEntity postData) {
        HttpParams httpParameters = new BasicHttpParams();
// Set the timeout in milliseconds until a connection is established.
        int timeoutConnection = 60000;
        HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);
// Set the default socket timeout (SO_TIMEOUT)
// in milliseconds which is the timeout for waiting for data.
        int timeoutSocket = 60000;
        HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
        String newUrl = m_serverAddr + m_serviceAddr + url;
        HttpClient httpClient = new DefaultHttpClient(httpParameters);

        Log.d(Constants.TAG, "post web " + newUrl);
        postData.setContentType("application/json");
        HttpPost httpPost = new HttpPost(newUrl);
        httpPost.setEntity(postData);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("content-type", "application/json");

        // Execute the request
        ResponseHandler<String> handler = new BasicResponseHandler();
        try {
            String result = httpClient.execute(httpPost, handler);
            Log.d(Constants.TAG, "post web result " + result);
            return result;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String GetHttp(final String url) {
        final String safeUrl;
        /*try
        {
            safeUrl = URLEncoder.encode(url, "UTF-8");
        }
        catch (UnsupportedEncodingException ex)
        {
            Log.d(Constants.TAG, "UnsupportedEncodingException: " + ex.getMessage());
            return null;
        }*/
        String newUrl = m_serverAddr + m_serviceAddr + url;
        HttpClient httpClient = new DefaultHttpClient();

        Log.d(Constants.TAG, "get web " + newUrl);
        // Prepare a request object
        HttpGet httpget = new HttpGet(newUrl);

        // Execute the request
        HttpResponse response;
        ResponseHandler<String> handler = new BasicResponseHandler();
        try {
            String result = httpClient.execute(httpget, handler);
            Log.d(Constants.TAG, "get web result " + result);
            return result;
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean CheckService() {
        if (m_serverAddr == null || TextUtils.isEmpty(m_serverAddr))
            return false;
        if (mNetworkAvailablityCheck == null || !mNetworkAvailablityCheck.getNetworkAvailable())
            return false;
        return true;
    }

    public boolean StartRecording(String carName, Date date) {
        if (!CheckService())
            return false;

        String url = "/" + carName + "/StartRecording";


        try {
            JSONObject jo = new JSONObject();
            jo.put("startTime", m_dataFormat.format(date));
            StringEntity e = new StringEntity(jo.toString(), "UTF-8");

            String result = PostHttp(url, e);
            boolean ret = Boolean.parseBoolean(result);
            return ret;
        } catch (Exception ex) {
            Log.d(Constants.TAG, "StartRecording error: " + ex.getMessage());
        }
        return false;
    }

    public boolean StopRecording(String carName, Date date) {
        if (!CheckService())
            return false;

        String url = "/" + carName + "/StopRecording";
        try {
            JSONObject jo = new JSONObject();
            jo.put("endTime", m_dataFormat.format(date));
            StringEntity e = new StringEntity(jo.toString(), "UTF-8");

            String result = PostHttp(url, e);
            boolean ret = Boolean.parseBoolean(result);
            return ret;
        } catch (Exception ex) {
            Log.d(Constants.TAG, "StopRecording error: " + ex.getMessage());
        }
        return false;
    }

    public boolean SendWayPoint(String carName, String gpsData, String actionData) {
        if (!CheckService())
            return false;

        String url = "/" + carName + "/SendWayPoint";
        try {
            JSONObject jo = new JSONObject();
            jo.put("gpsData", gpsData);
            jo.put("actionData", actionData);
            StringEntity e = new StringEntity(jo.toString(), "UTF-8");

            String result = PostHttp(url, e);
            boolean ret = Boolean.parseBoolean(result);
            return ret;
        } catch (Exception ex) {
            Log.d(Constants.TAG, "SendWayPoint error: " + ex.getMessage());
        }
        return false;
    }

    public boolean SendTrackPoint(String carName, String gpsData) {
        if (!CheckService())
            return false;

        String url = "/" + carName + "/SendTrackPoint";
        try {
            JSONObject jo = new JSONObject();
            jo.put("gpsData", gpsData);
            StringEntity e = new StringEntity(jo.toString(), "UTF-8");

            String result = PostHttp(url, e);
            boolean ret = Boolean.parseBoolean(result);
            return ret;
        } catch (Exception ex) {
            Log.d(Constants.TAG, "SendTrackPoint error: " + ex.getMessage());
        }
        return false;
    }

    public Point GetOffset(final int key) {
        if (!CheckService())
            return null;

        String url = "/GetOffset?k=" + key;
        try {
            String result = GetHttp(url);
            JSONObject json = new JSONObject(result);
            int latOffset = json.getInt("dLat");
            int lonOffset = json.getInt("dLon");
            return new Point(lonOffset, latOffset);
        } catch (Exception ex) {
            Log.d(Constants.TAG, "GetOffset error: " + ex.getMessage());
        }
        return null;
    }

    public boolean SendTrack(String carName, String gpxData) {
        if (!CheckService())
            return false;

        String url = "/" + carName + "/SendTrackData";
        try {
            JSONObject jo = new JSONObject();
            jo.put("gpxData", gpxData);
            StringEntity e = new StringEntity(jo.toString(), "UTF-8");

            String result = PostHttp(url, e);
            boolean ret = Boolean.parseBoolean(result);
            return ret;
        } catch (Exception ex) {
            Log.d(Constants.TAG, "SendTrackPoint error: " + ex.getMessage());
        }
        return false;
    }
}
