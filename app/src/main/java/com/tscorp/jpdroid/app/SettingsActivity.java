package com.tscorp.jpdroid.app;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

import java.util.List;

public class SettingsActivity extends PreferenceActivity {

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.preference_headers, target);
    }

    @Override
    public void onHeaderClick(Header header, int position) {
        super.onHeaderClick(header, position);

        if (header.id == R.id.enter_cert) {
            Intent is = new Intent(SettingsActivity.this, CertificateActivity.class);
            startActivity(is);
        }
    }

    public static class PrefsFragmentHost extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
        }
    }

    public static class PrefsFragmentIPOptions extends PreferenceFragment {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferencesopts);
        }
    }
}
