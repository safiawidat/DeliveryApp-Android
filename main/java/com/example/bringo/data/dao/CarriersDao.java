package com.example.bringo.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bringo.data.db.AppDbHelper;
import com.example.bringo.data.db.DbContract;

public class CarriersDao {

    private final AppDbHelper dbHelper;

    public CarriersDao(Context context) {
        dbHelper = new AppDbHelper(context);
    }

    public long insertCarrier(long customerId,
                              String idNumber,
                              String carPlate,
                              String bankAccount,
                              String license) {

        // Inserts a carrier profile linked to an existing customer row
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(DbContract.C_CARRIER_CUSTOMER_ID, customerId);
        cv.put(DbContract.C_CARRIER_ID_NUMBER, idNumber);
        cv.put(DbContract.C_CARRIER_CAR_NUMBER, carPlate);
        cv.put(DbContract.C_CARRIER_BALANCE, 0);

        // bankAccount/license are currently not stored in this schema (kept for signature compatibility)
        return db.insert(DbContract.T_CARRIERS, null, cv);
    }

    public long getCarrierIdByCustomerId(long customerId) {
        // Returns carrier_id for a given customer_id
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DbContract.C_CARRIER_ID +
                        " FROM " + DbContract.T_CARRIERS +
                        " WHERE " + DbContract.C_CARRIER_CUSTOMER_ID + "=?",
                new String[]{String.valueOf(customerId)}
        );

        long id = -1;
        if (c.moveToFirst()) id = c.getLong(0);
        c.close();
        return id;
    }

    public long getCarrierIdByCode(String email) {
        // Legacy helper: carrier id by customer email
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT ca." + DbContract.C_CARRIER_ID + " " +
                        "FROM " + DbContract.T_CARRIERS + " ca " +
                        "JOIN " + DbContract.T_CUSTOMERS + " cu " +
                        "ON ca." + DbContract.C_CARRIER_CUSTOMER_ID + " = cu." + DbContract.C_CUSTOMER_ID + " " +
                        "WHERE cu." + DbContract.C_EMAIL + "=?",
                new String[]{email}
        );

        long id = -1;
        if (c.moveToFirst()) id = c.getLong(0);
        c.close();
        return id;
    }

    public double getBalance(long carrierId) {
        // Returns current carrier balance
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DbContract.C_CARRIER_BALANCE +
                        " FROM " + DbContract.T_CARRIERS +
                        " WHERE " + DbContract.C_CARRIER_ID + "=?",
                new String[]{String.valueOf(carrierId)}
        );

        double balance = 0;
        if (c.moveToFirst()) balance = c.getDouble(0);
        c.close();
        return balance;
    }
}