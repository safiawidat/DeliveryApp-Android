package com.example.bringo.ui.customer;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bringo.R;
import com.example.bringo.ui.auth.StartActivity;
import com.example.bringo.utils.FirebaseBonus;
import com.example.bringo.utils.Session;

public class CustomerHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_customer_home);

        Button btnCreateRequest = findViewById(R.id.btnCreateRequest);
        Button btnTracking = findViewById(R.id.btnTracking);
        Button btnHistory = findViewById(R.id.btnHistory);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnCreateRequest.setOnClickListener(v ->
                startActivity(new Intent(this, CreateRequestActivity.class))
        );

        btnTracking.setOnClickListener(v ->
                startActivity(new Intent(this, CustomerOrdersActivity.class))
        );

        btnHistory.setOnClickListener(v ->
                startActivity(new Intent(this, CustomerHistoryActivity.class))
        );

        btnLogout.setOnClickListener(v -> {
            FirebaseBonus.signOut();
            Session.clear(this);

            Intent i = new Intent(this, StartActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }
}