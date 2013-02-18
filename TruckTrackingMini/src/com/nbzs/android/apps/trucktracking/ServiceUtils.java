package com.nbzs.android.apps.trucktracking;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 11-7-27
 * Time: 下午2:43
 * To change this template use File | Settings | File Templates.
 */

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.location.Location;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

public class ServiceUtils {
    private static final String TAG = Constants.TAG;

    private AppPreferences m_config;

    public AppPreferences getAppPreferences() {
        return m_config;
    }

    private Context m_context;

    private ServiceUtils(Context context) {
        m_context = context;

        m_config = AppPreferences.getInstance();
        //Log.d(TAG, "ServiceUtils track service address is " + m_carTrackService.Url);
    }


    /**
     * A factory which can produce instances of {@link ServiceUtils},
     * and can be overridden in tests (a.k.a. poor man's guice).
     */
    public static class Factory {
        private static Factory instance = new Factory();

        private static Object m_lockCreator = new Object();
        private static ServiceUtils m_instance;

        /**
         * Creates and returns an instance of {@link ServiceUtils} which
         * uses the given context to access its data.
         */
        public static ServiceUtils get(Context context) {
            synchronized (m_lockCreator) {
                if (m_instance == null) {
                    Log.d(TAG, "Create ServiceUtils Instance.");
                    m_instance = instance.newForContext(context);
                }
            }
            return m_instance;
        }

        /**
         * Returns the global instance of this factory.
         */
        public static Factory getInstance() {
            return instance;
        }

        /**
         * Overrides the global instance for this factory, to be used for testing.
         * If used, don't forget to set it back to the original value after the
         * test is run.
         */
        public static void overrideInstance(Factory factory) {
            instance = factory;
        }

        /**
         * Creates an instance of {@link ServiceUtils}.
         */
        protected ServiceUtils newForContext(Context context) {
            return new ServiceUtils(context);
        }
    }
}
