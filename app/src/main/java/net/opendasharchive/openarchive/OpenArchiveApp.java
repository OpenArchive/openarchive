package net.opendasharchive.openarchive;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.multidex.MultiDex;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.decoder.SimpleProgressiveJpegConfig;

import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.publish.PublishService;
import net.opendasharchive.openarchive.util.Prefs;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.annotation.AcraCore;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;

import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import io.cleaninsights.sdk.CleanInsights;
import io.cleaninsights.sdk.piwik.CleanInsightsApplication;
import okhttp3.OkHttpClient;

/**
 * Created by josh on 3/6/15.
 */
@AcraCore(buildConfigClass = BuildConfig.class)
public class OpenArchiveApp extends com.orm.SugarApp {

    public static volatile boolean orbotConnected = false;

    private CleanInsightsApplication cleanInsightsApp;

    private Space mCurrentSpace = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }


    @Override
    public void onCreate() {
        super.onCreate();

        Prefs.setContext(this);

        ImagePipelineConfig config = ImagePipelineConfig.newBuilder(this)
                .setProgressiveJpegConfig(new SimpleProgressiveJpegConfig())
                .setResizeAndRotateEnabledForNetwork(true)
                .setDownsampleEnabled(true)
                .build();

        Fresco.initialize(this, config);


        //disable proofmode GPS dat tracking by default
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putBoolean("trackLocation",false);

        if (Prefs.getUseTor() && OrbotHelper.isOrbotInstalled(this))
            initNetCipher(this);

        initCrashReporting();


        uploadQueue();
    }

    private void initCrashReporting ()
    {

        CoreConfigurationBuilder builder = new CoreConfigurationBuilder(this)
                .setBuildConfigClass(BuildConfig.class)
                .setReportFormat(StringFormat.KEY_VALUE_LIST)
                .setReportContent(
                        //ReportField.USER_COMMENT,
                        ReportField.REPORT_ID,
                        ReportField.APP_VERSION_NAME,
                        ReportField.APP_VERSION_CODE,
                        ReportField.ANDROID_VERSION,
                        ReportField.PHONE_MODEL,
                        ReportField.PACKAGE_NAME,
                        ReportField.CRASH_CONFIGURATION,
                        ReportField.CUSTOM_DATA,
                        ReportField.STACK_TRACE,
                        ReportField.APPLICATION_LOG,
                        ReportField.BUILD);
        /**
        builder.getPluginConfigurationBuilder(ToastConfigurationBuilder.class)
                .setEnabled(true)
                .setResText(R.string.crash_dialog_text);


// email config

         builder.getPluginConfigurationBuilder(HttpSenderConfigurationBuilder.class)
         .setUri(getResources().getString(R.string.error_end_point))
         .setHttpMethod(HttpSender.Method.POST)
         .setEnabled(true);

// dialog config
        builder.getPluginConfigurationBuilder(DialogConfigurationBuilder.class)
                .setResText(R.string.crash_dialog_text)
                .setResIcon(android.R.drawable.ic_dialog_info)
                .setTitle(getResources().getString(R.string.crash_dialog_title))
                .setResCommentPrompt(R.string.crash_dialog_comment_prompt)
                .setEnabled(true);
            **/

// Mail config

        builder.getPluginConfigurationBuilder(MailSenderConfigurationBuilder.class)
                .setEnabled(true)
                .setMailTo(getResources().getString(R.string.crashreportemail))
                .setReportAsFile(true);

        // The following line triggers the initialization of ACRA
        ACRA.init(this, builder);
    }

    public void uploadQueue ()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(this, PublishService.class));
        }
        else
        {
            startService(new Intent(this, PublishService.class));
        }
    }

    public CleanInsightsApplication getCleanInsightsApp ()
    {
        return cleanInsightsApp;
    }

    private void initInsights ()
    {
        CleanInsights cim = CleanInsights.getInstance(this);

        //setup a passthrough application for CleanInsights, since we are already a SugarApp
        cleanInsightsApp = new CleanInsightsApplication() {

            @Override
            public Context getApplicationContext() {
                return OpenArchiveApp.this.getApplicationContext();
            }

            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return OpenArchiveApp.this.getSharedPreferences(name, mode);
            }

            @Override
            public String getMeasureUrl() {
                return "https://demo.cleaninsights.io";
            }

            @Override
            public String getMeasureUrlCertificatePin()
            {
                //generate your own using this tool: https://github.com/scottyab/ssl-pin-generator
                return "sha256/ZG+5y3w2mxstotmn15d9tnJtwss591+L6EH/yJMF41I=";
            }

            @Override
            public Integer getSiteId() {
                return 1;
            }
        };

        cim.initPwiki(cleanInsightsApp);
    }

    public boolean getUseTor ()
    {
        return orbotConnected;
    }

    public static void initNetCipher(Context context) {
        final String LOG_TAG = "NetCipherClient";
        Log.i(LOG_TAG, "Initializing NetCipher client");

        Context appContext = context.getApplicationContext();

        OrbotHelper oh = OrbotHelper.get(appContext);

        if (BuildConfig.DEBUG) {
            oh.skipOrbotValidation();
        }

        if (oh.init()) {
            orbotConnected = true;
        }

        try {

            StrongOkHttpClientBuilder.forMaxSecurity(appContext).build(new StrongBuilder.Callback<OkHttpClient>() {
                @Override
                public void onConnected(OkHttpClient okHttpClient) {

                    orbotConnected = true;
                    Log.i("NetCipherClient", "Connection to orbot established!");
                    // from now on, you can create upload requests
                    // as usual, and they will be proxied through TOR.
                    // Bear in mind that
                }

                @Override
                public void onConnectionException(Exception exc) {
                    orbotConnected = false;
                    Log.e("NetCipherClient", "onConnectionException()", exc);
                }

                @Override
                public void onTimeout() {
                    orbotConnected = false;
                    Log.e("NetCipherClient", "onTimeout()");
                }

                @Override
                public void onInvalid() {
                    orbotConnected = false;
                    Log.e("NetCipherClient", "onInvalid()");
                }
            });
        } catch (Exception exc) {
            Log.e("Error", "Error while initializing TOR Proxy OkHttpClient", exc);
        }
    }


    public synchronized Space getCurrentSpace ()
    {
        if (mCurrentSpace == null) {

            long spaceId = Prefs.getCurrentSpaceId();
            if (spaceId != -1L) {
                mCurrentSpace = Space.findById(Space.class,spaceId);
            }
        }

        return mCurrentSpace;
    }
}
