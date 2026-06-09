package com.example.bringo.data.dao;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.bringo.data.db.AppDbHelper;
import com.example.bringo.data.db.DbContract;

public class CustomersDao {

    private final AppDbHelper dbHelper;

    public CustomersDao(Context context) {
        dbHelper = new AppDbHelper(context);
    }

    public long insertUser(String name,
                           String email,
                           String phone,
                           String password,
                           long defaultAddressId,
                           String type) {

        // Inserts a new customer row (USER or CARRIER)
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(DbContract.C_NAME, name);
        cv.put(DbContract.C_EMAIL, email);
        cv.put(DbContract.C_PHONE, phone);
        cv.put(DbContract.C_PASSWORD, password);
        cv.put(DbContract.C_CUSTOMER_TYPE, type);
        cv.put(DbContract.C_DEFAULT_ADDRESS_ID, defaultAddressId);

        return db.insert(DbContract.T_CUSTOMERS, null, cv);
    }

    public long login(String email, String password) {
        // Login by email + password, returns customer_id or -1
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DbContract.C_CUSTOMER_ID +
                        " FROM " + DbContract.T_CUSTOMERS +
                        " WHERE " + DbContract.C_EMAIL + "=? AND " + DbContract.C_PASSWORD + "=? " +
                        "LIMIT 1",
                new String[]{email, password}
        );

        try {
            if (c.moveToFirst()) return c.getLong(0);
            return -1;
        } finally {
            c.close();
        }
    }

    public boolean emailExists(String email) {
        // Quick existence check by email
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT 1 FROM " + DbContract.T_CUSTOMERS +
                        " WHERE " + DbContract.C_EMAIL + "=? LIMIT 1",
                new String[]{email}
        );

        try {
            return c.moveToFirst();
        } finally {
            c.close();
        }
    }

    public long getCustomerIdByEmail(String email) {
        // Returns customer_id for email, or -1 if not found
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DbContract.C_CUSTOMER_ID +
                        " FROM " + DbContract.T_CUSTOMERS +
                        " WHERE " + DbContract.C_EMAIL + "=? LIMIT 1",
                new String[]{email}
        );

        try {
            if (c.moveToFirst()) return c.getLong(0);
            return -1;
        } finally {
            c.close();
        }
    }

    public long getDefaultAddressId(long customerId) {
        // Returns saved default address id for a customer
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DbContract.C_DEFAULT_ADDRESS_ID +
                        " FROM " + DbContract.T_CUSTOMERS +
                        " WHERE " + DbContract.C_CUSTOMER_ID + "=? LIMIT 1",
                new String[]{String.valueOf(customerId)}
        );

        try {
            if (c.moveToFirst()) return c.getLong(0);
            return -1;
        } finally {
            c.close();
        }
    }

    public String getCustomerType(long userId) {
        // Returns USER or CARRIER for this customer_id
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DbContract.C_CUSTOMER_TYPE +
                        " FROM " + DbContract.T_CUSTOMERS +
                        " WHERE " + DbContract.C_CUSTOMER_ID + "=? LIMIT 1",
                new String[]{String.valueOf(userId)}
        );

        try {
            if (c.moveToFirst()) return c.getString(0);
            return null;
        } finally {
            c.close();
        }
    }

    public String getEmailById(long userId) {
        // Returns email for this customer_id
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DbContract.C_EMAIL +
                        " FROM " + DbContract.T_CUSTOMERS +
                        " WHERE " + DbContract.C_CUSTOMER_ID + "=? LIMIT 1",
                new String[]{String.valueOf(userId)}
        );

        try {
            if (c.moveToFirst()) return c.getString(0);
            return null;
        } finally {
            c.close();
        }
    }

    public boolean updateUserBasics(long customerId,
                                    String name,
                                    String phone,
                                    String password,
                                    long defaultAddressId) {

        // Updates name/phone/password/default address for an existing row
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(DbContract.C_NAME, name);
        cv.put(DbContract.C_PHONE, phone);
        cv.put(DbContract.C_PASSWORD, password);
        cv.put(DbContract.C_DEFAULT_ADDRESS_ID, defaultAddressId);

        int rows = db.update(
                DbContract.T_CUSTOMERS,
                cv,
                DbContract.C_CUSTOMER_ID + "=?",
                new String[]{String.valueOf(customerId)}
        );

        return rows > 0;
    }
}