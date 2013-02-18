package com.nbzs.android.apps.trucktracking;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.nbzs.android.apps.SystemUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 12-11-5
 * Time: 下午12:13
 * To change this template use File | Settings | File Templates.
 */
public class MainActivity extends FragmentActivity {
    private static final String TAG = Constants.TAG;

    private AppPreferences m_config;
    private CarTrackingWebServiceClient m_webSerivceClient;
    SendTrackPointContext sendTrackPointContext;

    private String m_currentWorkId;
    private final Timer timer = new Timer();

    @Override
    protected  void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        AppPreferences.Initialize(this);
        m_config = AppPreferences.getInstance();
        sendTrackPointContext = new SendTrackPointContext(this);
        sendTrackPointContext.onStart();

        OnTruckIdChanged();

        String serverUrl = m_config.getServerUrl();
        if (serverUrl != null && !TextUtils.isEmpty(serverUrl)) {
            m_webSerivceClient = new CarTrackingWebServiceClient(this, serverUrl);
        }

        Button b = ((Button)findViewById(R.id.doWorkStatusRest));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendTruckState("途中休息");
            }
        });

        b = ((Button)findViewById(R.id.doWorkStatusTrafficJam));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendTruckState("堵车");
            }
        });

        b = ((Button)findViewById(R.id.doWorkStatusTruckRepair));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SendTruckState("故障处理");
            }
        });

        b = ((Button)findViewById(R.id.getWorkCurrent));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetCurrentWork();
            }
        });

        b = ((Button)findViewById(R.id.getWorkNext));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GetNextWork();
            }
        });

        ClearWorkActions();

        CheckForUpdate();

        timer.scheduleAtFixedRate(new checkForNewWorkTask(), 0, 3600 * 1000);

        //m_webSerivceClient.SendTrackPoint("NB00101", "2012-01-01T01:01:01,20,20,0,0,0,0");
    }
    private void ClearWorkActions()
    {
        ((TextView)findViewById(R.id.workTitle)).setText("");
        LinearLayout linearLayout  = (LinearLayout) findViewById(R.id.workActions);
        linearLayout.removeAllViews();
    }
    private void CheckForUpdate()
    {
        try
        {
            int nowVersion = SystemUtils.getVersionCode(this);
            int serverVersion =  m_webSerivceClient.GetAppVersionCode();
            if (serverVersion > nowVersion)
            {
                DownloadNewApp();
            }
        }
        catch (Exception ex)
        {
            SystemUtils.processException(ex);
        }
    }
    private void DownloadNewApp()
    {
        final Context context = this;
        (new AsyncTask<String, File, Integer>() {
            private ProgressDialog dialog;
            @Override
            protected void onPreExecute() {
                this.dialog = new ProgressDialog(context);
                this.dialog.setMessage(getResources().getString(R.string.downloadapp));
                dialog.setIndeterminate(true);
                dialog.setCancelable(false);
                this.dialog.show();
            }
            @Override
            protected Integer doInBackground(String... urls) {
                int count = urls.length;
                for (int i = 0; i < count; i++) {
                    File file = SystemUtils.downloadFile(urls[i]);
                    publishProgress(file);
                    // Escape early if cancel() is called
                    if (isCancelled()) break;
                }
                return 1;
            }
            @Override
            protected void onProgressUpdate(File... file) {
                //setProgressPercent(progress[0]);
                if (file[0] != null)
                {
                    SystemUtils.install(file[0], context);
                }
            }
            @Override
            protected void onPostExecute(Integer result) {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
            }
        }).execute(m_config.getServerUrl() + "/CarTrackService/TruckTrackingMini.apk");
    }

    private class checkForNewWorkTask extends TimerTask
    {
        public void run()
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    GetCurrentWork();
                }
            });
        }
    }

    private CarTrackServiceSendTruckStateTask m_carTrackServiceSendTruckStateTask = new CarTrackServiceSendTruckStateTask();
    private Button m_carTrackServiceSendTruckStateTaskButton = null;
    private void SendTruckState(String state)
    {
        SendTruckState(state, null);
    }
    private void SendTruckState(String state, Button button)
    {
        m_carTrackServiceSendTruckStateTaskButton = button;
        if (m_webSerivceClient == null)
            return;
        if (m_currentWorkId == null)
            return;

        if (m_carTrackServiceSendTruckStateTask.getStatus() == AsyncTask.Status.RUNNING)
            return;

        if (m_carTrackServiceSendTruckStateTask.getStatus() == AsyncTask.Status.FINISHED)
            m_carTrackServiceSendTruckStateTask = new CarTrackServiceSendTruckStateTask();
        m_carTrackServiceSendTruckStateTask.execute(state);
    }
    private class CarTrackServiceSendTruckStateTask extends AsyncTask<String, String, Boolean> {
        @Override
        protected Boolean doInBackground(String... status) {
            if (status.length == 0)
                return null;
            String s = null;
            try
            {
                s = m_webSerivceClient.SendTruckState(m_currentWorkId, URLEncoder.encode(status[0]));
            }
            catch (Exception ex)
            {
                SystemUtils.processException(ex);
            }
            return !TextUtils.isEmpty(s) && s.equalsIgnoreCase("Ok");
        }

        private ProgressDialog dialog;
        @Override
        protected void onPreExecute() {
            this.dialog = new ProgressDialog(MainActivity.this);
            this.dialog.setMessage(getResources().getString(R.string.sending));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            this.dialog.show();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if (result)
            {
                Toast.makeText(MainActivity.this, R.string.have_be_sent, Toast.LENGTH_SHORT).show();
            }
            else
            {
                Toast.makeText(MainActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
            }
            if (m_carTrackServiceSendTruckStateTaskButton != null)
            {
                if (m_carTrackServiceSendTruckStateTaskButton.getId() == R.id.workActionEnd)
                {
                    GetCurrentWork();
                }
                else
                {
                    //m_carTrackServiceSendTruckStateTaskButton.setEnabled(!result);
                    GetCurrentWork();
                }
            }
        }
    }

    @Override
    protected  void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        if (TextUtils.isEmpty(m_config.getCarName()))
        {
            SystemUtils.startActivityForResult(this, SettingsActivity.class, REQ_CODE_SETTINGS);
            return;
        }
        GetCurrentWork();
    }
    private int m_currentWorkIdx = 0;
    private void GetNextWork()
    {
        m_currentWorkIdx++;
        GetWorkInfos(m_currentWorkIdx);
    }
    private void GetCurrentWork()
    {
        m_currentWorkIdx = 0;
        GetWorkInfos(m_currentWorkIdx);
    }
    private CarTrackServiceGetWorkTask m_carTrackServiceGetWorkTask = new CarTrackServiceGetWorkTask();
    private void GetWorkInfos(int workIdx)
    {
        m_currentWorkId = null;
        if (m_webSerivceClient == null)
            return;
        if (m_carTrackServiceGetWorkTask.getStatus() == AsyncTask.Status.RUNNING)
            return;

        if (m_carTrackServiceGetWorkTask.getStatus() == AsyncTask.Status.FINISHED)
            m_carTrackServiceGetWorkTask = new CarTrackServiceGetWorkTask();
        m_carTrackServiceGetWorkTask.execute(workIdx);
    }

    private class CarTrackServiceGetWorkTask extends AsyncTask<Integer, String, String> {
        @Override
        protected String doInBackground(Integer... workIdxs) {
            if (workIdxs.length == 0)
                return null;

            String currentWorkId = m_webSerivceClient.GetNextWorkId(m_config.getCarName(), workIdxs[0]);
            publishProgress(currentWorkId);
            if (currentWorkId != null)
            {
                m_currentWorkId = currentWorkId;
            }
            else
            {
                return null;
            }
            if (isCancelled())
                return null;
            if (!TextUtils.isEmpty(m_currentWorkId))
            {
                try
                {
                    String s = m_webSerivceClient.GetWorkSequence(m_currentWorkId);
                    return s;
                }
                catch (Exception ex)
                {
                    SystemUtils.processException(ex);
                    return null;
                }
            }
            else
            {
                return "";
            }
        }
        @Override
        protected void onProgressUpdate(String... progress) {
            UpdateUIStep1(progress[0]);
        }

        private ProgressDialog dialog;
        @Override
        protected void onPreExecute() {
            this.dialog = new ProgressDialog(MainActivity.this);
            this.dialog.setMessage(getResources().getString(R.string.DownloadWork));
            dialog.setIndeterminate(true);
            dialog.setCancelable(false);
            this.dialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            UpdateUIStep2(result);

            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (m_currentWorkIdx > 0 && TextUtils.isEmpty(m_currentWorkId))
                m_currentWorkIdx--;
        }

        private void UpdateUIStep1(String workId)
        {
            // empty: no work; null: error
            boolean b = TextUtils.isEmpty(workId);
            ((Button)findViewById(R.id.doWorkStatusRest)).setEnabled(!b);
            ((Button)findViewById(R.id.doWorkStatusTrafficJam)).setEnabled(!b);
            ((Button)findViewById(R.id.doWorkStatusTruckRepair)).setEnabled(!b);
            ((Button)findViewById(R.id.getWorkNext)).setEnabled(!b);

            if (workId == null)
            {
                Toast.makeText(MainActivity.this, R.string.network_error, Toast.LENGTH_SHORT).show();
            }
            else if (b)
            {
                Toast.makeText(MainActivity.this, R.string.no_work, Toast.LENGTH_SHORT).show();
            }

            if (m_currentWorkIdx == 0 && workId != null)
            {
                if (b)
                {
                    sendTrackPointContext.stopRecordingService();
                }
                else
                {
                    sendTrackPointContext.startRecordingService();
                }
            }
        }

        private void UpdateUIStep2(String s)
        {
            if (s == null)
                return;
            ClearWorkActions();
            if (TextUtils.isEmpty(s))
                return;
            LinearLayout linearLayout  = (LinearLayout) findViewById(R.id.workActions);
            try
            {
                //LinearLayout linearLayout  = (LinearLayout) findViewById(R.id.workActions);

                JSONObject jData = new JSONObject(s);
                String title = jData.getString("Title");
                ((TextView)findViewById(R.id.workTitle)).setText(title);

                JSONArray jArrayActions = jData.getJSONArray("Actions");
                int actionIdx = (int)jData.getInt("ActionIdx");
                int actionIdxIdx = (int)jData.getInt("ActionIdxIdx");

                JSONArray jArrayTaskIdxs = jData.getJSONArray("TaskIdxs");
                JSONArray xianghaos = jData.getJSONArray("Xianghao");
                JSONArray fengzhihaos = jData.getJSONArray("Fengzihao");

                for (int i = 0; i < jArrayActions.length(); i++) {
                    String actionTitle = jArrayActions.getString(i);

                    //int defaultHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 25, getResources().getDisplayMetrics());
                    {
                        LinearLayout v = (LinearLayout)LayoutInflater.from(MainActivity.this).inflate(R.layout.work_action, linearLayout, false);
                        ///ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        //p.height = defaultHeight;
                        //v.setLayoutParams(p);
                        ((TextView)v.findViewById(R.id.workActionTitle)).setText(actionTitle);
                        linearLayout.addView(v);

                        Button b = ((Button)v.findViewById(R.id.workActionDetail));
                        b.setTag(i);
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    int idx = (Integer)v.getTag();
                                    String s = m_webSerivceClient.GetWorkDetail(m_currentWorkId, idx);
                                    JSONObject jData = new JSONObject(s);
                                    DialogFragment dialog = new WorkActionDetailDialogFragment(jData);
                                    dialog.show(getSupportFragmentManager(), getResources().getString(R.string.WorkDetailLabel));
                                } catch (Exception e) {
                                    SystemUtils.processException(e);
                                }
                            }
                        });

                        b = ((Button)v.findViewById(R.id.workActionStart));
                        b.setTag(i);
                        b.setEnabled(i == actionIdx && actionIdxIdx != 0 && m_currentWorkIdx == 0);
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int idx = (Integer)v.getTag();
                                SendTruckState("动作-"  +Integer.toString(idx) + "-" + "0", (Button)v);
                            }
                        });

                        b =  ((Button)v.findViewById(R.id.workActionEnd));
                        b.setTag(i);
                        b.setEnabled(i == actionIdx && actionIdxIdx != 1 && m_currentWorkIdx == 0);
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int idx = (Integer)v.getTag();
                                SendTruckState("动作-"  +Integer.toString(idx) + "-" + "1", (Button)v);
                            }
                        });
                    }

                    if (title.contains("出口") && actionTitle.contains("堆场提箱"))
                    {
                        LinearLayout v = (LinearLayout)LayoutInflater.from(MainActivity.this).inflate(R.layout.work_action_send, linearLayout, false);
                        //ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        //p.height = defaultHeight;
                        //v.setLayoutParams(p);
                        ((TextView)v.findViewById(R.id.workActionSendTitle)).setText(R.string.input_xianghao);
                        linearLayout.addView(v);

                        final TextView txtSend =  ((TextView)v.findViewById(R.id.work_action_send_text));
                        Button b = ((Button)v.findViewById(R.id.work_action_send));
                        b.setTag(i);
                        //txtSend.setEnabled(i == actionIdx && m_currentWorkIdx == 0);
                        //b.setEnabled(i == actionIdx && m_currentWorkIdx == 0);
                        String sx = xianghaos.getString(jArrayTaskIdxs.getInt(i));
                        txtSend.setText(TextUtils.isEmpty(sx) ? "" : sx);
                        if (!TextUtils.isEmpty(sx) || m_currentWorkIdx != 0)
                        {
                            txtSend.setEnabled(false);
                            b.setEnabled(false);
                        }
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CharSequence txt = txtSend.getText();
                                if (TextUtils.isEmpty(txt))
                                    return;
                                int idx = (Integer)v.getTag();
                                SendTruckState("箱号-" + Integer.toString(idx) + "-" + txt, (Button)v);
                            }
                        });
                    }
                    if ((title.contains("出口") || title.contains("套箱")) && (actionTitle.contains("装货") && !(actionTitle.contains("带货"))))
                    {
                        LinearLayout v = (LinearLayout)LayoutInflater.from(MainActivity.this).inflate(R.layout.work_action_send, linearLayout, false);
                        //ViewGroup.LayoutParams p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        //p.height = defaultHeight;
                        //v.setLayoutParams(p);
                        ((TextView)v.findViewById(R.id.workActionSendTitle)).setText(R.string.input_fenhao);
                        linearLayout.addView(v);

                        final TextView txtSend =  ((TextView)v.findViewById(R.id.work_action_send_text));
                        Button b = ((Button)v.findViewById(R.id.work_action_send));
                        b.setTag(i);
                        //txtSend.setEnabled(i == actionIdx && m_currentWorkIdx == 0);
                        //b.setEnabled(i == actionIdx && m_currentWorkIdx == 0);
                        String sx = fengzhihaos.getString(jArrayTaskIdxs.getInt(i));
                        txtSend.setText(TextUtils.isEmpty(sx) ? "" : sx);
                        if (!TextUtils.isEmpty(sx) || m_currentWorkIdx != 0)
                        {
                            txtSend.setEnabled(false);
                            b.setEnabled(false);
                        }
                        b.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                CharSequence txt = txtSend.getText();
                                if (TextUtils.isEmpty(txt))
                                    return;
                                int idx = (Integer)v.getTag();
                                SendTruckState("封号-"  +Integer.toString(idx) + "-" + txt, (Button)v);
                            }
                        });
                    }
                }
            }
            catch (Exception e)
            {
                SystemUtils.processException(e);
            }
        }
    }


    /*@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item =menu.findItem(R.id.menu_recording);
        if (item.getTitle() == getResources().getString(R.string.startRecording))
        {
            item.setEnabled(!TextUtils.isEmpty(m_currentWorkId));
        }
        return super.onPrepareOptionsMenu(menu);
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    final static int REQ_CODE_SETTINGS = 1;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            /*case R.id.menu_recording:
                if (item.getTitle() == getResources().getString(R.string.startRecording))
                {
                    if (m_currentWorkId == null)
                    {
                        Toast.makeText(this, R.string.no_work, Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    Boolean b = sendTrackPointContext.startRecordingService();
                    if (b)
                    {
                        item.setTitle(getResources().getString(R.string.stopRecording));
                    }
                    return b;
                }
                else
                {
                    Boolean b = sendTrackPointContext.stopRecordingService();
                    if (b)
                    {
                        item.setTitle(getResources().getString(R.string.startRecording));
                    }
                    return b;
                }
                case R.id.menu_quit:
                finish();
                return true;*/
            case R.id.menu_settings:
                CheckForUpdate();
                return SystemUtils.startActivityForResult(this, SettingsActivity.class, REQ_CODE_SETTINGS);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        sendTrackPointContext.onStop();
        super.onDestroy();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode == REQ_CODE_SETTINGS){
            m_config.Update();
            OnTruckIdChanged();
            if (m_webSerivceClient != null)
            {
                m_webSerivceClient.SetServer(m_config.getServerUrl());
            }
        }
    }

    private void OnTruckIdChanged()
    {
        // ((TextView)findViewById(R.id.truckId)).setText(m_config.getCarName());
        this.setTitle(getResources().getString(R.string.app_name) + "(" + m_config.getCarName() + ")");
    }
}