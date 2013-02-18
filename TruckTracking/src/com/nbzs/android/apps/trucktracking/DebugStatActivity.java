package com.nbzs.android.apps.trucktracking;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.location.Location;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View.OnClickListener;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.widget.Toast;
import com.google.android.apps.mytracks.content.*;
import com.google.android.apps.mytracks.services.ITrackRecordingService;
import com.google.android.maps.GeoPoint;

import java.util.EnumSet;


/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 11-8-3
 * Time: 下午1:47
 * To change this template use File | Settings | File Templates.
 */
public class DebugStatActivity extends Activity {
    private static final String TAG = Constants.TAG;
    private MyTracksProviderUtils myTracksProviderUtils;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myTracksProviderUtils = MyTracksProviderUtils.Factory.get(this);

        setContentView(R.layout.my_debugstat);

        RefreshData();
    }

    private MenuItem refreshMenuItem;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        refreshMenuItem =
                menu.add(0, Constants.MENU_DEBUG_REFRESH, 0, R.string.refresh);
        refreshMenuItem.setIcon(R.drawable.menu_refresh);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Constants.MENU_DEBUG_REFRESH:
                RefreshData();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void RefreshData() {
        ServiceProcess lp = ServiceProcess.Get(this);
        ((TextView) findViewById(R.id.debugstat)).setText(lp.GetDebugStat());

        ((Button) findViewById(R.id.btnAction1)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                SendAction("提箱");
            }
        });
        ((Button) findViewById(R.id.btnAction2)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                SendAction("装货");
            }
        });
        ((Button) findViewById(R.id.btnAction3)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                SendAction("卸货");
            }
        });
        ((Button) findViewById(R.id.btnAction4)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                SendAction("还箱进港");
            }
        });

        final TextView txtActionSpecial = (TextView)findViewById(R.id.txtActionName);
        ((Button) findViewById(R.id.btnActionSpecial)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(android.view.View view) {
                CharSequence s = txtActionSpecial.getText();
                if (!TextUtils.isEmpty(s))
                {
                    SendAction(s.toString());
                }
            }
        });
    }

    private void SendAction(String actionData) {
        boolean ret = false;
        long wayPointId = insertWaypoint(actionData);
        if (wayPointId != -1)
        {
            ret = SendActionData(actionData);
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        if (ret) {
            builder.setTitle(R.string.success).setMessage("发送成功！").setPositiveButton("确定", null);
        } else {
            builder.setTitle(R.string.error).setMessage("发送失败！").setPositiveButton("确定", null);
        }
        builder.show();
    }

    private long insertWaypoint(String actionData) {
            Waypoint waypoint = new Waypoint();
            waypoint.setCategory("Marker");
            waypoint.setDescription(actionData);
            Uri uri = myTracksProviderUtils.insertWaypoint(waypoint);
            
            if (uri != null) {
                    Toast.makeText(this, R.string.success,
                            Toast.LENGTH_LONG).show();
            }

        return -1;
    }

    private boolean SendActionData(String actionData) {
        ServiceProcess lp = ServiceProcess.Get(this);
        return lp.SendWayPointData(myTracksProviderUtils.getLastLocation(), actionData);
    }
}