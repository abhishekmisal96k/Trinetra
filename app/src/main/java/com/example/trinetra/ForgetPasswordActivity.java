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

public class ForgetPasswordActivity extends AppCompatActivity {

    EditText etEmail, etOtp, etNewPassword;
    Button btnSendOtp, btnReset;

    String URL_SEND_OTP = "http://10.186.128.197/android_api/send_otp.php";
    String URL_RESET = "http://10.186.128.197/android_api/reset_password.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forget_password);

        etEmail = findViewById(R.id.etEmail);
        etOtp = findViewById(R.id.etOtp);
        etNewPassword = findViewById(R.id.etNewPassword);

        btnSendOtp = findViewById(R.id.btnSendOtp);
        btnReset = findViewById(R.id.btnReset);

        btnSendOtp.setOnClickListener(v -> sendOtp());

        btnReset.setOnClickListener(v -> resetPassword());
    }

    private void sendOtp() {

        String email = etEmail.getText().toString().trim();

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter valid email");
            return;
        }

        Map<String,String> params = new HashMap<>();
        params.put("email", email);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                URL_SEND_OTP,
                new JSONObject(params),

                response -> {
                    String message = response.optString("message");
                    Toast.makeText(this,message,Toast.LENGTH_SHORT).show();
                },

                error -> Toast.makeText(this,
                        "Server Error",Toast.LENGTH_SHORT).show()
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    private void resetPassword() {

        String email = etEmail.getText().toString().trim();
        String otp = etOtp.getText().toString().trim();
        String password = etNewPassword.getText().toString().trim();

        if(email.isEmpty() || otp.isEmpty() || password.isEmpty()){
            Toast.makeText(this,"Fill all fields",Toast.LENGTH_SHORT).show();
            return;
        }

        if(password.length() < 4 || password.length() > 15){
            etNewPassword.setError("Password must be 4-15 characters");
            return;
        }

        Map<String,String> params = new HashMap<>();
        params.put("email",email);
        params.put("otp",otp);
        params.put("password",password);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                URL_RESET,
                new JSONObject(params),

                response -> {

                    String status = response.optString("status");
                    String message = response.optString("message");

                    Toast.makeText(this,message,Toast.LENGTH_SHORT).show();

                    if(status.equals("success")){

                        Intent intent =
                                new Intent(ForgetPasswordActivity.this, login.class);
                        startActivity(intent);
                        finish();
                    }
                },

                error -> Toast.makeText(this,
                        "Server Error",Toast.LENGTH_SHORT).show()
        );

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }
}