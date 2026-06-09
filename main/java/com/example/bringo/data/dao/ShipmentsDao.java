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

public class ShipmentsDao {

    private final AppDbHelper dbHelper;

    // Status values stored in DB
    private static final String ST_CREATED = "CREATED";
    private static final String ST_IN_TRANSIT = "IN_TRANSIT";
    private static final String ST_PICKED_UP = "PICKED_UP";
    private static final String ST_ON_THE_WAY = "ON_THE_WAY";
    private static final String ST_DELIVERED = "DELIVERED";

    public ShipmentsDao(Context context) {
        dbHelper = new AppDbHelper(context);
    }

    private String now() {
        // Current timestamp string saved in DB
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());
    }

    private String etaAfterMinutes(int minutes) {
        // ETA = now + minutes
        long ms = System.currentTimeMillis() + minutes * 60L * 1000L;
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date(ms));
    }

    public long createRequest(long customerId,
                              long originAddrId,
                              long destAddrId,
                              String itemDesc,
                              double fee,
                              double km) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Simple tracking number
        String tracking = "BR" + System.currentTimeMillis();

        // Short text for list rows
        String itemShort = itemDesc;
        if (itemShort != null && itemShort.length() > 40) {
            itemShort = itemShort.substring(0, 40);
        }

        String nowStr = now();

        ContentValues cv = new ContentValues();
        cv.put(DbContract.C_TRACKING_NUMBER, tracking);

        cv.put(DbContract.C_SH_CUSTOMER_ID, customerId);
        cv.putNull(DbContract.C_SH_CARRIER_ID); // NULL until accepted

        cv.put(DbContract.C_ORIGIN_ADDR_ID, originAddrId);
        cv.put(DbContract.C_DEST_ADDR_ID, destAddrId);

        cv.put(DbContract.C_ITEM_DESC, itemShort);
        cv.put(DbContract.C_FEE, fee);
        cv.put(DbContract.C_DISTANCE_KM, km);

        cv.put(DbContract.C_CREATED_AT, nowStr);
        cv.put(DbContract.C_CURRENT_STATUS, ST_CREATED);
        cv.put(DbContract.C_CURRENT_STATUS_AT, nowStr);

        int mins = 10 + (int) Math.round(km * 2.0);
        cv.put(DbContract.C_ETA, etaAfterMinutes(mins));

        return db.insert(DbContract.T_SHIPMENTS, null, cv);
    }

    /**
     * Legacy assign (kept to avoid breaking other code).
     * This updates unconditionally.
     */
    public void assignCarrier(long shipmentId, long carrierId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(DbContract.C_SH_CARRIER_ID, carrierId);
        cv.put(DbContract.C_CURRENT_STATUS, ST_IN_TRANSIT);
        cv.put(DbContract.C_CURRENT_STATUS_AT, now());

        db.update(
                DbContract.T_SHIPMENTS,
                cv,
                DbContract.C_SHIPMENT_ID + "=?",
                new String[]{String.valueOf(shipmentId)}
        );
    }

    /**
     * Safe assign: assigns ONLY if still unassigned (carrier_id IS NULL).
     * Returns true only when the assignment really happened now.
     */
    public boolean tryAssignCarrier(long shipmentId, long carrierId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put(DbContract.C_SH_CARRIER_ID, carrierId);
        cv.put(DbContract.C_CURRENT_STATUS, ST_IN_TRANSIT);
        cv.put(DbContract.C_CURRENT_STATUS_AT, now());

        int rows = db.update(
                DbContract.T_SHIPMENTS,
                cv,
                DbContract.C_SHIPMENT_ID + "=? AND " + DbContract.C_SH_CARRIER_ID + " IS NULL",
                new String[]{String.valueOf(shipmentId)}
        );

        return rows > 0;
    }

    public void updateStatus(long shipmentId, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Update status and timestamp
        db.execSQL(
                "UPDATE " + DbContract.T_SHIPMENTS +
                        " SET " + DbContract.C_CURRENT_STATUS + "=?, " +
                        DbContract.C_CURRENT_STATUS_AT + "=? " +
                        "WHERE " + DbContract.C_SHIPMENT_ID + "=?",
                new Object[]{status, now(), shipmentId}
        );
    }

    /**
     * Carrier list:
     * - Shows AVAILABLE (unassigned) + MY JOB (assigned to this carrier)
     * - Hides DELIVERED
     * - Includes assigned_carrier_id + current_status so UI can differentiate rows
     */
    public Cursor getOpenRequestsCursor(long carrierId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        if (carrierId <= 0) {
            // If carrierId isn't known, show only unassigned (safe fallback)
            return db.rawQuery(
                    "SELECT " +
                            DbContract.C_SHIPMENT_ID + " AS _id, " +
                            DbContract.C_SH_CARRIER_ID + " AS assigned_carrier_id, " +
                            DbContract.C_TRACKING_NUMBER + ", " +
                            DbContract.C_ITEM_DESC + " AS service_level, " +
                            DbContract.C_CURRENT_STATUS + ", " +
                            DbContract.C_ETA + " " +
                            "FROM " + DbContract.T_SHIPMENTS + " " +
                            "WHERE " + DbContract.C_SH_CARRIER_ID + " IS NULL " +
                            "AND " + DbContract.C_CURRENT_STATUS + " <> ? " +
                            "ORDER BY " + DbContract.C_SHIPMENT_ID + " DESC",
                    new String[]{ST_DELIVERED}
            );
        }

        return db.rawQuery(
                "SELECT " +
                        DbContract.C_SHIPMENT_ID + " AS _id, " +
                        DbContract.C_SH_CARRIER_ID + " AS assigned_carrier_id, " +
                        DbContract.C_TRACKING_NUMBER + ", " +
                        DbContract.C_ITEM_DESC + " AS service_level, " +
                        DbContract.C_CURRENT_STATUS + ", " +
                        DbContract.C_ETA + " " +
                        "FROM " + DbContract.T_SHIPMENTS + " " +
                        "WHERE " + DbContract.C_CURRENT_STATUS + " <> ? " +
                        "AND (" + DbContract.C_SH_CARRIER_ID + " IS NULL OR " +
                        DbContract.C_SH_CARRIER_ID + " = ?) " +
                        "ORDER BY " + DbContract.C_SHIPMENT_ID + " DESC",
                new String[]{ST_DELIVERED, String.valueOf(carrierId)}
        );
    }

    public Cursor getCustomerShipmentsCursor(long customerId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        return db.rawQuery(
                "SELECT " +
                        DbContract.C_SHIPMENT_ID + " AS _id, " +
                        DbContract.C_TRACKING_NUMBER + ", " +
                        DbContract.C_ITEM_DESC + " AS service_level, " +
                        DbContract.C_FEE + " AS declared_value, " +
                        DbContract.C_CURRENT_STATUS + ", " +
                        DbContract.C_ETA + " " +
                        "FROM " + DbContract.T_SHIPMENTS + " " +
                        "WHERE " + DbContract.C_SH_CUSTOMER_ID + "=? " +
                        "ORDER BY " + DbContract.C_SHIPMENT_ID + " DESC",
                new String[]{String.valueOf(customerId)}
        );
    }

    public Cursor getShipmentDetailsCursor(long shipmentId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        return db.rawQuery(
                "SELECT " +
                        "s." + DbContract.C_SHIPMENT_ID + " AS _id, " +
                        "s." + DbContract.C_TRACKING_NUMBER + ", " +
                        "s." + DbContract.C_ITEM_DESC + " AS service_level, " +
                        "s." + DbContract.C_FEE + " AS declared_value, " +
                        "s." + DbContract.C_DISTANCE_KM + " AS weight_kg, " +
                        "s." + DbContract.C_CURRENT_STATUS + ", " +
                        "s." + DbContract.C_CURRENT_STATUS_AT + ", " +
                        "s." + DbContract.C_ETA + ", " +
                        "CASE WHEN s." + DbContract.C_SH_CARRIER_ID + " IS NULL THEN 'Unassigned' " +
                        "     ELSE cu." + DbContract.C_NAME + " END AS carrier_name " +
                        "FROM " + DbContract.T_SHIPMENTS + " s " +
                        "LEFT JOIN " + DbContract.T_CARRIERS + " ca ON s." + DbContract.C_SH_CARRIER_ID + " = ca." + DbContract.C_CARRIER_ID + " " +
                        "LEFT JOIN " + DbContract.T_CUSTOMERS + " cu ON ca." + DbContract.C_CARRIER_CUSTOMER_ID + " = cu." + DbContract.C_CUSTOMER_ID + " " +
                        "WHERE s." + DbContract.C_SHIPMENT_ID + "=?",
                new String[]{String.valueOf(shipmentId)}
        );
    }

    public long getCarrierId(long shipmentId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DbContract.C_SH_CARRIER_ID +
                        " FROM " + DbContract.T_SHIPMENTS +
                        " WHERE " + DbContract.C_SHIPMENT_ID + "=?",
                new String[]{String.valueOf(shipmentId)}
        );

        long id = -1;
        if (c.moveToFirst() && !c.isNull(0)) id = c.getLong(0);
        c.close();
        return id;
    }

    public boolean markDeliveredAndPayCarrier(long shipmentId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor c = db.rawQuery(
                "SELECT " + DbContract.C_SH_CARRIER_ID + ", " + DbContract.C_FEE +
                        " FROM " + DbContract.T_SHIPMENTS +
                        " WHERE " + DbContract.C_SHIPMENT_ID + "=?",
                new String[]{String.valueOf(shipmentId)}
        );

        if (!c.moveToFirst()) {
            c.close();
            return false;
        }

        if (c.isNull(0)) {
            c.close();
            return false;
        }

        long carrierId = c.getLong(0);
        double fee = c.getDouble(1);
        c.close();

        updateStatus(shipmentId, ST_DELIVERED);

        db.execSQL(
                "UPDATE " + DbContract.T_CARRIERS +
                        " SET " + DbContract.C_CARRIER_BALANCE + " = IFNULL(" + DbContract.C_CARRIER_BALANCE + ",0) + ? " +
                        "WHERE " + DbContract.C_CARRIER_ID + "=?",
                new Object[]{fee, carrierId}
        );

        return true;
    }
    // Tracking list = NOT delivered (optional search)
    public Cursor getCustomerActiveShipmentsCursor(long customerId, String query) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String base =
                "SELECT " +
                        DbContract.C_SHIPMENT_ID + " AS _id, " +
                        DbContract.C_TRACKING_NUMBER + ", " +
                        DbContract.C_ITEM_DESC + " AS service_level, " +
                        DbContract.C_FEE + " AS declared_value, " +
                        DbContract.C_CURRENT_STATUS + ", " +
                        DbContract.C_ETA + " " +
                        "FROM " + DbContract.T_SHIPMENTS + " " +
                        "WHERE " + DbContract.C_SH_CUSTOMER_ID + "=? " +
                        "AND " + DbContract.C_CURRENT_STATUS + " <> ? ";

        if (query == null || query.trim().isEmpty()) {
            return db.rawQuery(
                    base + "ORDER BY " + DbContract.C_SHIPMENT_ID + " DESC",
                    new String[]{String.valueOf(customerId), ST_DELIVERED}
            );
        }

        String like = "%" + query.trim() + "%";
        return db.rawQuery(
                base + "AND (" + DbContract.C_TRACKING_NUMBER + " LIKE ? OR " + DbContract.C_ITEM_DESC + " LIKE ?) " +
                        "ORDER BY " + DbContract.C_SHIPMENT_ID + " DESC",
                new String[]{String.valueOf(customerId), ST_DELIVERED, like, like}
        );
    }

    // History list = ONLY delivered (optional search)
    public Cursor getCustomerDeliveredShipmentsCursor(long customerId, String query) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String base =
                "SELECT " +
                        DbContract.C_SHIPMENT_ID + " AS _id, " +
                        DbContract.C_TRACKING_NUMBER + ", " +
                        DbContract.C_ITEM_DESC + " AS service_level, " +
                        DbContract.C_FEE + " AS declared_value, " +
                        DbContract.C_CURRENT_STATUS + ", " +
                        DbContract.C_ETA + " " +
                        "FROM " + DbContract.T_SHIPMENTS + " " +
                        "WHERE " + DbContract.C_SH_CUSTOMER_ID + "=? " +
                        "AND " + DbContract.C_CURRENT_STATUS + " = ? ";

        if (query == null || query.trim().isEmpty()) {
            return db.rawQuery(
                    base + "ORDER BY " + DbContract.C_SHIPMENT_ID + " DESC",
                    new String[]{String.valueOf(customerId), ST_DELIVERED}
            );
        }

        String like = "%" + query.trim() + "%";
        return db.rawQuery(
                base + "AND (" + DbContract.C_TRACKING_NUMBER + " LIKE ? OR " + DbContract.C_ITEM_DESC + " LIKE ?) " +
                        "ORDER BY " + DbContract.C_SHIPMENT_ID + " DESC",
                new String[]{String.valueOf(customerId), ST_DELIVERED, like, like}
        );
    }
    // Delete ONLY if the shipment belongs to this customer AND was NOT accepted yet (carrier_id IS NULL)
    public boolean deleteUnacceptedCustomerShipment(long shipmentId, long customerId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int rows = db.delete(
                DbContract.T_SHIPMENTS,
                DbContract.C_SHIPMENT_ID + "=? AND " +
                        DbContract.C_SH_CUSTOMER_ID + "=? AND " +
                        DbContract.C_SH_CARRIER_ID + " IS NULL AND " +
                        DbContract.C_CURRENT_STATUS + " <> ?",
                new String[]{String.valueOf(shipmentId), String.valueOf(customerId), ST_DELIVERED}
        );

        return rows > 0;
    }
}