package com.tscorp.jpdroid.app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;


// Object not locked by thread before wait()......

public class AtlasTranslator {
    enum ATTranslateMode {
        AutoTran,
        EngToJpnTran,
        JpnToEngTran
    }

    private Socket sock;
    private String atlHost;
    private int atlPort;
    private final int atlTimeout;
    private ATTranslateMode tranMode;

    AtlasTranslator(int timeout) {
        atlHost = "localhost";
        atlPort = 18000;
        tranMode = ATTranslateMode.AutoTran;
        atlTimeout = timeout;
        sock = null;
    }

    boolean initTran(String host, int port)
            throws IOException, AtlasException {
        return initTran(host, port, ATTranslateMode.AutoTran);
    }

    boolean initTran(String host, int port, ATTranslateMode TranMode)
            throws IOException, AtlasException {
        if ((sock != null) && sock.isConnected()) return true;
        atlHost = host;
        atlPort = port;
        tranMode = TranMode;

        sock = new Socket();
        sock.connect(new InetSocketAddress(atlHost, atlPort), atlTimeout * 1000);

        PrintWriter s_out = new PrintWriter(sock.getOutputStream());
        BufferedReader s_in = new BufferedReader(new InputStreamReader(sock.getInputStream(), "ISO-8859-1"));
        // INIT command and response
        s_out.write("INIT\r\n");
        s_out.flush();
        String buf = s_in.readLine();
        if (buf == null || buf.trim().isEmpty() || !buf.trim().equals("OK")) {
            sock.close();
            throw new AtlasException("ATLAS: initialization error");
        }

        // DIR command and response
        buf = "DIR:AUTO\r\n";
        if (tranMode == ATTranslateMode.EngToJpnTran)
            buf = "DIR:EJ\r\n";
        if (tranMode == ATTranslateMode.JpnToEngTran)
            buf = "DIR:JE\r\n";
        s_out.write(buf);
        s_out.flush();
        buf = s_in.readLine();
        if (buf == null || buf.trim().isEmpty() || !buf.trim().equals("OK")) {
            sock.close();
            throw new AtlasException("ATLAS: direction error");
        }
        return true;
    }

    String tranString(String src)
            throws IOException, AtlasException {
        if (!sock.isConnected()) return "ERROR: Socket not opened";

        PrintWriter s_out = new PrintWriter(sock.getOutputStream());
        BufferedReader s_in = new BufferedReader(new InputStreamReader(sock.getInputStream(), "ISO-8859-1"));

        // TR command and response
        String buf = URLEncoder.encode(src, "UTF-8").trim();
        if ((buf == null) || buf.trim().isEmpty()) return "";
        buf = "TR:" + buf + "\r\n";
        s_out.write(buf);
        s_out.flush();

        buf = s_in.readLine();
        if (buf == null) {
            throw new AtlasException("ATLAS: null response");
        }
        buf = buf.trim().replace('+', ' ');
        if (buf.isEmpty() || !buf.startsWith("RES:")) {
            if (buf.contains("NEED_RESTART")) {
                sock.close();
                throw new AtlasException("ATLAS: translation engine slipped. Please restart again.");
            } else {
                sock.close();
                throw new AtlasException("ATLAS: translation error");
            }
        }

        if (buf.startsWith("RES:"))
            buf = buf.replaceFirst("^RES:", "");

        return URLDecoder.decode(buf, "UTF-8");
    }

    void doneTran()
            throws IOException, AtlasException {
        doneTran(false);
    }

    void doneTran(boolean lazyClose)
            throws IOException, AtlasException {
        if (!sock.isConnected()) return;

        if (!lazyClose) {
            // FIN command and response
            PrintWriter s_out = new PrintWriter(sock.getOutputStream());
            BufferedReader s_in = new BufferedReader(new InputStreamReader(sock.getInputStream(), "ISO-8859-1"));

            s_out.write("FIN\r\n");
            s_out.flush();
            String buf = s_in.readLine();
            if (buf == null || buf.trim().isEmpty() || !buf.trim().equals("OK")) {
                sock.close();
                throw new AtlasException("ATLAS: finalization error");
            }
        }
        sock.close();
    }
}
