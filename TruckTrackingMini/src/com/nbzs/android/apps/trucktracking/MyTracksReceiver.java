package com.nbzs.android.apps.trucktracking;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * A receiver to receive MyTracks notifications.
 * 
 * @author Jimmy Shih
 */
public class MyTracksReceiver extends BroadcastReceiver {

  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();
    long trackId = intent.getLongExtra(context.getString(R.string.track_id_broadcast_extra), -1L);
    Toast.makeText(context, action + " " + trackId, Toast.LENGTH_LONG).show();
  }
}
