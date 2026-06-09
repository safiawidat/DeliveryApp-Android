package com.example.bringo.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bringo.data.db.AppDbHelper;
import com.example.bringo.data.db.DbContract;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TrackingEventsDao {

    private final AppDbHelper dbHelper;

    public TrackingEventsDao(Context context) {
        dbHelper = new AppDbHelper(context);
    }

    private String now() {
        // Timestamp string used in tracking_events
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    public long insertEvent(long shipmentId,
                            String status,
                            String locationDesc,
                            String details,
                            String source) {

        // Inserts one tracking event row for a shipment
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(DbContract.C_TE_SHIPMENT_ID, shipmentId);
        cv.put(DbContract.C_OCCURRED_AT, now());
        cv.put(DbContract.C_TE_STATUS, status);
        cv.put(DbContract.C_LOCATION_DESC, locationDesc);
        cv.put(DbContract.C_DETAILS, details);
        cv.put(DbContract.C_SOURCE, source);

        return db.insert(DbContract.T_TRACKING_EVENTS, null, cv);
    }

    public Cursor getEventsCursor(long shipmentId) {
        // Returns tracking events for a shipment (newest first)
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        return db.rawQuery(
                "SELECT " +
                        DbContract.C_EVENT_ID + " AS _id, " +
                        DbContract.C_OCCURRED_AT + ", " +
                        DbContract.C_TE_STATUS + ", " +
                        DbContract.C_LOCATION_DESC + ", " +
                        DbContract.C_DETAILS + ", " +
                        DbContract.C_SOURCE + " " +
                        "FROM " + DbContract.T_TRACKING_EVENTS + " " +
                        "WHERE " + DbContract.C_TE_SHIPMENT_ID + "=? " +
                        "ORDER BY " + DbContract.C_EVENT_ID + " DESC",
                new String[]{String.valueOf(shipmentId)}
        );
    }
}