package com.example.bringo.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bringo.R;
import com.example.bringo.ui.auth.StartActivity;
import com.example.bringo.utils.Keys;
import com.example.bringo.utils.Session;

public class CreateRequestActivity extends AppCompatActivity {

    // Fee calculation (used only to show checkout values)
    private static final double BASE_FEE = 10.0;
    private static final double FEE_PER_KM = 2.0;

    private EditText etItem;

    private EditText etPickLine1, etPickCity, etPickPostal, etPickX, etPickY;

    private CheckBox cbUseDefaultDelivery;
    private View boxDelivery;
    private EditText etDelLine1, etDelCity, etDelPostal, etDelX, etDelY;

    private Button btnContinue, btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_request);

        // Must be logged in as customer to create a request
        long userId = Session.getLoggedUserId(this);
        if (userId == -1) {
            startActivity(new Intent(this, StartActivity.class));
            finish();
            return;
        }

        bindViews();
        setupUi();
    }

    private void bindViews() {
        // Item
        etItem = findViewById(R.id.etItem);

        // Pickup address + simulation location
        etPickLine1 = findViewById(R.id.etPickLine1);
        etPickCity = findViewById(R.id.etPickCity);
        etPickPostal = findViewById(R.id.etPickPostal);
        etPickX = findViewById(R.id.etPickX);
        etPickY = findViewById(R.id.etPickY);

        // Delivery toggle + delivery address + simulation location
        cbUseDefaultDelivery = findViewById(R.id.cbUseDefaultDelivery);
        boxDelivery = findViewById(R.id.boxDelivery);

        etDelLine1 = findViewById(R.id.etDelLine1);
        etDelCity = findViewById(R.id.etDelCity);
        etDelPostal = findViewById(R.id.etDelPostal);
        etDelX = findViewById(R.id.etDelX);
        etDelY = findViewById(R.id.etDelY);

        // Buttons
        btnContinue = findViewById(R.id.btnContinue);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupUi() {
        // Default delivery address hides the delivery form
        cbUseDefaultDelivery.setChecked(true);
        boxDelivery.setVisibility(View.GONE);

        cbUseDefaultDelivery.setOnCheckedChangeListener((btn, checked) ->
                boxDelivery.setVisibility(checked ? View.GONE : View.VISIBLE)
        );

        btnBack.setOnClickListener(v -> finish());

        btnContinue.setOnClickListener(v -> {
            Intent checkoutIntent = buildCheckoutIntent();
            if (checkoutIntent != null) {
                startActivity(checkoutIntent);
            }
        });
    }

    private Intent buildCheckoutIntent() {
        // Item description is required
        String item = safe(etItem);
        if (TextUtils.isEmpty(item)) {
            toast("Please enter item description");
            return null;
        }

        // Pickup fields
        String pickLine1 = safe(etPickLine1);
        String pickCity = safe(etPickCity);
        String pickPostal = safe(etPickPostal);

        Double pickX = parseDouble(etPickX, "Pickup X");
        Double pickY = parseDouble(etPickY, "Pickup Y");
        if (pickX == null || pickY == null) return null;

        boolean useDefaultDelivery = cbUseDefaultDelivery.isChecked();

        // Delivery fields (only required if not using default)
        String delLine1 = safe(etDelLine1);
        String delCity = safe(etDelCity);
        String delPostal = safe(etDelPostal);

        Double delX;
        Double delY;

        if (useDefaultDelivery) {
            // Checkout will fetch the real default address; we keep current behavior
            delX = 0.0;
            delY = 0.0;
        } else {
            delX = parseDouble(etDelX, "Delivery X");
            delY = parseDouble(etDelY, "Delivery Y");
            if (delX == null || delY == null) return null;

            if (TextUtils.isEmpty(delCity) || TextUtils.isEmpty(delLine1)) {
                toast("Please fill delivery address");
                return null;
            }
        }

        // Simulated distance + fee
        double km = distance(pickX, pickY, delX, delY);
        double fee = BASE_FEE + (km * FEE_PER_KM);

        // Move to checkout with all required extras
        Intent i = new Intent(this, RequestCheckoutActivity.class);

        i.putExtra(Keys.EXTRA_ITEM_DESC, item);

        i.putExtra(Keys.EXTRA_PICK_LINE1, pickLine1);
        i.putExtra(Keys.EXTRA_PICK_CITY, pickCity);
        i.putExtra(Keys.EXTRA_PICK_POSTAL, pickPostal);
        i.putExtra(Keys.EXTRA_PICK_LAT, pickX);
        i.putExtra(Keys.EXTRA_PICK_LON, pickY);

        i.putExtra(Keys.EXTRA_USE_DEFAULT_DELIVERY, useDefaultDelivery);

        i.putExtra(Keys.EXTRA_DEL_LINE1, delLine1);
        i.putExtra(Keys.EXTRA_DEL_CITY, delCity);
        i.putExtra(Keys.EXTRA_DEL_POSTAL, delPostal);
        i.putExtra(Keys.EXTRA_DEL_LAT, delX);
        i.putExtra(Keys.EXTRA_DEL_LON, delY);

        i.putExtra(Keys.EXTRA_KM, km);
        i.putExtra(Keys.EXTRA_FEE, fee);

        return i;
    }

    private String safe(EditText et) {
        // Reads trimmed text safely
        return et.getText().toString().trim();
    }

    private Double parseDouble(EditText et, String fieldName) {
        // Parses a number field for coordinates
        try {
            return Double.parseDouble(et.getText().toString().trim());
        } catch (Exception e) {
            toast("Invalid number: " + fieldName);
            return null;
        }
    }

    private double distance(double x1, double y1, double x2, double y2) {
        // Euclidean distance between two points
        return Math.hypot(x2 - x1, y2 - y1);
    }

    private void toast(String msg) {
        // Small user feedback
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}