package net.opendasharchive.openarchive.features.onboarding

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import net.opendasharchive.openarchive.R
import net.opendasharchive.openarchive.databinding.ActivityEulaBinding
import net.opendasharchive.openarchive.util.Globals
import java.io.BufferedReader
import java.io.Closeable
import java.io.IOException
import java.io.InputStreamReader

/**
 * Created by micahjlucas on 12/15/14.
 */

/**
 * Displays an EULA ("End User License Agreement") that the user has to accept
 * before using the application. Your application should call
 * {@link EulaActivity#show(android.app.Activity)} in the onCreate() method of the first
 * activity. If the user accepts the EULA, it will never be shown again. If the
 * user refuses, {@link android.app.Activity#finish()} is invoked on your
 * activity.
 */
class EulaActivity(private val mActivity: AppCompatActivity) {

    private lateinit var mBinding: ActivityEulaBinding

    //callback to let the activity know when the user has accepted the EULA.
    internal interface OnEulaAgreedTo {
        //called when the user has accepted the eula and the dialog closes.
        fun onEulaAgreedTo()
    }

    /**
     * Displays the EULA if necessary. This method should be called from the
     * onCreate() method of your main Activity.
     *
     * @param mActivity The Activity to finish if the user rejects the EULA.
     * @return Whether the user has agreed already.
     */
    fun show(): Boolean {
        val sharedPrefs: SharedPreferences =
            mActivity.getSharedPreferences(Globals.PREF_FILE_KEY, Activity.MODE_PRIVATE)
        val noEula = !sharedPrefs.getBoolean(Globals.PREF_EULA_ACCEPTED, false)
        if (noEula) {
            val builder = AlertDialog.Builder(mActivity)
            val adbInflater = LayoutInflater.from(mActivity)
            mBinding = ActivityEulaBinding.inflate(adbInflater)
            builder.setView(mBinding.root)
            builder.setTitle(R.string.eula_title)
            builder.setCancelable(true)
            builder.setPositiveButton(R.string.eula_accept) { dialog, which ->
                accept(sharedPrefs)
                if (mActivity is OnEulaAgreedTo) {
                    (mActivity as OnEulaAgreedTo).onEulaAgreedTo()
                }
            }
            builder.setNegativeButton(R.string.eula_refuse) { dialog, which -> refuse(mActivity) }
            builder.setOnCancelListener {
                refuse(mActivity)
            }
            builder.create().show()
            return false
        }
        return true
    }

    private fun accept(preferences: SharedPreferences) {
        preferences.edit().putBoolean(Globals.PREF_EULA_ACCEPTED, true).commit()
    }

    private fun refuse(activity: Activity) {
        activity.finish()
    }

    /**
     * Return whether the EULA was accepted. Use this method in case you don't
     * wish to show a EULA dialog for the negative condition
     *
     * @param context
     * @return whether the EULA was accepted
     */
    fun isAccepted(context: Context): Boolean {
        val sharedPrefs = context.getSharedPreferences(
            Globals.PREF_FILE_KEY, Activity.MODE_PRIVATE
        )
        return sharedPrefs.getBoolean(Globals.PREF_EULA_ACCEPTED, false)
    }

    private fun readEula(): CharSequence? {
        var `in`: BufferedReader? = null
        return try {
            `in` = BufferedReader(InputStreamReader(mActivity.assets.open(Globals.ASSET_EULA)))
            var line: String?
            val buffer = StringBuilder()
            while (`in`.readLine().also { line = it } != null) {
                buffer.append(line).append('\n')
            }
            buffer
        } catch (e: IOException) {
            ""
        } finally {
            closeStream(`in`)
        }
    }

    /**
     * Closes the specified stream.
     *
     * @param stream The stream to close.
     */
    private fun closeStream(stream: Closeable?) {
        if (stream != null) {
            try {
                stream.close()
            } catch (e: IOException) {
                //ignore
            }
        }
    }
}