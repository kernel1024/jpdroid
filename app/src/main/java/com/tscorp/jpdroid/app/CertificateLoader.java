package com.tscorp.jpdroid.app;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;

public class CertificateLoader {

    private String certificate;
    private Context m_ctx;

    CertificateLoader(Context ctx) {
        FileInputStream in;
        m_ctx = ctx;

        try {

            in = m_ctx.openFileInput("server.crt");
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String s;
            StringBuilder sb = new StringBuilder();
            while ((s = br.readLine()) != null) {
                sb.append(s);
                sb.append("\n");
            }
            br.close();

            certificate = sb.toString();

        } catch (Exception e) {
            certificate = "ERROR: Unable to load certificate.";
            e.printStackTrace();
            Log.e("JPDroid", "Unable to load certificate: " + e.getMessage());
        }

    }

    public String getCertificate() {
        return certificate;
    }

    public void saveCertificate(String cert) {
        FileOutputStream fs;
        certificate = cert;
        if (certificate.contains("ERROR"))
            certificate = "";

        try {
            fs = m_ctx.openFileOutput("server.crt", Context.MODE_PRIVATE);
            fs.write(cert.getBytes());
            fs.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("JPDroid", "Unable to save certificate: " + e.getMessage());
        }
    }
}
