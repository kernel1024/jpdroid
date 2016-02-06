package com.tscorp.jpdroid.app;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;


public class AtlasTranslator {
    enum ATTranslateMode {
        AutoTran,
        EngToJpnTran,
        JpnToEngTran
    }

    private SSLSocket sock;
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

    boolean isConnected() {
        return sock != null && sock.isConnected();
    }

    boolean initTran(String host, int port, String token, String cert)
            throws IOException, AtlasException, CertificateException, KeyStoreException,
            NoSuchAlgorithmException, KeyManagementException {
        return initTran(host, port, ATTranslateMode.AutoTran, token, cert);
    }

    SSLSocketFactory createSSLFactory(String cert)
            throws CertificateException, KeyStoreException, NoSuchAlgorithmException,
            KeyManagementException, IOException {
        if (cert.isEmpty() || cert.contains("ERROR"))
            return (SSLSocketFactory) SSLSocketFactory.getDefault();

        SSLSocketFactory sf = null;
        InputStream caInput = new ByteArrayInputStream(cert.getBytes());

        // Load CAs from an InputStream
        // (could be from a resource or ByteArrayInputStream or ...
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        // From https://www.washington.edu/itconnect/security/ca/load-der.crt
        Certificate ca = cf.generateCertificate(caInput);

        // Create a KeyStore containing our trusted CAs
        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        // Create a TrustManager that trusts the CAs in our KeyStore
        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        // Create an SSLContext that uses our TrustManager
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);

        caInput.close();
        return context.getSocketFactory();
    }

    boolean initTran(String host, int port, ATTranslateMode TranMode, String token, String cert)
            throws IOException, AtlasException, CertificateException, KeyStoreException,
            NoSuchAlgorithmException, KeyManagementException {
        if ((sock != null) && sock.isConnected()) return true;
        atlHost = host;
        atlPort = port;
        tranMode = TranMode;

        SSLSocketFactory sf = createSSLFactory(cert);
        sock = (SSLSocket) sf.createSocket();
        sock.connect(new InetSocketAddress(atlHost, atlPort), atlTimeout * 1000);

        PrintWriter s_out = new PrintWriter(sock.getOutputStream());
        BufferedReader s_in = new BufferedReader(new InputStreamReader(sock.getInputStream(), "ISO-8859-1"));
        // INIT command and response
        s_out.write("INIT:" + token + "\r\n");
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
        if (buf.trim().isEmpty()) return "";
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
                if (!sock.isConnected())
                    sock.close();
                throw new AtlasException("ATLAS: finalization error");
            }
        }
        if (!sock.isConnected())
            sock.close();
    }
}
