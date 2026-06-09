package com.example.bringo.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bringo.R;
import com.example.bringo.utils.Keys;

public class StartActivity extends AppCompatActivity {

    // Role values passed to Login/Register (must match what those screens expect)
    private static final String ROLE_USER = "USER";
    private static final String ROLE_CARRIER = "CARRIER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // Entry buttons for customer/carrier flows
        Button btnCustomerLogin = findViewById(R.id.btnCustomerLogin);
        Button btnCustomerRegister = findViewById(R.id.btnCustomerRegister);
        Button btnCarrierLogin = findViewById(R.id.btnCarrierLogin);
        Button btnCarrierRegister = findViewById(R.id.btnCarrierRegister);

        // Customer actions
        btnCustomerLogin.setOnClickListener(v -> openLogin(ROLE_USER));
        btnCustomerRegister.setOnClickListener(v -> openRegister(ROLE_USER));

        // Carrier actions
        btnCarrierLogin.setOnClickListener(v -> openLogin(ROLE_CARRIER));
        btnCarrierRegister.setOnClickListener(v -> openRegister(ROLE_CARRIER));
    }

    private void openLogin(String role) {
        // Opens login and tells it which role was chosen
        Intent i = new Intent(this, LoginActivity.class);
        i.putExtra(Keys.EXTRA_LOGIN_ROLE, role);
        startActivity(i);
    }

    private void openRegister(String role) {
        // Opens register and tells it which role was chosen
        Intent i = new Intent(this, RegisterActivity.class);
        i.putExtra(Keys.EXTRA_LOGIN_ROLE, role);
        startActivity(i);
    }
}