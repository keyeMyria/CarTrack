package com.nbzs.android.apps.trucktracking;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.text.SimpleDateFormat;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.Log;

import com.nbzs.android.apps.SystemUtils;
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

public class CarTrackingWebServiceClient extends com.nbzs.android.apps.WebServiceClient {

    public CarTrackingWebServiceClient(Context context, String serverAddr)
        {
            super(context, serverAddr, "/CarTrackService/ZkzxDataService.svc");
        }

    //private final String m_serviceAddr = "/CarTrackService/GpsDataService.svc";
    SimpleDateFormat m_dataFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    public boolean StartRecording(String carName, Date date) {
        if (!CheckService())
            return false;
        if (TextUtils.isEmpty(carName))
            return false;

        String url = "/" + carName + "/StartRecording";

        try {
            JSONObject jo = new JSONObject();
            jo.put("startTime", m_dataFormat.format(date));

            String result = PostHttp(url, jo.toString(), true);
            Log.i(Constants.TAG, "StartRecording");
            return result != null;
        } catch (Exception e) {
            SystemUtils.processException(e);
            return false;
        }
    }

    public boolean StopRecording(String carName, Date date) {
        if (!CheckService())
            return false;
        if (TextUtils.isEmpty(carName))
            return false;

        String url = "/" + carName + "/StopRecording";
        try {
            JSONObject jo = new JSONObject();
            jo.put("endTime", m_dataFormat.format(date));

            String result = PostHttp(url, jo.toString(), true);
            Log.i(Constants.TAG, "StopRecording");
            return result != null;
        } catch (Exception e) {
            SystemUtils.processException(e);
            return false;
        }
    }

    public boolean SendWayPoint(String carName, String gpsData, String actionData) {
        if (!CheckService())
            return false;
        if (TextUtils.isEmpty(carName))
            return false;

        String url = "/" + carName + "/SendWayPoint";
        try {
            JSONObject jo = new JSONObject();
            jo.put("gpsData", gpsData);
            jo.put("actionData", actionData);

            String result = PostHttp(url, jo.toString(), false);
            Log.i(Constants.TAG, "SendWayPoint");
            return result != null;
        } catch (Exception e) {
            SystemUtils.processException(e);
            return false;
        }
    }

    public boolean SendTrackPoint(String carName, String gpsData) {
        if (!CheckService())
            return false;
        if (TextUtils.isEmpty(carName))
            return false;

        String url = "/" + carName + "/SendTrackPoint";
        try {
            JSONObject jo = new JSONObject();
            jo.put("gpsData", gpsData);

            String result = PostHttp(url, jo.toString(), false);
            Log.i(Constants.TAG, "SendTrackPoint");
            return result != null;
        } catch (Exception e) {
            SystemUtils.processException(e);
            return false;
        }
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
        } catch (Exception e) {
            SystemUtils.processException(e);
        }
        return null;
    }

    public boolean SendTrack(String carName, String gpxData) {
        if (!CheckService())
            return false;
        if (TextUtils.isEmpty(carName))
            return false;

        String url = "/" + carName + "/SendTrackData";
        try {
            JSONObject jo = new JSONObject();
            jo.put("gpxData", gpxData);

            String result = PostHttp(url, jo.toString(), true);
            Log.i(Constants.TAG, "SendTrack");
            return result != null;
        } catch (Exception e) {
            SystemUtils.processException(e);
            return false;
        }
    }

    /*public String GetCurrentWorkId(String carName) {
        if (!CheckService())
            return null;
        if (TextUtils.isEmpty(carName))
            return null;

        Log.i(Constants.TAG, "GetCurrentWorkId");
        String url = "/GetCurrentWorkerIdByTruckId/" + carName;
        try {
            String result = GetHttp(url);
            if (result == null)
                return null;
            JSONObject jData = new JSONObject(result);
            result = jData.getString("Value");

            return result;
        } catch (Exception e) {
            SystemUtils.processException(e);
            return null;
        }
    }*/
    public String GetNextWorkId(String carName, int idx) {
        if (!CheckService())
            return null;
        if (TextUtils.isEmpty(carName))
            return null;

        Log.i(Constants.TAG, "GetCurrentWorkId");
        String url = "/GetWorkerIdByTruckId/" + carName + "/" + Integer.toString(idx);
        try {
            String result = GetHttp(url);
            if (result == null)
                return null;
            JSONObject jData = new JSONObject(result);
            result = jData.getString("Value");

            return result;
        } catch (Exception e) {
            SystemUtils.processException(e);
            return null;
        }
    }
    public String GetWorkSequence(String workId) throws org.json.JSONException {
        if (!CheckService())
            return null;
        if (TextUtils.isEmpty(workId))
            return null;

        Log.i(Constants.TAG, "GetWorkSequence");
        String url = "/GetWorkSequence/" + workId;
        String result = GetHttp(url);
        JSONObject jData = new JSONObject(result);
        result = jData.getString("Value");

        return result;
    }

    public String GetWorkDetail(String workId, Integer actionIdx) throws org.json.JSONException {
        if (!CheckService())
            return null;
        if (TextUtils.isEmpty(workId))
            return null;

        Log.i(Constants.TAG, "GetWorkDetails");
        String url = "/GetWorkDetails/" + workId + "/" + Integer.toString(actionIdx);
        String result = GetHttp(url);
        JSONObject jData = new JSONObject(result);
        result = jData.getString("Value");
        return result;
    }

    public String SendTruckState(String workId, String state) throws org.json.JSONException {
        if (!CheckService())
            return null;
        if (TextUtils.isEmpty(workId))
            return null;

        Log.i(Constants.TAG, "SendTruckState");
        String url = "/SendTruckState/" + workId + "/" + state;
        String result = GetHttp(url);
        JSONObject jData = new JSONObject(result);
        result = jData.getString("Value");
        return result;
    }

    public int GetAppVersionCode() throws org.json.JSONException {
        if (!CheckService())
            return 0;

        Log.i(Constants.TAG, "GetAppVersionCode");
        String url = "/GetAppVersionCode";
        String result = GetHttp(url);
        JSONObject jData = new JSONObject(result);
        result = jData.getString("Value");
        return Integer.parseInt(result);
    }
}
