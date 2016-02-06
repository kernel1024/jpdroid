package com.tscorp.jpdroid.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class CertificateActivity extends Activity {

    private CertificateLoader cLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_certificate);

        cLoader = new CertificateLoader(this);

        String certificate = cLoader.getCertificate();

        final EditText certEdit = (EditText) findViewById(R.id.editCert);
        certEdit.setText(certificate);

        Button saveBtn = (Button) findViewById(R.id.buttonSaveCert);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cLoader.saveCertificate(certEdit.getText().toString());
                finish();
            }
        });

        Button clearBtn = (Button) findViewById(R.id.buttonClearCert);
        clearBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                certEdit.setText("");
            }
        });
    }
}
