package com.example.bringo.ui.customer;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.bringo.R;
import com.example.bringo.data.dao.ShipmentsDao;
import com.example.bringo.ui.auth.StartActivity;
import com.example.bringo.utils.Keys;
import com.example.bringo.utils.Session;

public class CustomerOrdersActivity extends AppCompatActivity {

    private ShipmentsDao shipmentsDao;

    private ListView lvMyOrders;
    private EditText etSearch;
    private Button btnSearch;
    private Button btnClear;

    private SimpleCursorAdapter adapter;
    private Cursor cursor;

    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_orders);

        // DAO + UI refs
        shipmentsDao = new ShipmentsDao(this);
        lvMyOrders = findViewById(R.id.lvMyOrders);

        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        btnClear = findViewById(R.id.btnClear);

        // Search actions
        btnSearch.setOnClickListener(v -> {
            currentQuery = etSearch.getText().toString().trim();
            loadList();
        });

        btnClear.setOnClickListener(v -> {
            currentQuery = "";
            etSearch.setText("");
            loadList();
        });

        // Click opens tracking
        lvMyOrders.setOnItemClickListener((parent, view, position, id) -> {
            Intent i = new Intent(this, CustomerTrackingActivity.class);
            i.putExtra(Keys.EXTRA_SHIPMENT_ID, id);
            startActivity(i);
        });

        // Long-press deletes ONLY if not accepted yet
        lvMyOrders.setOnItemLongClickListener((parent, view, position, id) -> {
            long shipmentId = id;
            confirmDelete(shipmentId);
            return true;
        });

        loadList();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadList();
    }

    private void confirmDelete(long shipmentId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete order?")
                .setMessage("You can delete only orders that were NOT accepted by a carrier yet.")
                .setPositiveButton("DELETE", (d, which) -> deleteIfAllowed(shipmentId))
                .setNegativeButton("CANCEL", null)
                .show();
    }

    private void deleteIfAllowed(long shipmentId) {
        long customerId = Session.getLoggedUserId(this);
        if (customerId == -1) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, StartActivity.class));
            finish();
            return;
        }

        boolean deleted = shipmentsDao.deleteUnacceptedCustomerShipment(shipmentId, customerId);
        if (!deleted) {
            Toast.makeText(this, "Can't delete: already accepted (or delivered).", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Order deleted ✅", Toast.LENGTH_SHORT).show();
        loadList();
    }

    private void loadList() {
        long customerId = Session.getLoggedUserId(this);
        if (customerId == -1) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, StartActivity.class));
            finish();
            return;
        }

        // Cursor with optional search filter
        Cursor newCursor = shipmentsDao.getCustomerActiveShipmentsCursor(customerId, currentQuery);
        if (adapter == null) {
            adapter = new SimpleCursorAdapter(
                    this,
                    android.R.layout.simple_list_item_2,
                    newCursor,
                    new String[]{"tracking_number", "current_status"},
                    new int[]{android.R.id.text1, android.R.id.text2},
                    0
            );
            lvMyOrders.setAdapter(adapter);
        } else {
            Cursor old = adapter.swapCursor(newCursor);
            if (old != null) old.close();
        }

        cursor = newCursor;
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