package com.zupay.smsreader;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.telephony.SmsMessage;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SmsReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        StringBuilder fullMsg = new StringBuilder();
        String sender = "";

        for (Object pdu : pdus) {
            SmsMessage sms = SmsMessage.createFromPdu((byte[]) pdu);
            sender = sms.getOriginatingAddress();
            fullMsg.append(sms.getMessageBody());
        }

        String msgBody = fullMsg.toString();

        // Check for 12-digit UTR number in Bank SMS
        if (msgBody.matches("(?s).*\\b\\d{12}\\b.*")) {
            SharedPreferences prefs = context.getSharedPreferences("ZuPayPrefs", Context.MODE_PRIVATE);
            String baseUrl = prefs.getString("server_url", "");
            String secret = prefs.getString("secret_key", "zupay123");
            String tgToken = prefs.getString("tg_token", "");
            String tgChat = prefs.getString("tg_chat", "");
            String callbackUrl = prefs.getString("callback_url", "");

            if (!baseUrl.isEmpty()) {
                sendToRailway(baseUrl + "/api/sms-webhook", secret, sender, msgBody, tgToken, tgChat, callbackUrl);
            }
        }
    }

    private void sendToRailway(String webhookUrl, String secret, String sender, String message, String tgToken, String tgChat, String callbackUrl) {
        new Thread(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String safeMsg = message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
                String json = String.format(
                    "{\"secret\":\"%s\",\"sender\":\"%s\",\"message\":\"%s\",\"tgBotToken\":\"%s\",\"tgChatId\":\"%s\",\"callbackUrl\":\"%s\"}",
                    secret, sender, safeMsg, tgToken, tgChat, callbackUrl
                );

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.getBytes("UTF-8"));
                }
                conn.getResponseCode();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
