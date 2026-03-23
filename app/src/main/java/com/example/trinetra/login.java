package com.example.trinetra;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class login extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvRegister, tvForget;

    String URL_LOGIN = "http://10.186.128.197/android_api/login.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);
        tvForget = findViewById(R.id.tvforget);

        btnLogin.setOnClickListener(v -> loginUser());

        tvRegister.setOnClickListener(v ->
                startActivity(new Intent(login.this, RegisterActivity.class)));

        tvForget.setOnClickListener(v ->
                startActivity(new Intent(login.this, ForgetPasswordActivity.class)));
    }

    private void loginUser() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter valid email");
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("email", email);
        params.put("password", password);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                URL_LOGIN,
                new JSONObject(params),

                response -> {
                    try {

                        String status = response.getString("status");
                        String message = response.getString("message");

                        if (status.equals("success")) {

                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                            Intent intent = new Intent(login.this, MainActivity.class);
                            startActivity(intent);
                            finish(); // prevent going back to login

                        } else {

                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },

                error -> Toast.makeText(this,
                        "Server Error: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show()
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}