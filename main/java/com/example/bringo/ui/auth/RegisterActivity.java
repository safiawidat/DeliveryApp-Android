package com.example.bringo.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bringo.R;
import com.example.bringo.data.dao.AddressesDao;
import com.example.bringo.data.dao.CarriersDao;
import com.example.bringo.data.dao.CustomersDao;
import com.example.bringo.utils.FirebaseBonus;
import com.example.bringo.utils.Keys;
import com.example.bringo.utils.Validation;

public class RegisterActivity extends AppCompatActivity {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_CARRIER = "CARRIER";

    private EditText etFullName, etEmail, etPassword, etPhone;
    private EditText etIdNumber, etCarPlate;
    private EditText etCity, etStreet, etLocX, etLocY;
    private Button btnRegister;

    private CustomersDao customersDao;
    private CarriersDao carriersDao;
    private AddressesDao addressesDao;

    private String role;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        role = getIntent().getStringExtra(Keys.EXTRA_LOGIN_ROLE);
        if (role == null) role = ROLE_USER;

        customersDao = new CustomersDao(this);
        carriersDao = new CarriersDao(this);
        addressesDao = new AddressesDao(this);

        initViews();
        setupScreenByRole();

        btnRegister.setOnClickListener(v -> doRegister());
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etPhone = findViewById(R.id.etPhone);

        etIdNumber = findViewById(R.id.etIdNumber);
        etCarPlate = findViewById(R.id.etCarPlate);

        etCity = findViewById(R.id.etCity);
        etStreet = findViewById(R.id.etStreet);
        etLocX = findViewById(R.id.etLocX);
        etLocY = findViewById(R.id.etLocY);

        btnRegister = findViewById(R.id.btnRegister);
    }

    private void setupScreenByRole() {
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        if (tvSubtitle != null) {
            tvSubtitle.setText(ROLE_CARRIER.equals(role) ? "Register Carrier" : "Register Customer");
        }

        // Customer flow doesn't need car plate UI (keep your existing behavior)
        if (ROLE_USER.equals(role)) {
            etCarPlate.setVisibility(View.GONE);

            View divAfterCarPlate = findViewById(R.id.divAfterCarPlate);
            if (divAfterCarPlate != null) divAfterCarPlate.setVisibility(View.GONE);
        }
    }

    private void doRegister() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        String idNum = etIdNumber.getText().toString().trim();
        String carPlate = etCarPlate.getText().toString().trim();

        String city = etCity.getText().toString().trim();
        String street = etStreet.getText().toString().trim();

        // Required basics
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(pass) || TextUtils.isEmpty(phone)) {
            toast("Fill name, email, password, and phone");
            return;
        }

        // Validations
        if (!Validation.isValidName(name)) {
            toast("Name must not contain numbers");
            return;
        }

        if (!Validation.isValidEmail(email)) {
            toast("Invalid email format");
            return;
        }

        if (pass.length() < 6) {
            toast("Password must be at least 6 characters");
            return;
        }

        if (!Validation.isValidPhoneIL(phone)) {
            toast("Phone must be 10 digits (starts with 0)");
            return;
        }

        if (TextUtils.isEmpty(city) || TextUtils.isEmpty(street)) {
            toast("Please fill city and street");
            return;
        }

        // Carrier-only validations
        if (ROLE_CARRIER.equals(role)) {
            if (TextUtils.isEmpty(idNum) || !Validation.isValidIsraeliId(idNum)) {
                toast("Invalid ID number");
                return;
            }
            if (TextUtils.isEmpty(carPlate) || !Validation.isValidCarPlate(carPlate)) {
                toast("Car plate must be 7-8 digits");
                return;
            }
        }

        // Coordinates must be numeric
        double x, y;
        try {
            x = Double.parseDouble(etLocX.getText().toString().trim());
            y = Double.parseDouble(etLocY.getText().toString().trim());
        } catch (Exception e) {
            toast("Invalid coordinates");
            return;
        }

        // Your enforced range
        if (x < 0 || x > 50 || y < 0 || y > 50) {
            toast("Coordinates must be between 0 and 50");
            return;
        }

        // If email already exists
        if (customersDao.emailExists(email)) {
            long existingId = customersDao.getCustomerIdByEmail(email);
            String existingRole = customersDao.getCustomerType(existingId);

            if (!role.equals(existingRole)) {
                toast("Account exists as " + existingRole);
                return;
            }

            // If it’s a carrier account missing carrier row, complete it
            if (ROLE_CARRIER.equals(role)) {
                long carrierId = carriersDao.getCarrierIdByCustomerId(existingId);
                if (carrierId == -1) {
                    long addrId = addressesDao.insertAddress(street, city, "", "IL", x, y);
                    customersDao.updateUserBasics(existingId, name, phone, pass, addrId);
                    carriersDao.insertCarrier(existingId, idNum, carPlate, "", "");
                    toast("Carrier profile completed");
                }
            }

            long finalCarrierId = ROLE_CARRIER.equals(role)
                    ? carriersDao.getCarrierIdByCustomerId(existingId)
                    : -1;

            // Firebase bonus (best-effort)
            FirebaseBonus.ensureSignedIn(email, pass);
            FirebaseBonus.upsertUserProfile(existingId, role, email, finalCarrierId);

            goToLogin(role, email);
            finish();
            return;
        }

        // New user creation
        long addrId = addressesDao.insertAddress(street, city, "", "IL", x, y);
        long userId = customersDao.insertUser(name, email, phone, pass, addrId, role);

        if (ROLE_CARRIER.equals(role)) {
            carriersDao.insertCarrier(userId, idNum, carPlate, "", "");
        }

        long carrierId = ROLE_CARRIER.equals(role)
                ? carriersDao.getCarrierIdByCustomerId(userId)
                : -1;

        // Firebase bonus (best-effort)
        FirebaseBonus.ensureSignedIn(email, pass);
        FirebaseBonus.upsertUserProfile(userId, role, email, carrierId);

        toast("Account created");
        goToLogin(role, email);
        finish();
    }

    private void goToLogin(String role, String email) {
        Intent i = new Intent(this, LoginActivity.class);
        i.putExtra(Keys.EXTRA_LOGIN_ROLE, role);
        i.putExtra(Keys.EXTRA_PREFILL_EMAIL, email);
        startActivity(i);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}