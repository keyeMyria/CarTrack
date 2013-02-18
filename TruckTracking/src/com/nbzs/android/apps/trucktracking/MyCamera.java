package com.nbzs.android.apps.trucktracking;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.media.ExifInterface;
import android.view.SurfaceHolder;
import com.google.android.maps.GeoPoint;
import com.google.android.apps.mytracks.content.MyTracksProviderUtils;
import com.google.android.apps.mytracks.content.Track;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 11-8-4
 * Time: 下午4:18
 * To change this template use File | Settings | File Templates.
 */
public class MyCamera {
    private String TAG = "MyCamera";

    private static  MyCamera s_instance;
    //private CameraViewActivity m_cameraView;
    private SurfaceHolder m_surfaceHolder;
    private Context m_context;
    public static MyCamera Get(Context context, SurfaceHolder surfaceHolder)
    {
        if (s_instance == null)
        {
             s_instance = new MyCamera(context);
            s_instance.m_context = context;
            s_instance.m_surfaceHolder = surfaceHolder;
        }
        return s_instance;
    }
    public void Destroy()
    {
        if (m_camera != null)
        {
            m_camera.release();
        }
        s_instance = null;
    }
    private Camera m_camera;
    public Camera GetCamera()
    {
        return m_camera;
    }
    public MyCamera(Context context)
    {
        CreateCamera();

        providerUtils = MyTracksProviderUtils.Factory.get(context);
    }
    private Camera CreateCamera()
    {
        m_camera = Camera.open();
		if (m_camera != null){
			Camera.Parameters params = m_camera.getParameters();
			m_camera.setParameters(params);
            m_camera.setDisplayOrientation(90);
		}
        return m_camera;
    }

    private boolean m_isRecording = false;
    private boolean  m_isPicture = false;
    public boolean isRecording()
    {
        return m_isRecording;
    }
    public boolean isPicutring()
    {
        return m_isPicture;
    }
    private MediaRecorder mediaRecorder;
	private final int maxDurationInMs = 20000;
	private final long maxFileSizeInBytes = 500000;
	private final int videoFramesPerSecond = 20;

    private MyTracksProviderUtils providerUtils;

    private File GetFile(boolean isPicture)
    {
         String trackName;
        long selectedTrackId = providerUtils.getLastTrackId();
                if  (selectedTrackId != -1)
                {
                     Track track = providerUtils.getTrack(selectedTrackId);
                    trackName = track.getName();
                }
                else
                {
                    trackName = "MyTrack";
                }
                //String directory = mFileUtils.buildExternalDirectoryPath(isPicture ? "Image" : "Video");
                //File dir = new File(directory);
                //String fileName = mFileUtils.buildUniqueFileName(dir, trackName, isPicture ? "jpg" : "mp4");

                    //File file = new File(dir, fileName);
                    //Log.d(TAG, "Camera prepare to write to file " + file.getAbsolutePath());

        //file.deleteOnExit();
        //mFileUtils.ensureDirectoryExists(file.getParentFile());

        //return file;
        return null;
    }
    Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
		public void onPictureTaken(byte[] imageData, Camera c) {

			if (imageData != null) {

				Intent mIntent = new Intent();

                try
                {
                    File file = GetFile(true);

                java.io.FileOutputStream out = new java.io.FileOutputStream(file);
					android.graphics.Bitmap e = android.graphics.BitmapFactory.decodeByteArray(imageData, 0,
							imageData.length);
					e.compress(android.graphics.Bitmap.CompressFormat.JPEG, 65, out);
					out.close();

                        ExifInterface exif = new ExifInterface(file.getAbsolutePath());
                        createExifData(exif);
                        exif.saveAttributes();

                }
                catch (FileNotFoundException e)
                {
                  e.printStackTrace();
                }
                catch (IOException e)
                {
                   e.printStackTrace();
                }
                m_isPicture = false;
                m_camera.startPreview();
				//setResult(FOTO_MODE,mIntent);
				//finish();
			}
		}
	};

    GeoPoint currentGeoPoint = null;
    void setCurrentLocation(GeoPoint p)
    {
         currentGeoPoint = p;
    }
/*
     * called when exif data profile is created
     */
public void createExifData(ExifInterface exif){
    if (currentGeoPoint == null)
        return;

    // create a reference for Latitude and Longitude
    double lat = currentGeoPoint.getLatitudeE6()/1000000.0;
    if (lat < 0) {
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "S");
        lat = -lat;
    } else {
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N");
    }

    exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,
                    formatLatLongString(lat));

    double lon = currentGeoPoint.getLongitudeE6()/1000000.0;
    if (lon < 0) {
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W");
        lon = -lon;
    } else {
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "E");
    }
    exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,
                    formatLatLongString(lon));

    try {
                    exif.saveAttributes();
            } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            }
            String make = android.os.Build.MANUFACTURER; // get the make of the device
            String model = android.os.Build.MODEL; // get the model of the divice

            exif.setAttribute(ExifInterface.TAG_MAKE, make);
            TelephonyManager telephonyManager = (TelephonyManager)m_context.getSystemService(Context.TELEPHONY_SERVICE);
            String imei = telephonyManager.getDeviceId();
            exif.setAttribute(ExifInterface.TAG_MODEL, model+" - "+imei);

            exif.setAttribute(ExifInterface.TAG_DATETIME, (new java.util.Date(System.currentTimeMillis())).toString()); // set the date & time

}
/*
     * formnat the Lat Long values according to standard exif format
     */
private static String formatLatLongString(double d) {
    // format latitude and longitude according to exif format
    StringBuilder b = new StringBuilder();
    b.append((int) d);
    b.append("/1,");
    d = (d - (int) d) * 60;
    b.append((int) d);
    b.append("/1,");
    d = (d - (int) d) * 60000;
    b.append((int) d);
    b.append("/1000");
    return b.toString();
  }


    public boolean takePicture()
    {
        m_camera.takePicture(null, mPictureCallback, mPictureCallback);
        m_isPicture = true;
        return true;
    }
    public boolean startRecording(){
        try
        {
            m_camera.unlock();
        }
        catch (Exception e)
        {
            Log.e(TAG,e.getMessage());
			e.printStackTrace();
        }
		try {
			mediaRecorder = new MediaRecorder();

			mediaRecorder.setCamera(m_camera);
			mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
			mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

			mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);

			//mediaRecorder.setMaxDuration(maxDurationInMs);

           File file = GetFile(false);
			mediaRecorder.setOutputFile(file.getAbsolutePath());

			//mediaRecorder.setVideoFrameRate(videoFramesPerSecond);
			//mediaRecorder.setVideoSize(m_cameraView.mSurfaceView.getWidth(), m_cameraView.mSurfaceView.getHeight());

			mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
			mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);

			mediaRecorder.setPreviewDisplay(m_surfaceHolder.getSurface());

			//mediaRecorder.setMaxFileSize(maxFileSizeInBytes);

            mediaRecorder.prepare();
			mediaRecorder.start();
            m_isRecording = true;
			return true;
		} catch (IllegalStateException e) {
			Log.e(TAG,e.getMessage());
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			Log.e(TAG,e.getMessage());
			e.printStackTrace();
			return false;
		}
	}

    public void stopRecording(){
	mediaRecorder.stop();
    mediaRecorder.reset();   // You can reuse the object by going back to setAudioSource() step
	m_camera.lock();
        m_isRecording = false;
}
}
