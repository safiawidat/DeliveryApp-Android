package com.example.bringo.ui.carrier;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bringo.R;
import com.example.bringo.data.dao.ShipmentsDao;
import com.example.bringo.utils.FirebaseBonus;
import com.example.bringo.utils.Keys;

import java.util.HashMap;
import java.util.Map;

public class CarrierJobActivity extends AppCompatActivity {

    private static final String ST_PICKED_UP = "PICKED_UP";
    private static final String ST_ON_THE_WAY = "ON_THE_WAY";
    private static final String ST_DELIVERED = "DELIVERED";

    private long shipmentId;
    private ShipmentsDao shipmentsDao;

    private TextView tvJobInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrier_job);

        shipmentId = getIntent().getLongExtra(Keys.EXTRA_SHIPMENT_ID, -1);
        if (shipmentId == -1) {
            finish();
            return;
        }

        shipmentsDao = new ShipmentsDao(this);

        tvJobInfo = findViewById(R.id.tvJobInfo);
        Button btnPickedUp = findViewById(R.id.btnPickedUp);
        Button btnOnTheWay = findViewById(R.id.btnOnTheWay);
        Button btnDelivered = findViewById(R.id.btnDelivered);
        Button btnBack = findViewById(R.id.btnBack);

        loadInfo();

        btnPickedUp.setOnClickListener(v -> {
            shipmentsDao.updateStatus(shipmentId, ST_PICKED_UP);
            Toast.makeText(this, "Status: PICKED_UP ✅", Toast.LENGTH_SHORT).show();

            // Firebase bonus (best-effort)
            pushStatusToFirebase(ST_PICKED_UP, "Carrier picked up the item.");

            loadInfo();
        });

        btnOnTheWay.setOnClickListener(v -> {
            shipmentsDao.updateStatus(shipmentId, ST_ON_THE_WAY);
            Toast.makeText(this, "Status: ON_THE_WAY ✅", Toast.LENGTH_SHORT).show();

            // Firebase bonus (best-effort)
            pushStatusToFirebase(ST_ON_THE_WAY, "Carrier is on the way.");

            loadInfo();
        });

        btnDelivered.setOnClickListener(v -> {
            boolean ok = shipmentsDao.markDeliveredAndPayCarrier(shipmentId);
            if (!ok) {
                Toast.makeText(this, "Deliver failed", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Delivered ✅ Balance updated 💰", Toast.LENGTH_SHORT).show();

            // Firebase bonus (best-effort)
            pushStatusToFirebase(ST_DELIVERED, "Delivered successfully.");

            startActivity(new Intent(this, CarrierHomeActivity.class));
            finish();
        });

        btnBack.setOnClickListener(v -> finish());
    }

    private void pushStatusToFirebase(String status, String details) {
        Map<String, Object> ship = new HashMap<>();
        ship.put("shipmentId", shipmentId);
        ship.put("status", status);

        FirebaseBonus.upsertShipment(ship, String.valueOf(shipmentId));

        Map<String, Object> ev = new HashMap<>();
        ev.put("status", status);
        ev.put("location", "");
        ev.put("details", details);
        ev.put("source", "carrier");

        FirebaseBonus.addShipmentEvent(String.valueOf(shipmentId), ev);
    }

    private void loadInfo() {
        Cursor c = shipmentsDao.getShipmentDetailsCursor(shipmentId);
        try {
            if (c.moveToFirst()) {
                String tracking = c.getString(c.getColumnIndexOrThrow("tracking_number"));
                String status = c.getString(c.getColumnIndexOrThrow("current_status"));
                String eta = c.getString(c.getColumnIndexOrThrow("eta"));
                String item = c.getString(c.getColumnIndexOrThrow("service_level"));
                double fee = c.getDouble(c.getColumnIndexOrThrow("declared_value"));

                tvJobInfo.setText(
                        "Shipment ID: " + shipmentId +
                                "\nTracking: " + tracking +
                                "\nItem: " + item +
                                "\nFee: " + fee + "₪" +
                                "\nETA: " + eta +
                                "\nCurrent: " + status
                );
            } else {
                tvJobInfo.setText("Shipment not found.");
            }
        } finally {
            c.close();
        }
    }
}