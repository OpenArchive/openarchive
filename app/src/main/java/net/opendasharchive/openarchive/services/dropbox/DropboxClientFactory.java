package net.opendasharchive.openarchive.services.dropbox;

import android.content.Context;
import android.util.Log;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.http.OkHttp3Requestor;
import com.dropbox.core.v2.DbxClientV2;

import net.opendasharchive.openarchive.util.Prefs;

import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import okhttp3.OkHttpClient;

/**
 * Singleton instance of {@link DbxClientV2} and friends
 */
public class DropboxClientFactory {

    private DbxClientV2 sDbxClient;

    public DbxClientV2 init(Context context, String accessToken) {
        if (sDbxClient == null) {
            DbxRequestConfig requestConfig = DbxRequestConfig.newBuilder("dbc")
                    .withHttpRequestor(new OkHttp3Requestor(getOkClient(context)))
                    .build();

            sDbxClient = new DbxClientV2(requestConfig, accessToken);
        }

        return sDbxClient;
    }

    public DbxClientV2 getClient() {
        if (sDbxClient == null) {
            throw new IllegalStateException("Client not initialized.");
        }
        return sDbxClient;
    }

    private OkHttpClient client;

    private OkHttpClient getOkClient (Context context)
    {
        client = new OkHttpClient.Builder().build();

        if (Prefs.getUseTor() && OrbotHelper.isOrbotInstalled(context)) {

            try {

                OrbotHelper.requestStartTor(context);

                StrongOkHttpClientBuilder builder = new StrongOkHttpClientBuilder(context);
                builder.withBestProxy().build(new StrongBuilder.Callback<OkHttpClient>() {
                    @Override
                    public void onConnected(OkHttpClient okHttpClient) {

                        Log.i("NetCipherClient", "Connection to orbot established!");
                        client = okHttpClient;
                      }


                    @Override
                    public void onConnectionException(Exception exc) {
                        Log.e("NetCipherClient", "onConnectionException()", exc);

                    }

                    @Override
                    public void onTimeout() {
                        Log.e("NetCipherClient", "onTimeout()");

                    }

                    @Override
                    public void onInvalid() {
                        Log.e("NetCipherClient", "onInvalid()");

                    }
                });


            } catch (Exception exc) {
                Log.e("Error", "Error while initializing TOR Proxy OkHttpClient", exc);

            }


        }

        return client;
    }
}