package com.example.bringo.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bringo.data.db.AppDbHelper;
import com.example.bringo.data.db.DbContract;

public class AddressesDao {

    private final AppDbHelper dbHelper;

    public AddressesDao(Context context) {
        dbHelper = new AppDbHelper(context);
    }

    public long insertAddress(String line1, String city, String postalCode, String country, double lat, double lon) {
        // Inserts a minimal address row
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(DbContract.C_LINE1, line1);
        cv.put(DbContract.C_CITY, city);
        cv.put(DbContract.C_POSTAL_CODE, postalCode);
        cv.put(DbContract.C_COUNTRY, country);
        cv.put(DbContract.C_LAT, lat);
        cv.put(DbContract.C_LON, lon);

        return db.insert(DbContract.T_ADDRESSES, null, cv);
    }

    public double[] getLatLon(long addressId) {
        // Returns [lat, lon] for an address id
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DbContract.C_LAT + ", " + DbContract.C_LON +
                        " FROM " + DbContract.T_ADDRESSES +
                        " WHERE " + DbContract.C_ADDRESS_ID + "=? LIMIT 1",
                new String[]{String.valueOf(addressId)}
        );

        try {
            double lat = 0, lon = 0;
            if (c.moveToFirst()) {
                lat = c.getDouble(0);
                lon = c.getDouble(1);
            }
            return new double[]{lat, lon};
        } finally {
            c.close();
        }
    }

    public String getAddressText(long addressId) {
        // Returns a simple text version of the address
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DbContract.C_LINE1 + ", " + DbContract.C_CITY + ", " + DbContract.C_POSTAL_CODE +
                        " FROM " + DbContract.T_ADDRESSES +
                        " WHERE " + DbContract.C_ADDRESS_ID + "=? LIMIT 1",
                new String[]{String.valueOf(addressId)}
        );

        try {
            if (!c.moveToFirst()) return "";
            String line1 = c.getString(0);
            String city = c.getString(1);
            String postal = c.getString(2);
            return line1 + ", " + city + " (" + postal + ")";
        } finally {
            c.close();
        }
    }
}