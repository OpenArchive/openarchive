package net.opendasharchive.openarchive.features.core

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityDataUsageBinding
import net.opendasharchive.openarchive.util.Constants
import net.opendasharchive.openarchive.util.extensions.routeTo
import org.witness.proofmode.crypto.PgpUtils
import java.io.IOException

class SettingsActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityDataUsageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityDataUsageBinding.inflate(layoutInflater)
        setContentView(mBinding.root)
        initLayout()
    }

    private fun initLayout() {
        setSupportActionBar(mBinding.toolbar)
        supportActionBar?.let {
            it.title = Constants.EMPTY_STRING
            it.setDisplayHomeAsUpEnabled(true)
        }
        routeTo(SettingsFragment(), mBinding.content.id)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                //NavUtils.navigateUpFromSameTask(this);
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {

        const val KEY_TYPE = "type"
        const val KEY_DATAUSE = "datause"
        const val KEY_METADATA = "metadata"
        const val KEY_NETWORKING = "networking"

        class SettingsFragment : PreferenceFragmentCompat() {
            override fun onCreatePreferences(bundle: Bundle?, s: String?) {
                // Load the Preferences from the XML file
                val type = requireActivity().intent.getStringExtra(KEY_TYPE)
                if (type.isNullOrEmpty() || type == KEY_DATAUSE) {
                    addPreferencesFromResource(R.xml.app_prefs_datause)
                } else if (type == KEY_METADATA) {
                    addPreferencesFromResource(R.xml.app_prefs_metadata)
                    val myPref = findPreference<Preference>("share_proofmode")
                    myPref?.let {
                        it.onPreferenceClickListener = Preference.OnPreferenceClickListener { //open browser or intent here
                            shareKey(requireActivity())
                            true
                        }
                    }
                } else if (type == KEY_NETWORKING) {
                    addPreferencesFromResource(R.xml.app_prefs_networking)
                }
            }
        }

        fun shareKey(activity: Activity) {
            try {
                val mPgpUtils = PgpUtils.getInstance(activity, PgpUtils.DEFAULT_PASSWORD)
                val pubKey = mPgpUtils.publicKey
                if (pubKey.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_SEND)
                    intent.type = "text/plain"
                    intent.putExtra(Intent.EXTRA_TEXT, pubKey)
                    activity.startActivity(intent)
                }
            } catch (ioe: IOException) {
                Log.e("Proofmode", "error publishing key", ioe)
            }
        }
    }

}