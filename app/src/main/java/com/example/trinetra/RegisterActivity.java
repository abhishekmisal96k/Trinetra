package com.example.trinetra;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnRegister;

    String URL_REGISTER = "http://10.186.128.197/android_api/register.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnregister);

        btnRegister.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Check empty fields
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter valid email");
            etEmail.requestFocus();
            return;
        }

        // Validate password length (4 - 15)
        if (password.length() < 4 || password.length() > 15) {
            etPassword.setError("Password must be between 4 and 15 characters");
            etPassword.requestFocus();
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("email", email);
        params.put("password", password);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                URL_REGISTER,
                new JSONObject(params),

                response -> {
                    String status = response.optString("status");
                    String message = response.optString("message");

                    Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_SHORT).show();

                    if ("success".equals(status)) {

                        // Go to login screen
                        Intent intent = new Intent(RegisterActivity.this, login.class);
                        startActivity(intent);
                        finish();
                    }
                },

                error -> Toast.makeText(RegisterActivity.this,
                        "Server Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}