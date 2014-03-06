import com.tscorp.jpdroid.app.AtlasException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class AuxTranslator implements Runnable {

    private int atl_timeout;
    private int atl_retries;
    private int atl_port;
    private String atl_host;
    private String atl_text;

    AuxTranslator(String hostname, int port, int timeout, int retries, String text) {
        atl_host = hostname;
        atl_port = port;
        atl_timeout = timeout;
        atl_retries = retries;
        atl_text = text;
    }

    private void sendAlertMsg(String msg) {

    }

    private void sendReadyText(List<String> text) {

    }

    // thread examples - http://arashmd.blogspot.de/2013/06/java-threading.html

    @Override
    public void run() {
        List<String> tx = Arrays.asList(atl_text.split("\\r?\\n"));
        List<String> xt = Arrays.asList();

        AtlasTranslator tran = new AtlasTranslator(atl_timeout);
        try {
            boolean ok_tran = false;
            for (int i=0;i<atl_retries;i++) {
                if (!tran.initTran(atl_host,atl_port)) continue;
                for (String txs : tx) {
                    xt.add(tran.tranString(txs));
                    ok_tran = true;
                }
            }
            if (!ok_tran) {
                xt.clear();
                sendAlertMsg("ATLAS connection failure");
            } else if (!xt.isEmpty()) {
                sendReadyText(xt);
            }
        } catch (IOException e) {
            xt.clear(); sendAlertMsg(e.getMessage());
        } catch (AtlasException e) {
            xt.clear(); sendAlertMsg(e.getMessage());
        }

    }

}
