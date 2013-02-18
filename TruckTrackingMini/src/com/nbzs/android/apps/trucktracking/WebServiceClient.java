package com.nbzs.android.apps.trucktracking;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.text.SimpleDateFormat;

import android.content.Context;
import android.graphics.Point;
import android.text.TextUtils;
import android.util.Log;

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

public class WebServiceClient {
    private Context m_ctx;
    public WebServiceClient(Context context, String serverAddr) {
        m_ctx = context;
        m_serverAddr = serverAddr;
        disableConnectionReuseIfNecessary();
    }
    public void SetServer(String serverAddr)
    {
        m_serverAddr = serverAddr;
    }
    private String m_serverAddr;
    //private final String m_serviceAddr = "/CarTrackService/GpsDataService.svc";
    private final String m_serviceAddr = "/CarTrackService/ZkzxDataService.svc";

    SimpleDateFormat m_dataFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private final int defaultTimeout = 30000;

    private void disableConnectionReuseIfNecessary() {
        // HTTP connection reuse which was buggy pre-froyo
        if (Integer.parseInt(android.os.Build.VERSION.SDK) < android.os.Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }
    private String readStream(InputStream in) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(in),1000);
        for (String line = r.readLine(); line != null; line =r.readLine()){
            sb.append(line);
        }
        in.close();
        return sb.toString();
    }
    private String PostHttp1(final String address, final String data, Boolean getResult)
    {
        String newUrl = m_serverAddr + m_serviceAddr + address;
        Log.d(Constants.TAG, "post web " + newUrl + "," + data.toString());
        HttpURLConnection urlConnection = null;

        try {
            URL url = new URL(newUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setChunkedStreamingMode(0);
            urlConnection.setRequestProperty("Content-Type", "application/json");

            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
            //OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream());
            out.write(data.getBytes());
            out.flush();

            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            String result = readStream(in);
            Log.d(Constants.TAG, "get web result " + result);
            return result;
        }
        catch (Exception e) {
            SystemUtils.processException(e);
            return null;
        }
        finally {
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
        }

    }
    private String GetHttp1(final String address) {
        String newUrl = m_serverAddr + m_serviceAddr + address;
        Log.d(Constants.TAG, "get web " + newUrl);

        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(newUrl);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Accept-Encoding", null);
            urlConnection.setRequestProperty("User-Agent", null);
            urlConnection.setRequestProperty("Host", null);
            InputStream in = new BufferedInputStream(urlConnection.getInputStream());
            String result = readStream(in);
            Log.d(Constants.TAG, "get web result " + result);
            return result;
        }
        catch (Exception e) {
            SystemUtils.processException(e);
            return null;
        }
        finally {
            if (urlConnection != null)
            {
                urlConnection.disconnect();
            }
        }
    }

    private String PostHttp(final String url, final String data, Boolean getResult) {
        String newUrl = m_serverAddr + m_serviceAddr + url;
        Log.d(Constants.TAG, "post web " + newUrl + "," + data.toString());

        HttpParams httpParameters = new BasicHttpParams();
// Set the timeout in milliseconds until a connection is established.
        HttpConnectionParams.setConnectionTimeout(httpParameters, defaultTimeout);
// Set the default socket timeout (SO_TIMEOUT)
// in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, defaultTimeout);
        HttpClient httpClient = new DefaultHttpClient(httpParameters);

        // Execute the request
        ResponseHandler<String> handler;
        try {
            StringEntity postData = new StringEntity(data.toString(), "UTF-8");
            postData.setContentType("application/json");
            HttpPost httpPost = new HttpPost(newUrl);
            httpPost.setEntity(postData);
            // no need after setContentType in postData
            //httpPost.setHeader("Accept", "application/json");
            //httpPost.setHeader("content-type", "application/json");
            //httpPost.setHeader("Cache-Control", "no-cache");

            String result = "";
            if (getResult)
            {
                handler = new BasicResponseHandler();
                httpClient.execute(httpPost, handler);
            }
            else
            {
                HttpResponse httpResponse = httpClient.execute(httpPost);
            }
            Log.d(Constants.TAG, "post web result " + result);
            return result;
        } catch (ClientProtocolException e) {
            SystemUtils.processException(e);
        } catch (IOException e) {
            SystemUtils.processException(e);
        }
        catch (Exception e) {
            SystemUtils.processException(e);
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
        HttpParams httpParameters = new BasicHttpParams();
// Set the timeout in milliseconds until a connection is established.
        HttpConnectionParams.setConnectionTimeout(httpParameters, defaultTimeout);
// Set the default socket timeout (SO_TIMEOUT)
// in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, defaultTimeout);
        HttpClient httpClient = new DefaultHttpClient(httpParameters);

        String newUrl = m_serverAddr + m_serviceAddr + url;

        Log.d(Constants.TAG, "get web " + newUrl);
        // Prepare a request object
        HttpGet httpget = new HttpGet(newUrl);
        //httpget.setHeader("Cache-Control", "no-cache");

        // Execute the request
        HttpResponse response;
        ResponseHandler<String> handler = new BasicResponseHandler();
        try {
            String result = httpClient.execute(httpget, handler);
            Log.d(Constants.TAG, "get web result " + result);
            return result;
        } catch (ClientProtocolException e) {
            SystemUtils.processException(e);
        } catch (IOException e) {
            SystemUtils.processException(e);
        }
        return null;
    }

    private boolean CheckService() {
        if (m_serverAddr == null || TextUtils.isEmpty(m_serverAddr))
            return false;
        return SystemUtils.isOnline(m_ctx);
    }

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
