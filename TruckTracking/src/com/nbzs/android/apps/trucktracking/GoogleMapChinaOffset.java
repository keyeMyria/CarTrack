package com.nbzs.android.apps.trucktracking;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Point;
import android.util.Log;
import android.content.ContentValues;

/**
 * Created by IntelliJ IDEA.
 * User: Administrator
 * Date: 11-7-28
 * Time: 下午1:31
 * To change this template use File | Settings | File Templates.
 */
public class GoogleMapChinaOffset {

    public class DatabaseHelper extends SQLiteOpenHelper {
        public DatabaseHelper(Context context) {
            super(context, "GoogleMapOffset.db", null, 2);
        }

        private final String sqlCreate = "CREATE TABLE IF NOT EXISTS GoogleMapOffsetCache (Id INTEGER Primary Key NOT NULL, dlat INTEGER NOT NULL, dlon INTEGER NOT NULL); ";

        public void onCreate(SQLiteDatabase db) {
            db.execSQL(sqlCreate);
        }

        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + m_tableName);
            onCreate(db);
        }
    }

    private SQLiteDatabase mDatabase = null;
    private ServiceProcess m_serviceProcess;
    private final String m_tableName = "GoogleMapOffsetCache";

    public GoogleMapChinaOffset(Context context) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        try {
            mDatabase = dbHelper.getWritableDatabase();
        } catch (SQLiteException e) {
            Log.e(Constants.TAG, "Unable to open database for writing", e);
        }

        m_serviceProcess = ServiceProcess.Get(context);
    }
    private LRUMapOffsetCache m_cache = new LRUMapOffsetCache(1000);
     private Point getOffsetFromCache(int key)
     {
         if (m_cache.containsKey(key))
             return m_cache.get(key);
         return null;
     }
    public void setOffsetToCache(final int key, final Point offset) {
        m_cache.put(key, offset);
    }
    public Point getOffset(final double lat, final double lon, final int zoom) {
        if (m_serviceProcess != null && !m_serviceProcess.getMyWebConfig().getisUseGoogleMapOffset())
            return null;
        if (lat > 53.55 || lat < 18.15 || lon > 134.79 || lon < 73.65)
            return null;
        if (zoom < 11 || zoom > 18)
            return null;

        short iLat = (short)((short) Math.round(lat * 100) + 9000);
        short iLon = (short) Math.round(lon * 100);
        int key = ((int)iLat << 16) + iLon;

        Point offset = getOffsetFromCache(key);
        if (offset == null)
        {
            offset = getOffsetFromDb(key);
        }
        if (offset == null)
        {
            offset = m_serviceProcess.getWebServiceClient().GetOffset(key);
            if (offset != null)
            {
                setOffsetToDb(key, offset);
                Log.d(Constants.TAG, "Get Map offset from web for " + key + ": " + offset.x + "," + offset.y);
            }
        }
        if (offset != null)
        {
            setOffsetToCache(key, offset);
        }
        else
        {
            Log.d(Constants.TAG, "can't Get Map offset ");
        }

        if (offset == null)
            return null;
        else
        {
            int dz = (int)Math.pow(2, 18 - zoom);
            return new Point(offset.x / dz, offset.y / dz);
        }
    }

    public void setOffsetToDb(final int key, final Point offset) {
        if (mDatabase == null)
            return;
        ContentValues cv = new ContentValues();
        cv.put("Id", key);
        cv.put("dlat", offset.x);
        cv.put("dlon", offset.y);
        mDatabase.insert(m_tableName, null, cv);
    }

    public Point getOffsetFromDb(final int key) {
        if (mDatabase == null)
            return null;

        try {
            Point ret = null;
            final String[] columns = {"dlat", "dlon"};
            //final long index = ((z << z) + x << z) + y;
            final Cursor cur = mDatabase.query(m_tableName, columns, "Id = " + key, null, null, null, null);
            if (cur.getCount() != 0) {
                cur.moveToFirst();
                ret = new Point(cur.getInt(0), cur.getInt(1));
            }
            cur.close();
            return ret;
        } catch (final Throwable e) {
            android.util.Log.d(Constants.TAG, "sqlite Error :" + e.getMessage());
        }

        return null;
    }
}