package com.example.bringo.ui.carrier;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bringo.R;
import com.example.bringo.data.dao.CarriersDao;
import com.example.bringo.data.dao.ShipmentsDao;
import com.example.bringo.data.dao.TrackingEventsDao;
import com.example.bringo.ui.auth.StartActivity;
import com.example.bringo.utils.FirebaseBonus;
import com.example.bringo.utils.Keys;
import com.example.bringo.utils.Session;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CarrierHomeActivity extends AppCompatActivity {

    private static final String STATUS_IN_TRANSIT = "IN_TRANSIT";

    private long carrierId;

    private CarriersDao carriersDao;
    private ShipmentsDao shipmentsDao;
    private TrackingEventsDao trackingEventsDao;

    private TextView tvBalance;
    private ListView lvRequests;
    private Button btnRefresh;
    private Button btnLogout;

    private SimpleCursorAdapter adapter;
    private Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_carrier_home);

        // DAOs for balance, requests list, and tracking updates
        carriersDao = new CarriersDao(this);
        shipmentsDao = new ShipmentsDao(this);
        trackingEventsDao = new TrackingEventsDao(this);

        // UI refs
        tvBalance = findViewById(R.id.tvBalance);
        lvRequests = findViewById(R.id.lvRequests);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnLogout = findViewById(R.id.btnLogout);

        // Resolve carrier id from session (or from logged user id)
        carrierId = resolveCarrierId();

        // Refresh balance + list
        btnRefresh.setOnClickListener(v -> refreshScreen());

        // Logout: clear session and reset task stack
        btnLogout.setOnClickListener(v -> {
            FirebaseBonus.signOut();
            Session.clear(this);

            Intent i = new Intent(this, StartActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });

        // Click behavior depends on whether the job is AVAILABLE or MY JOB
        lvRequests.setOnItemClickListener((parent, view, position, id) -> {
            long shipmentId = id;

            long assignedCarrierId = getAssignedCarrierIdFromRow(parent.getItemAtPosition(position));

            if (assignedCarrierId == -1) {
                // AVAILABLE -> allow accept
                new AlertDialog.Builder(this)
                        .setTitle("Accept request?")
                        .setMessage("Shipment ID: " + shipmentId)
                        .setPositiveButton("ACCEPT", (d, which) -> acceptRequest(shipmentId))
                        .setNegativeButton("CANCEL", null)
                        .show();
                return;
            }

            if (assignedCarrierId == carrierId) {
                // MY JOB -> resume
                openJob(shipmentId);
                return;
            }

            Toast.makeText(this, "Already accepted by another carrier.", Toast.LENGTH_SHORT).show();
        });

        refreshScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshScreen();
    }

    private long resolveCarrierId() {
        long cid = Session.getLoggedCarrierId(this);
        if (cid != -1) return cid;

        long userId = Session.getLoggedUserId(this);
        if (userId == -1) return -1;

        return carriersDao.getCarrierIdByCustomerId(userId);
    }

    private void refreshScreen() {
        if (carrierId == -1) {
            tvBalance.setText("₪0.00");
            loadRequests();
            return;
        }

        double balance = carriersDao.getBalance(carrierId);
        tvBalance.setText(String.format(Locale.US, "₪%.2f", balance));

        loadRequests();
    }

    private void acceptRequest(long shipmentId) {
        if (carrierId == -1) {
            Toast.makeText(this, "Carrier profile not found. Please login again.", Toast.LENGTH_SHORT).show();
            Session.clear(this);
            startActivity(new Intent(this, StartActivity.class));
            finish();
            return;
        }

        boolean assignedNow = shipmentsDao.tryAssignCarrier(shipmentId, carrierId);

        if (!assignedNow) {
            long existing = shipmentsDao.getCarrierId(shipmentId);
            if (existing == carrierId) {
                openJob(shipmentId);
                return;
            }

            Toast.makeText(this, "Request already accepted.", Toast.LENGTH_SHORT).show();
            refreshScreen();
            return;
        }

        // SQLite tracking event
        trackingEventsDao.insertEvent(
                shipmentId,
                STATUS_IN_TRANSIT,
                "Carrier assigned",
                "Carrier accepted the request. Heading to pickup.",
                "system"
        );

        // Firebase bonus (best-effort): update shipment + IN_TRANSIT event
        Map<String, Object> ship = new HashMap<>();
        ship.put("shipmentId", shipmentId);
        ship.put("carrierSqliteId", carrierId);
        ship.put("status", STATUS_IN_TRANSIT);

        FirebaseBonus.upsertShipment(ship, String.valueOf(shipmentId));

        Map<String, Object> ev = new HashMap<>();
        ev.put("status", STATUS_IN_TRANSIT);
        ev.put("location", "Carrier assigned");
        ev.put("details", "Carrier accepted the request. Heading to pickup.");
        ev.put("source", "system");

        FirebaseBonus.addShipmentEvent(String.valueOf(shipmentId), ev);

        openJob(shipmentId);
    }

    private void openJob(long shipmentId) {
        Intent i = new Intent(this, CarrierJobActivity.class);
        i.putExtra(Keys.EXTRA_SHIPMENT_ID, shipmentId);
        startActivity(i);
    }

    private void loadRequests() {
        Cursor newCursor = shipmentsDao.getOpenRequestsCursor(carrierId);

        if (adapter == null) {
            adapter = new SimpleCursorAdapter(
                    this,
                    android.R.layout.simple_list_item_2,
                    newCursor,
                    new String[]{"tracking_number", "current_status"},
                    new int[]{android.R.id.text1, android.R.id.text2},
                    0
            );

            adapter.setViewBinder((view, c, columnIndex) -> {
                if (view.getId() != android.R.id.text2) return false;

                long assignedCarrierId = -1;
                int idxAssigned = c.getColumnIndex("assigned_carrier_id");
                if (idxAssigned != -1 && !c.isNull(idxAssigned)) {
                    assignedCarrierId = c.getLong(idxAssigned);
                }

                String status = safeString(c, "current_status");
                String item = safeString(c, "service_level");

                String label;
                if (assignedCarrierId == -1) {
                    label = "🟩 AVAILABLE";
                } else if (assignedCarrierId == carrierId) {
                    label = "🟧 MY JOB";
                } else {
                    label = "⬜ TAKEN";
                }

                ((TextView) view).setText(label + " • " + status + " • " + item);
                return true;
            });

            lvRequests.setAdapter(adapter);
        } else {
            Cursor old = adapter.swapCursor(newCursor);
            if (old != null) old.close();
        }

        cursor = newCursor;
    }

    private long getAssignedCarrierIdFromRow(Object itemAtPosition) {
        try {
            Cursor row = (Cursor) itemAtPosition;
            int idx = row.getColumnIndex("assigned_carrier_id");
            if (idx != -1 && !row.isNull(idx)) {
                return row.getLong(idx);
            }
        } catch (Exception ignored) {
        }
        return -1;
    }

    private String safeString(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        if (idx == -1 || c.isNull(idx)) return "";
        String s = c.getString(idx);
        return s == null ? "" : s;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (adapter != null) {
            Cursor c = adapter.swapCursor(null);
            if (c != null) c.close();
        } else if (cursor != null) {
            cursor.close();
        }
    }
}