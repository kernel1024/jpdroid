package com.tscorp.jpdroid.app;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private TextView mainView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PreferenceManager.setDefaultValues(this,R.xml.preferences,false);
        PreferenceManager.setDefaultValues(this,R.xml.preferencesopts,false);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        ActionBar b = getActionBar();
        b.setTitle("");

        mainView = (TextView)findViewById(R.id.mainTextView);
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
                Intent is = new Intent(MainActivity.this,SettingsActivity.class);
                startActivity(is);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showToast(int msg) {
        Context ctx = getApplicationContext();
        if (ctx==null) return;
        Toast t = Toast.makeText(ctx,msg,Toast.LENGTH_SHORT);
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
        if ((cd==null) ||
                (!cd.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)) ||
                (cb.getPrimaryClip()==null)) {
            showToast(R.string.msg_clipboard_incompat);
            return;
        }

        ClipData cdt = cb.getPrimaryClip();
        ClipData.Item ci = cdt.getItemAt(0);
        CharSequence s = ci.getText();

        mainView.setText(s);
    }

    private void faTranslate() {
        String atl_host = prefs.getString("atlas_host","localhost");
        int atl_port = Integer.valueOf(prefs.getString("atlas_port","18000"));
        int atl_retry = Integer.valueOf(prefs.getString("atlas_retry","3"));
        int atl_timeout = Integer.valueOf(prefs.getString("atlas_timeout","5"));

        String out = atl_host + ":" + Integer.toString(atl_port) + " - "
                + Integer.toString(atl_retry) + " - " + Integer.toString(atl_timeout);

        mainView.setText(out);

    }

}
