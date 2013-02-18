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
import android.preference.PreferenceManager;

public class AppPreferences {
    public static AppPreferences getInstance()
    {
        return s_inst;
    }
    private static AppPreferences s_inst;
    public static void Initialize(Context context)
    {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, false);

        s_inst = new AppPreferences();
        //_preferences = context.getSharedPreferences(Constants.SETTINGS_NAME, Context.MODE_PRIVATE);
        s_inst._preferences =  PreferenceManager.getDefaultSharedPreferences(context);
        s_inst.Update();
    }
    private AppPreferences()
        {

        }
        public void Update()
        {
           _carName = _preferences.getString("TruckName", null);
            _serverUrl = _preferences.getString("ServerUrl", null);
//            _isSendToWeb = _preferences.getBoolean("UseWebService", false);
//            _isUseGoogleMapOffset = _preferences.getBoolean("UseGoogleMapOffset", false);
//            _isStartVideoRecording =  _preferences.getBoolean("StartVideoRecording", false);
            _sendInterval = Integer.parseInt(_preferences.getString("SendInterval", "0"));
            _enableTts = _preferences.getBoolean("EnableTts", false);
            _enableGeocode = _preferences.getBoolean("EnableGeocode",false);
            _enableScreen = _preferences.getBoolean("EnableScreen", false);
        }
        private SharedPreferences _preferences;
        private String _carName;
        private String _serverUrl;
        private int _sendInterval;
        private boolean _enableTts;
        private boolean _enableGeocode;
        private boolean _enableScreen;

        //private boolean _isSendToWeb;
        //private boolean _isUseGoogleMapOffset;
        //private boolean _isStartVideoRecording;

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
             return true;
        }
//
//
//        public boolean getisUseGoogleMapOffset()
//        {
//             return _isUseGoogleMapOffset;
//        }
//
//        public boolean getisStartVideoRecording()
//        {
//             return _isStartVideoRecording;
//        }

        public boolean getIsDebug()
        {
            return false;
        }

        public int getSendTryCnt()
        {
            return 1;
        }
        public int getSendMsgQueCnt()
        {
            return 1000;
        }

        public int getSendInterval()
        {
            return _sendInterval * 1000;
        }
    
        public int getSendSleepTime()
        {
            return this.getSendInterval() / getSendTryCnt();
        }
        public boolean enableReverseGeocode()
        {
            return _enableGeocode;
        }

        public boolean enableTts()
        {
            return _enableTts;
        }

        public boolean enableScreen()
        {
            return _enableScreen;
        }
}
