package com.zupay.smsreader;

import android.Manifest;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS}, 101);
        }

        SharedPreferences prefs = getSharedPreferences("ZuPayPrefs", MODE_PRIVATE);

        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 60, 50, 60);

        TextView title = new TextView(this);
        title.setText("ZuPay Control Panel (No Railway Vars Needed)");
        title.setTextSize(18f);
        layout.addView(title);

        EditText serverUrlInput = new EditText(this);
        serverUrlInput.setHint("Railway Domain (e.g. https://zupay.up.railway.app)");
        serverUrlInput.setText(prefs.getString("server_url", ""));
        layout.addView(serverUrlInput);

        EditText tgTokenInput = new EditText(this);
        tgTokenInput.setHint("Telegram Bot Token (123456:ABC...)");
        tgTokenInput.setText(prefs.getString("tg_token", ""));
        layout.addView(tgTokenInput);

        EditText tgChatInput = new EditText(this);
        tgChatInput.setHint("Telegram Chat/Group ID (-100123456789)");
        tgChatInput.setText(prefs.getString("tg_chat", ""));
        layout.addView(tgChatInput);

        EditText callbackInput = new EditText(this);
        callbackInput.setHint("Main Website Callback Webhook URL");
        callbackInput.setText(prefs.getString("callback_url", ""));
        layout.addView(callbackInput);

        EditText secretInput = new EditText(this);
        secretInput.setHint("Secret Key (Default: zupay123)");
        secretInput.setText(prefs.getString("secret_key", "zupay123"));
        layout.addView(secretInput);

        Button saveBtn = new Button(this);
        saveBtn.setText("Save & Sync Config to Railway");
        saveBtn.setOnClickListener(v -> {
            String baseUrl = serverUrlInput.getText().toString().trim().replaceAll("/+$", "");
            String tgToken = tgTokenInput.getText().toString().trim();
            String tgChat = tgChatInput.getText().toString().trim();
            String callbackUrl = callbackInput.getText().toString().trim();
            String secret = secretInput.getText().toString().trim();

            prefs.edit()
                .putString("server_url", baseUrl)
                .putString("tg_token", tgToken)
                .putString("tg_chat", tgChat)
                .putString("callback_url", callbackUrl)
                .putString("secret_key", secret)
                .apply();

            Toast.makeText(this, "Syncing with Railway Server...", Toast.LENGTH_SHORT).show();

            new Thread(() -> {
                try {
                    URL url = new URL(baseUrl + "/api/setup-config");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    String json = String.format(
                        "{\"tgBotToken\":\"%s\",\"tgChatId\":\"%s\",\"callbackUrl\":\"%s\",\"secretKey\":\"%s\"}",
                        tgToken, tgChat, callbackUrl, secret
                    );

                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(json.getBytes("UTF-8"));
                    }

                    int code = conn.getResponseCode();
                    runOnUiThread(() -> {
                        if (code == 200) {
                            Toast.makeText(this, "✅ Connected! Check Telegram for WhatsApp QR!", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "⚠️ Server Error Code: " + code, Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "❌ Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                }
            }).start();
        });

        layout.addView(saveBtn);
        scroll.addView(layout);
        setContentView(scroll);
    }
}
