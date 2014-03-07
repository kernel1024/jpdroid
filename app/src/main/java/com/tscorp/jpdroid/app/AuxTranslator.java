package com.tscorp.jpdroid.app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

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
    private final OutputMode out_mode;
    private ArrayList<String> atl_text;
    private Handler ui_handler;

    AuxTranslator(String hostname, int port, int timeout, int retries, ArrayList<String> text,
                  Handler uiHandler, OutputMode outMode) {
        atl_host = hostname;
        atl_port = port;
        atl_timeout = timeout;
        atl_retries = retries;
        atl_text = new ArrayList<String>(text);
        ui_handler = uiHandler;
        out_mode = outMode;
    }

    private void sendProgressMsg(int progress) {
        Message msg = ui_handler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("progress", progress);
        msg.setData(b);
        ui_handler.sendMessage(msg);
    }

    private void sendAlertMsg(String s) {
        Message msg = ui_handler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("message", s);
        msg.setData(b);
        ui_handler.sendMessage(msg);
    }

    private void sendReadyText(String text) {
        Message msg = ui_handler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("text", text);
        msg.setData(b);
        ui_handler.sendMessage(msg);
    }

    private void sendReadyTextToast(String text) {
        Message msg = ui_handler.obtainMessage();
        Bundle b = new Bundle();
        b.putString("toast", text);
        msg.setData(b);
        ui_handler.sendMessage(msg);
    }

    @Override
    public void run() {
        String res = "";
        String a_msg = "";
        String lastTran = "";
        boolean fTran = (out_mode == OutputMode.TOAST);

        AtlasTranslator tran = new AtlasTranslator(atl_timeout);
        boolean ok_conn = false;
        try {
            String c_msg = "";
            for (int i = 0; i < atl_retries; i++) {
                boolean ti = false;
                try {
                    ti = tran.initTran(atl_host, atl_port);
                } catch (Exception e) {
                    c_msg = e.getMessage();
                    Thread.sleep(atl_timeout * 1000);
                }
                if (!ti) continue;
                ok_conn = true;
                break;
            }
            if (!ok_conn) {
                if (c_msg.isEmpty())
                    c_msg = "ATLAS connection failure";
                throw new AtlasException(c_msg);
            }

            if (!fTran)
                sendProgressMsg(0);
            int idx = 0;
            for (String txs : atl_text) {
                String s = tran.tranString(txs);
                lastTran = s;
                res += txs + "\n" + s + "\n\n";
                idx++;
                if (!fTran)
                    sendProgressMsg(100 * idx / atl_text.size());
            }
            if (!fTran)
                sendProgressMsg(100);
            tran.doneTran();

        } catch (Exception e) {
            a_msg = e.getMessage();
            try {
                if (ok_conn) tran.doneTran();
            } catch (Exception ex) {
                Log.e("AuxTranslator", "Double exception while closing ATLAS", ex);
            }
        }
        if (!a_msg.isEmpty())
            sendAlertMsg(a_msg);
        else if (!res.isEmpty()) {
            if (fTran)
                sendReadyTextToast(lastTran);
            else
                sendReadyText(res);
        } else
            sendAlertMsg("Empty result");

        if (!fTran)
            sendProgressMsg(-1);
    }
}
