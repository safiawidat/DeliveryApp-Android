package com.example.bringo.ui.customer;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bringo.R;
import com.example.bringo.data.dao.AddressesDao;
import com.example.bringo.data.dao.CustomersDao;
import com.example.bringo.data.dao.ShipmentsDao;
import com.example.bringo.data.dao.TrackingEventsDao;
import com.example.bringo.ui.auth.StartActivity;
import com.example.bringo.utils.FirebaseBonus;
import com.example.bringo.utils.Keys;
import com.example.bringo.utils.Session;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RequestCheckoutActivity extends AppCompatActivity {

    private static final String ST_CREATED = "CREATED";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_checkout);

        // Must be logged in
        long userId = Session.getLoggedUserId(this);
        if (userId == -1) {
            startActivity(new Intent(this, StartActivity.class));
            finish();
            return;
        }

        // Read required extras from CreateRequestActivity
        String item = getIntent().getStringExtra(Keys.EXTRA_ITEM_DESC);
        if (item == null || item.trim().isEmpty()) {
            Toast.makeText(this, "Missing item description", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String pickLine1 = getIntent().getStringExtra(Keys.EXTRA_PICK_LINE1);
        String pickCity = getIntent().getStringExtra(Keys.EXTRA_PICK_CITY);
        String pickPostal = getIntent().getStringExtra(Keys.EXTRA_PICK_POSTAL);
        double pickLat = getIntent().getDoubleExtra(Keys.EXTRA_PICK_LAT, 0);
        double pickLon = getIntent().getDoubleExtra(Keys.EXTRA_PICK_LON, 0);

        boolean useDefault = getIntent().getBooleanExtra(Keys.EXTRA_USE_DEFAULT_DELIVERY, true);

        String delLine1 = getIntent().getStringExtra(Keys.EXTRA_DEL_LINE1);
        String delCity = getIntent().getStringExtra(Keys.EXTRA_DEL_CITY);
        String delPostal = getIntent().getStringExtra(Keys.EXTRA_DEL_POSTAL);
        double delLat = getIntent().getDoubleExtra(Keys.EXTRA_DEL_LAT, 0);
        double delLon = getIntent().getDoubleExtra(Keys.EXTRA_DEL_LON, 0);

        double fee = getIntent().getDoubleExtra(Keys.EXTRA_FEE, 0);
        double km = getIntent().getDoubleExtra(Keys.EXTRA_KM, 0);

        // UI summary
        TextView tv = findViewById(R.id.tvSummary);
        tv.setText(
                "Item: " + item +
                        "\nFee: " + String.format(Locale.US, "%.2f", fee) + " ₪" +
                        "\nDistance: " + String.format(Locale.US, "%.1f", km) + " km"
        );

        Button btnPay = findViewById(R.id.btnPay);

        btnPay.setOnClickListener(v -> {
            // Prevent double clicks
            btnPay.setEnabled(false);

            AddressesDao addressesDao = new AddressesDao(this);

            // Insert pickup address
            long pickupAddrId = addressesDao.insertAddress(pickLine1, pickCity, pickPostal, "IL", pickLat, pickLon);
            if (pickupAddrId == -1) {
                btnPay.setEnabled(true);
                Toast.makeText(this, "Payment failed: pickup address", Toast.LENGTH_SHORT).show();
                return;
            }

            // Resolve delivery address (default or custom)
            long destAddrId;
            if (useDefault) {
                CustomersDao customersDao = new CustomersDao(this);
                destAddrId = customersDao.getDefaultAddressId(userId);
            } else {
                destAddrId = addressesDao.insertAddress(delLine1, delCity, delPostal, "IL", delLat, delLon);
            }

            if (destAddrId == -1) {
                btnPay.setEnabled(true);
                Toast.makeText(this, "Payment failed: delivery address", Toast.LENGTH_SHORT).show();
                return;
            }

            // Insert shipment row
            ShipmentsDao shipmentsDao = new ShipmentsDao(this);
            long shipmentId = shipmentsDao.createRequest(userId, pickupAddrId, destAddrId, item, fee, km);
            if (shipmentId == -1) {
                btnPay.setEnabled(true);
                Toast.makeText(this, "Payment failed: shipment insert", Toast.LENGTH_SHORT).show();
                return;
            }

            // First tracking event (SQLite)
            TrackingEventsDao teDao = new TrackingEventsDao(this);
            teDao.insertEvent(shipmentId, ST_CREATED, pickCity, "Order paid. Waiting for carrier.", "manual");

            // Firebase bonus (best-effort): shipment doc + CREATED event
            String trackingNumber = "";
            Cursor sc = shipmentsDao.getShipmentDetailsCursor(shipmentId);
            try {
                if (sc.moveToFirst()) {
                    trackingNumber = sc.getString(sc.getColumnIndexOrThrow("tracking_number"));
                }
            } finally {
                sc.close();
            }

            Map<String, Object> ship = new HashMap<>();
            ship.put("shipmentId", shipmentId);
            ship.put("customerSqliteId", userId);
            ship.put("carrierSqliteId", null);
            ship.put("status", ST_CREATED);
            ship.put("trackingNumber", trackingNumber);
            ship.put("itemDesc", item);
            ship.put("fee", fee);
            ship.put("km", km);
            ship.put("pickupCity", pickCity);
            ship.put("deliveryCity", useDefault ? "DEFAULT" : delCity);

            FirebaseBonus.upsertShipment(ship, String.valueOf(shipmentId));

            Map<String, Object> ev = new HashMap<>();
            ev.put("status", ST_CREATED);
            ev.put("location", pickCity);
            ev.put("details", "Order paid. Waiting for carrier.");
            ev.put("source", "manual");

            FirebaseBonus.addShipmentEvent(String.valueOf(shipmentId), ev);

            Toast.makeText(this, "Payment success! Request created.", Toast.LENGTH_SHORT).show();

            // Jump to tracking for this shipment
            Intent i = new Intent(this, CustomerTrackingActivity.class);
            i.putExtra(Keys.EXTRA_SHIPMENT_ID, shipmentId);
            startActivity(i);
            finish();
        });
    }
}