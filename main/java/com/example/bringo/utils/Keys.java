package com.example.bringo.utils;

public final class Keys {
    private Keys() {
        // No instances
    }

    // SharedPreferences file name
    public static final String PREFS = "prefs";

    // Saved session fields (SharedPreferences keys)
    public static final String LOGGED_USER_ID = "logged_user_id";
    public static final String LOGGED_ROLE = "logged_role";
    public static final String LOGGED_EMAIL = "logged_email";
    public static final String LOGGED_CARRIER_ID = "logged_carrier_id";

    // Intent extras
    public static final String EXTRA_SHIPMENT_ID = "shipment_id";
    public static final String EXTRA_LOGIN_ROLE = "login_role";
    public static final String EXTRA_PREFILL_EMAIL = "EXTRA_PREFILL_EMAIL";

    // Carrier screen navigation
    public static final String EXTRA_CARRIER_ID = "carrier_id";

    // CreateRequest -> RequestCheckout extras (keep string values EXACTLY the same)
    public static final String EXTRA_ITEM_DESC = "item_desc";

    public static final String EXTRA_PICK_LINE1 = "pick_line1";
    public static final String EXTRA_PICK_CITY = "pick_city";
    public static final String EXTRA_PICK_POSTAL = "pick_postal";
    public static final String EXTRA_PICK_LAT = "pick_lat";
    public static final String EXTRA_PICK_LON = "pick_lon";

    public static final String EXTRA_USE_DEFAULT_DELIVERY = "use_default_delivery";

    public static final String EXTRA_DEL_LINE1 = "del_line1";
    public static final String EXTRA_DEL_CITY = "del_city";
    public static final String EXTRA_DEL_POSTAL = "del_postal";
    public static final String EXTRA_DEL_LAT = "del_lat";
    public static final String EXTRA_DEL_LON = "del_lon";

    public static final String EXTRA_KM = "km";
    public static final String EXTRA_FEE = "fee";
}