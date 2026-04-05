package com.example.trinetra;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private static final int PICK_FILE_REQUEST = 1;

    Button btnChoose, btnUpload;
    TextView txtFileName, txtResult;
    ProgressBar progressBar;

    Uri selectedFileUri = null;
    String selectedFileName = "";

    String uploadUrl = "http://10.52.79.182/android_api/upload_detect.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnChoose = findViewById(R.id.btnChoose);
        btnUpload = findViewById(R.id.btnUpload);
        txtFileName = findViewById(R.id.txtFileName);
        txtResult = findViewById(R.id.txtResult);
        progressBar = findViewById(R.id.progressBar);

        btnChoose.setOnClickListener(v -> chooseFile());

        btnUpload.setOnClickListener(v -> {
            if (selectedFileUri == null) {
                Toast.makeText(this, "Please select image first", Toast.LENGTH_SHORT).show();
                return;
            }

            txtResult.setText(""); // 🔥 clear old result

            String type = getContentResolver().getType(selectedFileUri);

            if (type == null || !type.startsWith("image/")) {
                Toast.makeText(this, "Only image allowed!", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadFile();
        });
    }

    private void chooseFile() {
        selectedFileUri = null; // 🔥 reset
        selectedFileName = "";

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_FILE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            selectedFileName = getFileName(selectedFileUri);

            txtFileName.setText("Selected: " + selectedFileName);
            txtResult.setText("Ready to upload...");
        }
    }

    @SuppressLint("Range")
    private String getFileName(Uri uri) {
        String result = "image_file";

        if (uri != null && "content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) cursor.close();
            }
        }

        return result;
    }

    private void uploadFile() {
        progressBar.setVisibility(View.VISIBLE);
        txtResult.setText("Uploading & detecting...");

        new Thread(() -> {
            try {
                String boundary = "----Boundary" + System.currentTimeMillis();

                URL url = new URL(uploadUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");

                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream request = new DataOutputStream(connection.getOutputStream());

                // 🔥 FILE PART
                request.writeBytes("--" + boundary + "\r\n");
                request.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"" + selectedFileName + "\"\r\n");

                // ✅ FIX MIME TYPE
                String mimeType = getContentResolver().getType(selectedFileUri);
                if (mimeType == null) mimeType = "image/jpeg";

                Log.d("MIME_TYPE", mimeType);

                request.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");

                InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);

                if (inputStream == null) {
                    runOnUiThread(() -> txtResult.setText("Failed to read file"));
                    return;
                }

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    request.write(buffer, 0, bytesRead);
                }

                inputStream.close();

                request.writeBytes("\r\n");
                request.writeBytes("--" + boundary + "--\r\n");

                request.flush();
                request.close();

                int responseCode = connection.getResponseCode();

                InputStream responseStream = (responseCode == HttpURLConnection.HTTP_OK)
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                StringBuilder response = new StringBuilder();

                byte[] respBuffer = new byte[1024];
                int len;

                while ((len = responseStream.read(respBuffer)) != -1) {
                    response.append(new String(respBuffer, 0, len));
                }

                responseStream.close();
                connection.disconnect(); // 🔥 IMPORTANT

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);

                    try {
                        JSONObject json = new JSONObject(response.toString());

                        String status = json.optString("status");
                        String message = json.optString("message");

                        JSONObject data = json.optJSONObject("data");

                        String prediction = "unknown";
                        double confidence = 0;
                        String type = "image";

                        if (data != null) {
                            prediction = data.optString("prediction", "unknown");
                            confidence = data.optDouble("confidence", 0);
                            type = data.optString("media_type", "image");
                        }

                        txtResult.setText(
                                "Status: " + status + "\n" +
                                        "Message: " + message + "\n" +
                                        "Prediction: " + prediction + "\n" +
                                        "Confidence: " + (confidence * 100) + "%\n" +
                                        "Type: " + type + "\n\n" +
                                        "Raw:\n" + response.toString()
                        );

                    } catch (Exception e) {
                        txtResult.setText("Error parsing:\n" + response.toString());
                    }
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    txtResult.setText("Upload failed: " + e.getMessage());
                });
            }
        }).start();
    }
    }
