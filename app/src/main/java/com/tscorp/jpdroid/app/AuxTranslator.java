package com.tscorp.jpdroid.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import java.util.ArrayList;

class AuxTranslator implements Runnable {

    public enum OutputMode {
        TEXT,
        TOAST
    }

    private final int atl_timeout;
    private final int atl_retries;
    private final int atl_port;
    private final String atl_host;
    private final String atl_token;
    private final String atl_cert;
    private final OutputMode out_mode;
    private ArrayList<String> atl_text;
    private Handler ui_handler;
    private boolean stopTranslation;

    AuxTranslator(String hostname, int port, int timeout, int retries, String token, String cert,
                  ArrayList<String> text, Handler uiHandler, OutputMode outMode) {
        atl_host = hostname;
        atl_port = port;
        atl_timeout = timeout;
        atl_retries = retries;
        atl_text = new ArrayList<String>(text);
        atl_token = token;
        atl_cert = cert;
        ui_handler = uiHandler;
        out_mode = outMode;
    }

    private void sendMessageWithString(String key, String value) {
        Message msg = ui_handler.obtainMessage();
        Bundle b = new Bundle();
        b.putString(key, value);
        msg.setData(b);
        ui_handler.sendMessage(msg);
    }

    private void sendProgressMsg(int progress) {
        Message msg = ui_handler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("progress", progress);
        msg.setData(b);
        ui_handler.sendMessage(msg);
    }

    private void sendAlertMsg(String s) {
        sendMessageWithString("message",s);
    }

    private void sendReadyText(String text) {
        sendMessageWithString("text", text);
    }

    private void sendReadyTextToast(String text) {
        sendMessageWithString("toast", text);
    }

    private void sendCleanupRequest() {
        sendMessageWithString("cleanup", "");
    }

    public void breakTranslation() {
        stopTranslation = true;
    }

    @Override
    public void run() {
        stopTranslation = false;
        String res = "";
        String a_msg = "";
        String lastTran = "";
        boolean simpleTran = (out_mode == OutputMode.TOAST);

        try {
            String c_msg = "";
            boolean tranComplete = false;
            sendAlertMsg("Connecting...");
            for (int i = 0; i < atl_retries; i++) {
                AtlasTranslator tran = new AtlasTranslator(atl_timeout);
                try {
                    if (tran.isConnected())
                        tran.doneTran();
                    if (tran.initTran(atl_host, atl_port, atl_token, atl_cert)) {
                        if (!simpleTran)
                            sendProgressMsg(0);
                        int idx = 0;
                        for (String txs : atl_text) {
                            if (stopTranslation) break;
                            String s = tran.tranString(txs);
                            lastTran = s;
                            res += txs + "\n" + s + "\n\n";
                            idx++;
                            if (!simpleTran)
                                sendProgressMsg(100 * idx / atl_text.size());
                        }
                        if (!simpleTran)
                            sendProgressMsg(100);
                        tranComplete = true;
                    }
                    tran.doneTran();
                } catch (Exception e) {
                    if (tran.isConnected())
                        tran.doneTran();
                    c_msg = e.getMessage();
                    Thread.sleep(atl_timeout * 1000);
                }
                if (tranComplete) break;
            }
            if (!tranComplete) {
                if (c_msg.isEmpty())
                    c_msg = "ATLAS initialization failure";
                throw new AtlasException(c_msg);
            }

        } catch (Exception e) {
            a_msg = e.getMessage();
        }
        if (!a_msg.isEmpty())
            sendAlertMsg(a_msg);
        else if (!res.isEmpty()) {
            if (simpleTran)
                sendReadyTextToast(lastTran);
            else
                sendReadyText(res);
        } else
            sendAlertMsg("Empty result");

        if (!simpleTran)
            sendProgressMsg(-1);

        sendCleanupRequest();
    }
}
