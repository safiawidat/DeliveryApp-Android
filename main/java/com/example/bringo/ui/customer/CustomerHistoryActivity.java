package com.example.bringo.ui.customer;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bringo.R;
import com.example.bringo.data.dao.ShipmentsDao;
import com.example.bringo.ui.auth.StartActivity;
import com.example.bringo.utils.Keys;
import com.example.bringo.utils.Session;

public class CustomerHistoryActivity extends AppCompatActivity {

    private ShipmentsDao shipmentsDao;
    private ListView lvHistory;

    private SimpleCursorAdapter adapter;
    private Cursor cursor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_history);

        // Must be logged in
        long customerId = Session.getLoggedUserId(this);
        if (customerId == -1) {
            startActivity(new Intent(this, StartActivity.class));
            finish();
            return;
        }

        shipmentsDao = new ShipmentsDao(this);
        lvHistory = findViewById(R.id.lvHistory);

        // Click opens tracking for that shipment
        lvHistory.setOnItemClickListener((p, v, pos, shipmentId) -> {
            Intent i = new Intent(this, CustomerTrackingActivity.class);
            i.putExtra(Keys.EXTRA_SHIPMENT_ID, shipmentId);
            startActivity(i);
        });

        loadList(customerId);
    }

    @Override
    protected void onResume() {
        super.onResume();

        long customerId = Session.getLoggedUserId(this);
        if (customerId == -1) {
            startActivity(new Intent(this, StartActivity.class));
            finish();
            return;
        }

        loadList(customerId);
    }

    private void loadList(long customerId) {
        // Loads all shipments for the logged-in customer
        Cursor newCursor = shipmentsDao.getCustomerDeliveredShipmentsCursor(customerId, "");
        if (adapter == null) {
            adapter = new SimpleCursorAdapter(
                    this,
                    android.R.layout.simple_list_item_2,
                    newCursor,
                    new String[]{"tracking_number", "current_status"},
                    new int[]{android.R.id.text1, android.R.id.text2},
                    0
            );
            lvHistory.setAdapter(adapter);
        } else {
            Cursor old = adapter.swapCursor(newCursor);
            if (old != null) old.close();
        }

        cursor = newCursor;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Close active cursor to avoid leaks
        if (adapter != null) {
            Cursor c = adapter.swapCursor(null);
            if (c != null) c.close();
        } else if (cursor != null) {
            cursor.close();
        }
    }
}