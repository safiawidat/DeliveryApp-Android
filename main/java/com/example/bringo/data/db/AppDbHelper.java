package com.example.bringo.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AppDbHelper extends SQLiteOpenHelper {

    public AppDbHelper(Context context) {
        super(context, DbContract.DB_NAME, null, DbContract.DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.execSQL("PRAGMA foreign_keys=ON;");
    }
    @Override
    public void onCreate(SQLiteDatabase db) {

        // 1) addresses (must come before customers because customers FK -> addresses)
        db.execSQL(
                "CREATE TABLE " + DbContract.T_ADDRESSES + " (" +
                        DbContract.C_ADDRESS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        DbContract.C_LINE1 + " TEXT NOT NULL, " +
                        DbContract.C_LINE2 + " TEXT, " +
                        DbContract.C_CITY + " TEXT NOT NULL, " +
                        DbContract.C_REGION + " TEXT, " +
                        DbContract.C_POSTAL_CODE + " TEXT, " +
                        DbContract.C_COUNTRY + " TEXT NOT NULL, " +
                        DbContract.C_LAT + " REAL, " +
                        DbContract.C_LON + " REAL" +
                        ");"
        );

        // 2) customers
        db.execSQL(
                "CREATE TABLE " + DbContract.T_CUSTOMERS + " (" +
                        DbContract.C_CUSTOMER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        DbContract.C_NAME + " TEXT NOT NULL, " +
                        DbContract.C_EMAIL + " TEXT UNIQUE, " +
                        DbContract.C_PHONE + " TEXT, " +
                        DbContract.C_PASSWORD + " TEXT, " +
                        DbContract.C_CUSTOMER_TYPE + " TEXT NOT NULL, " +   // USER / CARRIER
                        DbContract.C_DEFAULT_ADDRESS_ID + " INTEGER, " +
                        "FOREIGN KEY(" + DbContract.C_DEFAULT_ADDRESS_ID + ") REFERENCES " +
                        DbContract.T_ADDRESSES + "(" + DbContract.C_ADDRESS_ID + ")" +
                        ");"
        );

        // 3) carriers (NO name/code, NO UNASSIGNED row)
        db.execSQL(
                "CREATE TABLE " + DbContract.T_CARRIERS + " (" +
                        DbContract.C_CARRIER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        DbContract.C_CARRIER_CUSTOMER_ID + " INTEGER NOT NULL UNIQUE, " +
                        DbContract.C_CARRIER_ID_NUMBER + " TEXT, " +
                        DbContract.C_CARRIER_CAR_NUMBER + " TEXT, " +
                        DbContract.C_CARRIER_BALANCE + " REAL DEFAULT 0, " +
                        "FOREIGN KEY(" + DbContract.C_CARRIER_CUSTOMER_ID + ") REFERENCES " +
                        DbContract.T_CUSTOMERS + "(" + DbContract.C_CUSTOMER_ID + ")" +
                        ");"
        );

        // 4) shipments (NO shipper/consignee, NO status_codes FK, carrier_id nullable)
        db.execSQL(
                "CREATE TABLE " + DbContract.T_SHIPMENTS + " (" +
                        DbContract.C_SHIPMENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        DbContract.C_TRACKING_NUMBER + " TEXT NOT NULL UNIQUE, " +
                        DbContract.C_SH_CUSTOMER_ID + " INTEGER NOT NULL, " +
                        DbContract.C_SH_CARRIER_ID + " INTEGER, " + // NULL until accepted
                        DbContract.C_ORIGIN_ADDR_ID + " INTEGER NOT NULL, " +
                        DbContract.C_DEST_ADDR_ID + " INTEGER NOT NULL, " +
                        DbContract.C_ITEM_DESC + " TEXT NOT NULL, " +
                        DbContract.C_FEE + " REAL NOT NULL, " +
                        DbContract.C_DISTANCE_KM + " REAL NOT NULL, " +
                        DbContract.C_CREATED_AT + " TEXT NOT NULL, " +
                        DbContract.C_CURRENT_STATUS + " TEXT NOT NULL, " +
                        DbContract.C_CURRENT_STATUS_AT + " TEXT, " +
                        DbContract.C_ETA + " TEXT, " +
                        "FOREIGN KEY(" + DbContract.C_SH_CUSTOMER_ID + ") REFERENCES " +
                        DbContract.T_CUSTOMERS + "(" + DbContract.C_CUSTOMER_ID + "), " +
                        "FOREIGN KEY(" + DbContract.C_SH_CARRIER_ID + ") REFERENCES " +
                        DbContract.T_CARRIERS + "(" + DbContract.C_CARRIER_ID + "), " +
                        "FOREIGN KEY(" + DbContract.C_ORIGIN_ADDR_ID + ") REFERENCES " +
                        DbContract.T_ADDRESSES + "(" + DbContract.C_ADDRESS_ID + "), " +
                        "FOREIGN KEY(" + DbContract.C_DEST_ADDR_ID + ") REFERENCES " +
                        DbContract.T_ADDRESSES + "(" + DbContract.C_ADDRESS_ID + ")" +
                        ");"
        );

        // 5) tracking_events (status TEXT, no FK to status_codes)
        db.execSQL(
                "CREATE TABLE " + DbContract.T_TRACKING_EVENTS + " (" +
                        DbContract.C_EVENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        DbContract.C_TE_SHIPMENT_ID + " INTEGER NOT NULL, " +
                        DbContract.C_OCCURRED_AT + " TEXT NOT NULL, " +
                        DbContract.C_TE_STATUS + " TEXT NOT NULL, " +
                        DbContract.C_LOCATION_DESC + " TEXT, " +
                        DbContract.C_DETAILS + " TEXT, " +
                        DbContract.C_SOURCE + " TEXT, " +
                        "FOREIGN KEY(" + DbContract.C_TE_SHIPMENT_ID + ") REFERENCES " +
                        DbContract.T_SHIPMENTS + "(" + DbContract.C_SHIPMENT_ID + ") ON DELETE CASCADE" +
                        ");"
        );

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.T_TRACKING_EVENTS);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.T_SHIPMENTS);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.T_CARRIERS);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.T_CUSTOMERS);
        db.execSQL("DROP TABLE IF EXISTS " + DbContract.T_ADDRESSES);
        onCreate(db);
    }

}
