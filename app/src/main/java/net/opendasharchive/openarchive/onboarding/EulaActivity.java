package net.opendasharchive.openarchive.onboarding;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.util.Globals;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;

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
class EulaActivity {
    //callback to let the activity know when the user has accepted the EULA.
    static interface OnEulaAgreedTo {
        //called when the user has accepted the eula and the dialog closes.
        void onEulaAgreedTo();
    }

    Activity mActivity;

    EulaActivity(final Activity activity) {
        mActivity = activity;
    }

    /**
     * Displays the EULA if necessary. This method should be called from the
     * onCreate() method of your main Activity.
     *
     * @param mActivity The Activity to finish if the user rejects the EULA.
     * @return Whether the user has agreed already.
     */
    boolean show() {
        final SharedPreferences sharedPrefs = mActivity.getSharedPreferences(Globals.PREF_FILE_KEY, Activity.MODE_PRIVATE);
        // boolean noOptIn =
        // !prefsAnalytics.contains(Globals.PREFERENCE_ANALYTICS_OPTIN);
        boolean noEula = !sharedPrefs.getBoolean(Globals.PREF_EULA_ACCEPTED, false);

        // if (noEula || noOptIn) {
        if (noEula) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
            LayoutInflater adbInflater = LayoutInflater.from(mActivity);
            View view = adbInflater.inflate(R.layout.activity_eula, null);
            // cb = (CheckBox) view.findViewById(R.id.checkbox);
            builder.setView(view);
            builder.setTitle(R.string.eula_title);
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.eula_accept, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    accept(sharedPrefs);
                    if (mActivity instanceof OnEulaAgreedTo) {
                        ((OnEulaAgreedTo) mActivity).onEulaAgreedTo();
                    }
                }
            });
            builder.setNegativeButton(R.string.eula_refuse, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    refuse(mActivity);
                }
            });
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                public void onCancel(DialogInterface dialog) {
                    refuse(mActivity);
                }
            });
            // builder.setMessage(readEula(activity));
            builder.create().show();
            return false;
        }
        return true;
    }

    /**
     * Return whether the EULA was accepted. Use this method in case you don't
     * wish to show a EULA dialog for the negative condition
     *
     * @param context
     * @return whether the EULA was accepted
     */
    public static boolean isAccepted(Context context) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                Globals.PREF_FILE_KEY, Activity.MODE_PRIVATE);
        return sharedPrefs.getBoolean(Globals.PREF_EULA_ACCEPTED, false);
    }

    private void accept(SharedPreferences preferences) {
        preferences.edit().putBoolean(Globals.PREF_EULA_ACCEPTED, true).commit();
    }

    private static void refuse(Activity activity) {
        activity.finish();
    }

    private CharSequence readEula() {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(mActivity.getAssets().open(Globals.ASSET_EULA)));
            String line;
            StringBuilder buffer = new StringBuilder();
            while ((line = in.readLine()) != null) {
                buffer.append(line).append('\n');
            }
            return buffer;
        } catch (IOException e) {
            return "";
        } finally {
            closeStream(in);
        }
    }

    /**
     * Closes the specified stream.
     *
     * @param stream The stream to close.
     */
    private void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                //ignore
            }
        }
    }
}

