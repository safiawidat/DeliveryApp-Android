package com.example.bringo.data.db;

public final class DbContract {
    private DbContract() {}

    // ===== Database =====
    public static final String DB_NAME = "bringo.db";
    public static final int DB_VERSION = 4;

    // ===== customers =====
    public static final String T_CUSTOMERS = "customers";
    public static final String C_CUSTOMER_ID = "customer_id";
    public static final String C_NAME = "name";
    public static final String C_EMAIL = "email";
    public static final String C_PHONE = "phone";

    // BrinGo additions (minimal)
    public static final String C_PASSWORD = "password";
    public static final String C_CUSTOMER_TYPE = "customer_type"; // USER / CARRIER
    public static final String C_DEFAULT_ADDRESS_ID = "default_address_id";

    // ===== addresses =====
    public static final String T_ADDRESSES = "addresses";
    public static final String C_ADDRESS_ID = "address_id";
    public static final String C_LINE1 = "line1";
    public static final String C_LINE2 = "line2";
    public static final String C_CITY = "city";
    public static final String C_REGION = "region";
    public static final String C_POSTAL_CODE = "postal_code";
    public static final String C_COUNTRY = "country";
    public static final String C_LAT = "lat";
    public static final String C_LON = "lon";

    // ===== carriers =====
    public static final String T_CARRIERS = "carriers";
    public static final String C_CARRIER_ID = "carrier_id";
    public static final String C_CARRIER_CUSTOMER_ID = "customer_id";
    public static final String C_CARRIER_ID_NUMBER = "id_number";
    public static final String C_CARRIER_CAR_NUMBER = "car_number";
    public static final String C_CARRIER_BALANCE = "balance";

    // ===== shipments (orders) =====
    public static final String T_SHIPMENTS = "shipments";
    public static final String C_SHIPMENT_ID = "shipment_id";
    public static final String C_TRACKING_NUMBER = "tracking_number";
    public static final String C_SH_CUSTOMER_ID = "customer_id";
    public static final String C_SH_CARRIER_ID = "carrier_id";
    public static final String C_ORIGIN_ADDR_ID = "origin_addr_id";
    public static final String C_DEST_ADDR_ID = "dest_addr_id";
    public static final String C_ITEM_DESC = "item_desc";
    public static final String C_FEE = "fee";
    public static final String C_DISTANCE_KM = "distance_km";
    public static final String C_CREATED_AT = "created_at";
    public static final String C_CURRENT_STATUS = "current_status";
    public static final String C_CURRENT_STATUS_AT = "current_status_at";
    public static final String C_ETA = "eta";

    // ===== tracking_events =====
    public static final String T_TRACKING_EVENTS = "tracking_events";
    public static final String C_EVENT_ID = "event_id";
    public static final String C_TE_SHIPMENT_ID = "shipment_id";
    public static final String C_OCCURRED_AT = "occurred_at";
    public static final String C_TE_STATUS = "status";
    public static final String C_LOCATION_DESC = "location_desc";
    public static final String C_DETAILS = "details";
    public static final String C_SOURCE = "source";
}
