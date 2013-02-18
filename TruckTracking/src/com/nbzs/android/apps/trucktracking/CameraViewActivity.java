package com.nbzs.android.apps.trucktracking;

/**
 * Created by IntelliJ IDEA.
 * User: Zephyrrr
 * Date: 11-8-4
 * Time: 下午8:20
 * To change this template use File | Settings | File Templates.
 */

import java.io.IOException;
import java.util.EnumSet;


import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import com.google.android.apps.mytracks.content.Track;
import com.google.android.apps.mytracks.content.Waypoint;
import com.google.android.maps.GeoPoint;

public class CameraViewActivity extends Activity implements SurfaceHolder.Callback {
    //static final int FOTO_MODE = 0;
    private static final String TAG = Constants.TAG;

    SurfaceView mSurfaceView;
    SurfaceHolder mSurfaceHolder;
    Camera mCamera;
    boolean mPreviewRunning = false;
    private Context mContext = this;
    private MyCamera m_myCamera;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Log.d(TAG, "CameraViewActivity.onCreate");

        Bundle extras = getIntent().getExtras();

        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.camera_surface);

        mSurfaceView = (SurfaceView) findViewById(R.id.surface_camera);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    private MenuItem menuPicture, menuStartRecording, menuStopRecording;
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menuPicture = menu.add(0, Constants.MENU_CAMERA_PICTURE, 0, R.string.take_picture).setIcon(R.drawable.menu_picture);
        menuStartRecording = menu.add(0, Constants.MENU_CAMERA_RECORDING_START, 0, R.string.start_recording_video).setIcon(R.drawable.menu_record);
        menuStopRecording = menu.add(0, Constants.MENU_CAMERA_RECORDING_STOP, 0, R.string.stop_recording_video).setIcon(R.drawable.menu_stop);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menuPicture.setVisible(m_myCamera != null && !m_myCamera.isPicutring() && !m_myCamera.isRecording());
        menuStartRecording.setVisible(m_myCamera != null && !m_myCamera.isPicutring() && !m_myCamera.isRecording());
        menuStopRecording.setVisible(m_myCamera != null && !m_myCamera.isPicutring() && m_myCamera.isRecording());

       return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Constants.MENU_CAMERA_PICTURE:
                m_myCamera.takePicture();
                return true;
            case Constants.MENU_CAMERA_RECORDING_START:
                m_myCamera.startRecording();
                return true;
            case Constants.MENU_CAMERA_RECORDING_STOP:
                m_myCamera.stopRecording();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "CameraViewActivity.surfaceCreated");
       m_myCamera = MyCamera.Get(this, mSurfaceHolder);
        mCamera = m_myCamera.GetCamera();
        if (mCamera == null) {
            Toast.makeText(getApplicationContext(), "Camera not available!", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        Log.d(TAG, "CameraViewActivity.surfaceChanged");

        // XXX stopPreview() will crash if preview is not running
        if (mPreviewRunning) {
            mCamera.stopPreview();
        }

        Camera.Parameters p = mCamera.getParameters();
        p.setPreviewSize(w, h);
        //p.setPreviewFormat(PixelFormat.JPEG);
        mCamera.setParameters(p);
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
            mPreviewRunning = true;

            autoAction(true);

        } catch (IOException e) {
            Log.e(TAG, "Error in surfaceChanged: " + e.getMessage());
            e.printStackTrace();
        }

    }
    private void autoAction(boolean record)
    {
        int recording = getIntent().getIntExtra("recording", 0);
            if (record && recording == 1)
            {
                 m_myCamera.startRecording();
                //finish();
            }
            if (!record && recording == 2)
            {
                if (m_myCamera.isRecording())
                {
                    m_myCamera.stopRecording();
                }
                finish();
            }
    }
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "CameraViewActivity.surfaceDestroyed");
        mCamera.stopPreview();
        mPreviewRunning = false;
        m_myCamera.Destroy();
        m_myCamera = null;
    }


    protected void onResume() {
        Log.d(TAG, "CameraViewActivity.onResume");
        super.onResume();

        autoAction(false);
    }

    protected void onPause() {
        Log.d(TAG, "CameraViewActivity.onPause");
        super.onPause();
    }


}
