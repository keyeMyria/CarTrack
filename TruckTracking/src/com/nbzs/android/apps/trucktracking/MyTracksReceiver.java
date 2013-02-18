package com.nbzs.android.apps.trucktracking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;

public class MyTracksReceiver extends BroadcastReceiver {
//    private ServiceProcess m_mywebLocationProcess;
//    private MyTracksProviderUtils myTracksProviderUtils;
//
//    public void onStart()
//    {
//        myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);
//        m_mywebLocationProcess = ServiceProcess.Get(this);
//    }

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    long trackId = intent.getLongExtra(context.getString(R.string.track_id_broadcast_extra), -1L);
    Toast.makeText(context, action + " " + trackId, Toast.LENGTH_LONG).show();

//      if (action == "Start")
//      {
//          new java.lang.Thread() {
//              @Override
//              public void run() {
//                  m_mywebLocationProcess.StartRecording();
//              }
//          }.start();
//      }
//      else if (action == "Stop")
//      {
//          new java.lang.Thread() {
//              @Override
//              public void run() {
//                  m_mywebLocationProcess.StopRecording();
//              }
//          }.start();
//      }
//      else  if (action == "TrackPoint")
//      {
//          m_mywebLocationProcess.SendTrackPointData(location);
//      }
  }
}
