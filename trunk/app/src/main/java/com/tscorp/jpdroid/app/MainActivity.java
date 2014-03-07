package com.tscorp.jpdroid.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends Activity {

    private TextView mainView;
    private ProgressBar mainProgress;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferencesopts, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        ActionBar b = getActionBar();
        if (b != null)
            b.setTitle("");

        mainView = (TextView) findViewById(R.id.mainTextView);
        mainProgress = (ProgressBar) findViewById(R.id.mainProgress);

        Intent intent = getIntent();
        String action = intent.getAction();
        String mime = intent.getType();
        String scheme = intent.getScheme();
        if (Intent.ACTION_VIEW.equals(action) && (mime != null) && (scheme != null) &&
                (mime.startsWith("text/")) && (scheme.startsWith("file"))) {
            Uri f_name = intent.getData();
            if (f_name != null) {
                String tx = "";
                File file = new File(f_name.getPath());
                BufferedReader in = null;
                try {
                    if (file.length()>10*1024*1024)
                        throw new AtlasException("File too big (over 10Mb).");

                    in = new BufferedReader(new InputStreamReader(
                            new FileInputStream(file),"UTF-8"));

                    String buf;
                    while ((buf = in.readLine())!=null)
                        tx += buf + "\r\n";
                } catch (Exception e) {
                    showToast("File error: "+e.getMessage());
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException ex){
                            Log.e("MainActivity","Double exception while closing intent file.",ex);
                        }
                    }
                }
                if (!tx.isEmpty())
                    mainView.setText(tx);
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_paste:
                faPaste();
                return true;
            case R.id.action_translate:
                faTranslate();
                return true;
            case R.id.action_clear:
                faClear();
                return true;
            case R.id.action_settings:
                Intent is = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(is);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showToast(int msg) {
        Context ctx = getApplicationContext();
        if (ctx == null) return;
        Toast t = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT);
        t.show();
    }

    private void showToast(String msg) {
        Context ctx = getApplicationContext();
        if (ctx == null) return;
        Toast t = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT);
        t.show();
    }

    private void faClear() {
        mainView.setText("");
    }

    private void faPaste() {
        ClipboardManager cb = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (!cb.hasPrimaryClip()) {
            showToast(R.string.msg_clipboard_empty);
            return;
        }
        ClipDescription cd = cb.getPrimaryClipDescription();
        if ((cd == null) ||
                (!cd.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) ||
                (cb.getPrimaryClip() == null)) {
            showToast(R.string.msg_clipboard_incompat);
            return;
        }

        ClipData cdt = cb.getPrimaryClip();
        ClipData.Item ci = cdt.getItemAt(0);
        CharSequence s = ci.getText();

        mainView.setText(s);
    }

    public Handler atl_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle b = msg.getData();
            if (b == null) return;
            if (b.containsKey("progress")) {
                int x = b.getInt("progress");
                if (x >= 0) {
                    if (mainProgress.getVisibility() != View.VISIBLE)
                        mainProgress.setVisibility(View.VISIBLE);
                    mainProgress.setProgress(x);
                } else
                    mainProgress.setVisibility(View.GONE);
            }
            if (b.containsKey("text")) {
                String tx = b.getString("text");
                if (tx != null && !tx.isEmpty())
                    mainView.setText(tx);
            }
            if (b.containsKey("message")) {
                showToast(b.getString("message"));
            }

        }
    };

    private void faTranslate() {
        String atl_host;
        int atl_port, atl_timeout, atl_retry;
        try {
            atl_host = prefs.getString("atlas_host", "localhost");
            atl_port = Integer.valueOf(prefs.getString("atlas_port", "18000"));
            atl_retry = Integer.valueOf(prefs.getString("atlas_retry", "3"));
            atl_timeout = Integer.valueOf(prefs.getString("atlas_timeout", "5"));
        } catch (Exception e) {
            showToast(e.getMessage());
            return;
        }

        CharSequence cs = mainView.getText();
        if (cs == null || cs.length() == 0) {
            faClear();
            return;
        }
        String s = cs.toString();

        ArrayList<String> tx = new ArrayList<String>(Arrays.asList(s.split("\\r?\\n")));

        AuxTranslator tran = new AuxTranslator(atl_host, atl_port,
                atl_timeout, atl_retry, tx, atl_handler);
        Thread t = new Thread(tran);
        t.start();
    }

}
