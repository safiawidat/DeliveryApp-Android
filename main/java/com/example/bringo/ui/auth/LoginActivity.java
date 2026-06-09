package com.example.bringo.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bringo.R;
import com.example.bringo.data.dao.CarriersDao;
import com.example.bringo.data.dao.CustomersDao;
import com.example.bringo.ui.carrier.CarrierHomeActivity;
import com.example.bringo.ui.customer.CustomerHomeActivity;
import com.example.bringo.utils.FirebaseBonus;
import com.example.bringo.utils.Keys;
import com.example.bringo.utils.Session;

public class LoginActivity extends AppCompatActivity {

    private static final String ROLE_USER = "USER";
    private static final String ROLE_CARRIER = "CARRIER";

    private EditText etEmail;
    private EditText etPassword;

    private CustomersDao customersDao;
    private String wantedRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        wantedRole = getIntent().getStringExtra(Keys.EXTRA_LOGIN_ROLE);
        if (wantedRole == null) wantedRole = ROLE_USER;

        customersDao = new CustomersDao(this);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnGoRegister = findViewById(R.id.btnGoRegister);
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);

        tvSubtitle.setText(ROLE_CARRIER.equals(wantedRole) ? "Login as Carrier" : "Login as Customer");

        String prefill = getIntent().getStringExtra(Keys.EXTRA_PREFILL_EMAIL);
        if (!TextUtils.isEmpty(prefill)) {
            etEmail.setText(prefill);
        }

        btnLogin.setOnClickListener(v -> doLogin());

        btnGoRegister.setOnClickListener(v -> {
            Intent i = new Intent(this, RegisterActivity.class);
            i.putExtra(Keys.EXTRA_LOGIN_ROLE, wantedRole);
            startActivity(i);
        });
    }

    private void doLogin() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        if (!com.example.bringo.utils.Validation.isValidEmail(email)) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return;
        }

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass)) {
            Toast.makeText(this, "Please fill email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        long userId = customersDao.login(email, pass);
        if (userId == -1) {
            Toast.makeText(this, "Wrong login", Toast.LENGTH_SHORT).show();
            return;
        }

        String realRole = customersDao.getCustomerType(userId);
        if (realRole == null) realRole = ROLE_USER;

        if (!wantedRole.equals(realRole)) {
            Toast.makeText(this, "Wrong login type. Use " + realRole + " login.", Toast.LENGTH_SHORT).show();
            return;
        }

        long carrierId = -1;
        if (ROLE_CARRIER.equals(realRole)) {
            CarriersDao carriersDao = new CarriersDao(this);
            carrierId = carriersDao.getCarrierIdByCustomerId(userId);

            if (carrierId == -1) {
                Toast.makeText(this, "Carrier profile not found. Please register as Carrier.", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Session.saveLogin(this, userId, realRole, email, carrierId);

        // Firebase bonus (best-effort)
        FirebaseBonus.ensureSignedIn(email, pass);
        FirebaseBonus.upsertUserProfile(userId, realRole, email, carrierId);

        if (ROLE_CARRIER.equals(realRole)) {
            startActivity(new Intent(this, CarrierHomeActivity.class));
        } else {
            startActivity(new Intent(this, CustomerHomeActivity.class));
        }

        finish();
    }
}