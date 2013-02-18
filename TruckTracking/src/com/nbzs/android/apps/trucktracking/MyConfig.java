package com.nbzs.android.apps.trucktracking;
/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 11-7-27
 * Time: 下午2:38
 * To change this template use File | Settings | File Templates.
 */
import android.content.SharedPreferences;
import android.content.Context;

public class MyConfig {
    public MyConfig(Context context)
        {
            //PreferenceManager.SetDefaultValues(activity, Resource.Xml.carconfig, false);
            //_preferences = PreferenceManager.GetDefaultSharedPreferences(activity.ApplicationContext);
            _preferences = context.getSharedPreferences(Constants.SETTINGS_NAME, 0);

           Update();
        }
        public void Update()
        {
           _carName = _preferences.getString("CarName", "ZJB-8092H");
            _serverUrl = _preferences.getString("ServerUrl", "http://17haha8.gicp.net");
            _isSendToWeb = _preferences.getBoolean("UseWebService", false);
            _isUseGoogleMapOffset = _preferences.getBoolean("UseGoogleMapOffset", false);
            _isStartVideoRecording =  _preferences.getBoolean("StartVideoRecording", false);
        }
        private SharedPreferences _preferences;
        private String _carName;
        private String _serverUrl;
        private boolean _isSendToWeb;
        private boolean _isUseGoogleMapOffset;
    private boolean _isStartVideoRecording;

        public String getCarName()
        {
            return _carName;
        }
        public String getServerUrl()
        {
            return _serverUrl;
        }


        public boolean getIsUseWebService()
        {
             return _isSendToWeb;
        }


        public boolean getisUseGoogleMapOffset()
        {
             return _isUseGoogleMapOffset;
        }

        public boolean getisStartVideoRecording()
        {
             return _isStartVideoRecording;
        }

        public boolean getIsDebug()
        {
            return false;
        }

        public int getSendTryCnt()
        {
            return 1;
        }
}
