package net.opendasharchive.openarchive.core;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import net.opendasharchive.openarchive.R;

import org.witness.proofmode.crypto.PgpUtils;

import java.io.IOException;


public class SettingsActivity extends AppCompatActivity {

    SettingsFragment fragment;

    public final static String KEY_TYPE = "type";
    public final static String KEY_DATAUSE = "datause";
    public final static String KEY_METADATA = "metadata";
    public final static String KEY_NETWORKING = "networking";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_usage);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        FragmentManager fm = getSupportFragmentManager();
        fragment = (SettingsFragment)fm.findFragmentByTag("ftag");
        if (fragment == null) {
            FragmentTransaction ft = fm.beginTransaction();
            fragment = new SettingsFragment();
            ft.add(R.id.content, fragment, "ftag");
            ft.commit();
        }

    }



    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            // Load the Preferences from the XML file

            String type = getActivity().getIntent().getStringExtra(KEY_TYPE);

            if (TextUtils.isEmpty(type) || type.equals(KEY_DATAUSE))
                addPreferencesFromResource(R.xml.app_prefs_datause);
            else if (type.equals(KEY_METADATA)) {
                addPreferencesFromResource(R.xml.app_prefs_metadata);

                Preference myPref = findPreference("share_proofmode");
                myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(Preference preference) {
                        //open browser or intent here
                        shareKey(getActivity());
                        return true;
                    }
                });

            }
            else if (type.equals(KEY_NETWORKING))
                addPreferencesFromResource(R.xml.app_prefs_networking);

        }


    }



    public static void shareKey (Activity activity)
    {

        try {

            PgpUtils mPgpUtils = PgpUtils.getInstance(activity,PgpUtils.DEFAULT_PASSWORD);
            String pubKey = mPgpUtils.getPublicKey();
            if (!TextUtils.isEmpty(pubKey)) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_TEXT, pubKey);
                activity.startActivity(intent);
            }
        }
        catch (IOException ioe)
        {
            Log.e("Proofmode","error publishing key",ioe);
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
