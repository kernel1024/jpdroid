import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import java.util.ArrayList;
import java.util.Arrays;

public class AuxTranslator implements Runnable {

    private int atl_timeout;
    private int atl_retries;
    private int atl_port;
    private String atl_host;
    private String atl_text;
    private Handler ui_handler;

    AuxTranslator(String hostname, int port, int timeout, int retries, String text, Handler uiHandler) {
        atl_host = hostname;
        atl_port = port;
        atl_timeout = timeout;
        atl_retries = retries;
        atl_text = text;
        ui_handler = uiHandler;
    }

    private void sendProgressMsg(int progress) {
        Message msg = ui_handler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt("progress",progress);
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

    private void sendReadyText(ArrayList<String> text) {
        Message msg = ui_handler.obtainMessage();
        Bundle b = new Bundle();
        b.putStringArrayList("text", text);
        msg.setData(b);
        ui_handler.sendMessage(msg);
    }

    @Override
    public void run() {
        ArrayList<String> tx = new ArrayList<String>(Arrays.asList(atl_text.split("\\r?\\n")));
        ArrayList<String> xt = new ArrayList<String>();

        AtlasTranslator tran = new AtlasTranslator(atl_timeout);
        try {
            boolean ok_tran = false;
            for (int i=0;i<atl_retries;i++) {
                if (!tran.initTran(atl_host,atl_port)) continue;
                sendProgressMsg(0);
                for (String txs : tx) {
                    xt.add(tran.tranString(txs));
                    ok_tran = true;
                    sendProgressMsg(100*tx.indexOf(txs)/txs.length());
                }
                sendProgressMsg(100);
            }
            if (!ok_tran) {
                sendAlertMsg("ATLAS connection failure");
            } else if (!xt.isEmpty()) {
                sendReadyText(xt);
            }
        } catch (Exception e) {
            sendAlertMsg(e.getMessage());
        } finally {
            xt.clear();
            sendProgressMsg(-1);
        }

    }

}
