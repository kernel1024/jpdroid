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
import android.text.Editable;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends Activity {

    private EditText mainView;
    private ProgressBar mainProgress;
    private SharedPreferences prefs;
    private Object mActionMode;
    private AuxTranslator mainTranslator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainTranslator = null;
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferencesopts, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        ActionBar b = getActionBar();
        if (b != null)
            b.setTitle("");

        mainView = (EditText) findViewById(R.id.mainTextView);
        mainProgress = (ProgressBar) findViewById(R.id.mainProgress);

        mainView.setCustomSelectionActionModeCallback(mActionModeCallback);

        mainView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (mActionMode == null)
                    return false;
                mActionMode = MainActivity.this.startActionMode(mActionModeCallback);
                v.setSelected(true);
                return true;
            }
        });

        checkIntent();
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            if (inflater != null)
                inflater.inflate(R.menu.context, menu);
            MenuItem m = menu.findItem(android.R.id.cut);
            if (m != null) m.setVisible(false);
            m = menu.findItem(android.R.id.paste);
            if (m != null) m.setVisible(false);
            return true;
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Editable e = mainView.getText();
            if (e == null || !mainView.hasSelection()) return false;
            String s = e.toString();
            if (s.isEmpty()) return false;
            s = s.substring(mainView.getSelectionStart(), mainView.getSelectionEnd());
            if (s.isEmpty()) return false;

            switch (item.getItemId()) {
                case R.id.action_sel_search:
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, s);
                    startActivity(Intent.createChooser(intent, "Search for selection"));
                    mode.finish();
                    return true;
                case R.id.action_sel_translate:
                    faTranslate(s, true);
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        public void onDestroyActionMode(ActionMode mode) {
            mActionMode = null;
        }
    };

    private void checkIntent() {
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
                    if (file.length() > 10 * 1024 * 1024)
                        throw new AtlasException("File too big (over 10Mb).");

                    in = new BufferedReader(new InputStreamReader(
                            new FileInputStream(file), "UTF-8"));

                    String buf;
                    while ((buf = in.readLine()) != null)
                        tx += buf + "\n";
                } catch (Exception e) {
                    showToast("File error: " + e.getMessage());
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException ex) {
                            Log.e("JPDroid", "Double exception while closing intent file.", ex);
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
                CharSequence cs = mainView.getText();
                if (cs == null || cs.length() == 0)
                    faClear();
                else
                    faTranslate(cs.toString(), false);
                return true;
            case R.id.action_clear:
                faClear();
                return true;
            case R.id.action_settings:
                Intent is = new Intent(MainActivity.this, SettingsActivity.class);
                startActivity(is);
                return true;
            case R.id.action_stop:
                if (mainTranslator!=null)
                    mainTranslator.breakTranslation();
                return true;
            case R.id.action_exit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showToast(int msg) {
        Context ctx = getApplicationContext();
        if (ctx == null) return;
        Toast t = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT);
        t.show();
    }

    public void showToast(String msg) {
        Log.d("JPDroid", msg);
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
                (
                        (!cd.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) &&
                        (!cd.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML))) ||
                (cb.getPrimaryClip() == null)) {
            showToast(R.string.msg_clipboard_incompat);
            return;
        }

        ClipData cdt = cb.getPrimaryClip();
        ClipData.Item ci = cdt.getItemAt(0);
        CharSequence s = ci.coerceToText(this);

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
            if (b.containsKey("toast")) {
                Context ctx = getApplicationContext();
                if (ctx == null) return;
                Toast t = Toast.makeText(ctx, b.getString("toast"), Toast.LENGTH_LONG);
                t.show();
            }
            if (b.containsKey("cleanup"))
                mainTranslator = null;

        }
    };

    private void faTranslate(String s, boolean toastMode) {
        if (s == null || s.isEmpty()) return;
        if (mainTranslator!=null) {
            showToast("Atlas translator is busy.");
            return;
        }
        String atl_host, atl_token, atl_cert;
        int atl_port, atl_timeout, atl_retry;
        try {
            atl_host = prefs.getString("atlas_host", "localhost");
            atl_port = Integer.valueOf(prefs.getString("atlas_port", "18000"));
            atl_retry = Integer.valueOf(prefs.getString("atlas_retry", "3"));
            atl_timeout = Integer.valueOf(prefs.getString("atlas_timeout", "5"));
            atl_token = prefs.getString("atlas_token", "");

            CertificateLoader cLoader = new CertificateLoader(this);
            atl_cert = cLoader.getCertificate();
        } catch (Exception e) {
            showToast(e.getMessage());
            return;
        }

        ArrayList<String> tx = new ArrayList<String>(Arrays.asList(s.split("\\r?\\n")));

        if (toastMode) {
            AuxTranslator tran;
            tran = new AuxTranslator(atl_host, atl_port,
                    atl_timeout, atl_retry, atl_token, atl_cert, tx, atl_handler, AuxTranslator.OutputMode.TOAST);
            Thread t = new Thread(tran);
            t.start();
        } else {
            mainTranslator = new AuxTranslator(atl_host, atl_port,
                    atl_timeout, atl_retry, atl_token, atl_cert, tx, atl_handler, AuxTranslator.OutputMode.TEXT);
            Thread t = new Thread(mainTranslator);
            t.start();
        }
    }

}
