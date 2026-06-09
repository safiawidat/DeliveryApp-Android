package com.example.bringo.ui.customer;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bringo.R;
import com.example.bringo.data.dao.ShipmentsDao;
import com.example.bringo.data.dao.TrackingEventsDao;
import com.example.bringo.utils.Keys;

public class CustomerTrackingActivity extends AppCompatActivity {

    private static final String ST_CREATED = "CREATED";

    private long shipmentId;

    private ShipmentsDao shipmentsDao;
    private TrackingEventsDao trackingEventsDao;

    private TextView tvTrackingTitle, tvCarrier, tvStatus, tvEta, tvTimeline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_tracking);

        // Shipment id passed via Keys.EXTRA_SHIPMENT_ID
        shipmentId = getIntent().getLongExtra(Keys.EXTRA_SHIPMENT_ID, -1);
        if (shipmentId == -1) {
            goHome();
            return;
        }

        // DAOs for shipment + timeline info
        shipmentsDao = new ShipmentsDao(this);
        trackingEventsDao = new TrackingEventsDao(this);

        // UI refs
        tvTrackingTitle = findViewById(R.id.tvTrackingTitle);
        tvCarrier = findViewById(R.id.tvCarrier);
        tvStatus = findViewById(R.id.tvStatus);
        tvEta = findViewById(R.id.tvEta);
        tvTimeline = findViewById(R.id.tvTimeline);

        Button btnBackHome = findViewById(R.id.btnBackHome);
        btnBackHome.setOnClickListener(v -> goHome());

        loadTracking();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTracking();
    }

    private void goHome() {
        // Return to customer home
        startActivity(new Intent(this, CustomerHomeActivity.class));
        finish();
    }

    private void loadTracking() {
        // Loads shipment details for header fields
        Cursor c = shipmentsDao.getShipmentDetailsCursor(shipmentId);

        try {
            if (!c.moveToFirst()) {
                tvTrackingTitle.setText("Shipment not found");
                tvCarrier.setText("");
                tvStatus.setText("");
                tvEta.setText("");
                tvTimeline.setText(buildTimelineFromDb());
                return;
            }

            String tracking = c.getString(c.getColumnIndexOrThrow("tracking_number"));
            String carrierName = c.getString(c.getColumnIndexOrThrow("carrier_name"));
            String status = c.getString(c.getColumnIndexOrThrow("current_status"));
            String eta = c.getString(c.getColumnIndexOrThrow("eta"));

            if (TextUtils.isEmpty(carrierName)) carrierName = "Unassigned";
            if (TextUtils.isEmpty(eta)) eta = "-";
            if (TextUtils.isEmpty(status)) status = ST_CREATED;

            tvTrackingTitle.setText("Tracking: " + tracking);
            tvCarrier.setText("Carrier: " + carrierName);
            tvStatus.setText("Status: " + status);
            tvEta.setText("ETA: " + eta);

            // Keep your current behavior: simple timeline based on current status
            tvTimeline.setText(buildTimeline(status));
        } finally {
            c.close();
        }
    }

    private String buildTimeline(String status) {
        // Simple progress view from a status string
        String[] steps = {"CREATED", "IN_TRANSIT", "PICKED_UP", "ON_THE_WAY", "DELIVERED"};

        int current = -1;
        for (int i = 0; i < steps.length; i++) {
            if (steps[i].equals(status)) {
                current = i;
                break;
            }
        }

        if (current == -1) return "⏳ " + status;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.length; i++) {
            String prefix = (i < current) ? "✅ " : (i == current ? "⏳ " : "⬜ ");

            // UI label tweak (keeps existing behavior)
            String label = steps[i].equals("IN_TRANSIT") ? "ACCEPTED" : steps[i];

            sb.append(prefix).append(label);
            if (i != steps.length - 1) sb.append("\n");
        }

        return sb.toString();
    }

    private String buildTimelineFromDb() {
        // Builds a detailed timeline from tracking_events table
        Cursor c = trackingEventsDao.getEventsCursor(shipmentId);

        if (c == null || !c.moveToFirst()) {
            if (c != null) c.close();
            return "No tracking events yet.";
        }

        StringBuilder sb = new StringBuilder();
        do {
            String at = c.getString(c.getColumnIndexOrThrow("occurred_at"));
            String st = c.getString(c.getColumnIndexOrThrow("status"));
            String loc = c.getString(c.getColumnIndexOrThrow("location_desc"));
            String det = c.getString(c.getColumnIndexOrThrow("details"));

            sb.append("• ").append(at).append("  |  ").append(st);

            if (!TextUtils.isEmpty(loc)) sb.append("  @ ").append(loc);
            if (!TextUtils.isEmpty(det)) sb.append("\n  ").append(det);

            sb.append("\n\n");
        } while (c.moveToNext());

        c.close();
        return sb.toString().trim();
    }
}