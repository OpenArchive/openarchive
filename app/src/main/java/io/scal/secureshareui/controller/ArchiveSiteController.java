package io.scal.secureshareui.controller;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;

import com.thegrizzlylabs.sardineandroid.impl.handler.ResponseHandler;

import net.opendasharchive.openarchive.R;
import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.Prefs;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;

import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import io.scal.secureshareui.lib.Util;
import io.scal.secureshareui.login.ArchiveLoginActivity;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ArchiveSiteController extends SiteController {

	public static final String SITE_NAME = "Internet Archive";
	public static final String SITE_KEY = "archive";

	public static final String THUMBNAIL_PATH = "__ia_thumb.jpg";

	private static final String TAG = "ArchiveSiteController";
    public final static String ARCHIVE_BASE_URL = "https://archive.org/";

    private boolean mContinueUpload = true;

    static {
        METADATA_REQUEST_CODE = 1022783271;
    }

	private static final String ARCHIVE_API_ENDPOINT = "https://s3.us.archive.org";
    private final static String ARCHIVE_DETAILS_ENDPOINT = "https://archive.org/details/";

	public static final MediaType MEDIA_TYPE = MediaType.parse("");

    private OkHttpClient client;

	public ArchiveSiteController(Context context, SiteControllerListener listener, String jobId) {
		super(context, listener, jobId);
        initClient(context);
	}

	private void initClient (Context context)
    {

        if (true)
        {
            this.client = new OkHttpClient.Builder().build();
        }
        else {

            try {

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
    }

	@Override
	public void startRegistration(Space space) {
		Intent intent = new Intent(mContext, ArchiveLoginActivity.class);
		intent.putExtra("register",true);
		intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, space.getPassword());
		((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE);
		// FIXME not a safe cast, context might be a service
	}

	@Override
	public void startAuthentication(Space space) {
		Intent intent = new Intent(mContext, ArchiveLoginActivity.class);
		intent.putExtra(SiteController.EXTRAS_KEY_CREDENTIALS, space.getPassword());
		((Activity) mContext).startActivityForResult(intent, SiteController.CONTROLLER_REQUEST_CODE);
		// FIXME not a safe cast, context might be a service
	}

    @Override
    public boolean upload(Space space, final Media media, HashMap<String, String> valueMap) {
        try {
            // starting from 3.1+, you can also use content:// URI string instead of absolute file
            String mediaUri = valueMap.get(VALUE_KEY_MEDIA_PATH);
            String mimeType = valueMap.get(VALUE_KEY_MIME_TYPE);
            String licenseUrl = valueMap.get(VALUE_KEY_LICENSE_URL);

            // TODO this should make sure we arn't accidentally using one of archive.org's metadata fields by accident
            String title = valueMap.get(VALUE_KEY_TITLE);
            String slug = valueMap.get(VALUE_KEY_SLUG);
            String tags = valueMap.get(VALUE_KEY_TAGS);
            String author = valueMap.get(VALUE_KEY_AUTHOR);
            String profileUrl = valueMap.get(VALUE_KEY_PROFILE_URL);
            String locationName = valueMap.get(VALUE_KEY_LOCATION_NAME);
            String body = valueMap.get(VALUE_KEY_BODY);

            String uploadBasePath;
            String uploadPath;

            String randomString = new Util.RandomString(4).nextString();
            uploadBasePath = slug + "-" + randomString;

             uploadPath = "/" + uploadBasePath + "/" + getTitleFileName(media);


            MediaType mediaType = mimeType == null ? null : MediaType.parse(mimeType);
            RequestBody requestBody = RequestBodyUtil.create(mContext.getContentResolver(), Uri.parse(mediaUri), media.getContentLength(), mediaType, new RequestListener() {


                long lastBytes = 0;

                @Override
                public void transferred(long bytes) {

                    if (bytes > lastBytes) {
                        jobProgress(bytes, null);
                        lastBytes = bytes;
                    }


                }

                @Override
                public boolean continueUpload() {
                    return mContinueUpload;
                }

                @Override
                public void transferComplete() {

                    String finalPath = ARCHIVE_DETAILS_ENDPOINT + uploadBasePath;
                    media.setServerUrl(finalPath);
                    jobSucceeded(finalPath);


                }
            });

            Headers.Builder headersBuilder = new Headers.Builder();
            headersBuilder.add("Accept", "*/*");
            headersBuilder.add("x-archive-auto-make-bucket","1");
            headersBuilder.add("x-amz-auto-make-bucket", "1");
            headersBuilder.add("x-archive-meta-language", "eng"); // FIXME set based on locale or selected
            headersBuilder.add("authorization", "LOW " + space.getUsername() + ":" + space.getPassword());

            if(!TextUtils.isEmpty(author)) {
                headersBuilder.add("x-archive-meta-author", author);
                if (profileUrl != null) {
                    headersBuilder.add("x-archive-meta-authorurl", profileUrl);
                }
            }

            if (mimeType != null) {
                headersBuilder.add("x-archive-meta-mediatype", mimeType);
                if(mimeType.contains("audio")) {
                    headersBuilder.add("x-archive-meta-collection", "opensource_audio");
                } else if (mimeType.contains("image")) {
                    headersBuilder.add("x-archive-meta-collection", "opensource_media");
                }
                else {
                    headersBuilder.add("x-archive-meta-collection", "opensource_movies");
                }
            } else {
                headersBuilder.add("x-archive-meta-collection", "opensource_media");
            }

            if (!TextUtils.isEmpty(locationName)) {
                headersBuilder.add("x-archive-meta-location", locationName);
            }

            if (!TextUtils.isEmpty(tags)) {
                String keywords = tags.replace(',', ';').replaceAll(" ", "");
                headersBuilder.add("x-archive-meta-subject", keywords);
            }

            if (!TextUtils.isEmpty(body)) {
                headersBuilder.add("x-archive-meta-description", body);
            }

            if (!TextUtils.isEmpty(title)) {
                headersBuilder.add("x-archive-meta-title", title);
            }

            if (!TextUtils.isEmpty(licenseUrl)) {
                headersBuilder.add("x-archive-meta-licenseurl", licenseUrl);
            }

		/*
		For uploads which need to be available ASAP in the content
  management system, an interactive user's upload for example,
  one can request interactive queue priority:
		 */
            headersBuilder.add("x-archive-interactive-priority","1");

            put (ARCHIVE_API_ENDPOINT + uploadPath, requestBody, headersBuilder.build());

            return true;

        } catch (Exception exc) {
            Log.e("AndroidUploadService", exc.getMessage(), exc);
        }

        return false;
    }

    private void put(String url, RequestBody requestBody, @NonNull Headers headers) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .put(requestBody)
                .headers(headers)
                .build();
        execute(request);
    }

    @Override
    public void cancel() {

        mContinueUpload = false;
    }


    private void execute(Request request) throws IOException {
        execute(request, (ResponseHandler<Void>) response -> {

            if (!response.isSuccessful()) {
                String message = "Error contacting " + response.request().url();
                throw new IOException(message + " = " + response.code() +": " + response.message());
            }
            else
            {
                Log.d(TAG,"successful PUT to: " + response.request().url());
            }

            return null;
        });
    }

    private <T> T execute(Request request, ResponseHandler<T> responseHandler) throws IOException {
        Response response = client.newCall(request).execute();
        return responseHandler.handleResponse(response);
    }

    private String getArchiveUploadEndpoint (String title, String slug, String mimeType)
    {
        String urlPath;
        String url;
        String ext;

        String randomString = new Util.RandomString(4).nextString();
        urlPath = slug + "-" + randomString;
        ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

        if (TextUtils.isEmpty(ext))
        {
            if (mimeType.startsWith("image"))
                ext = "jpg";
            else if (mimeType.startsWith("video"))
                ext = "mp4";
            else if (mimeType.startsWith("audio"))
                ext = "m4a";
            else
                ext = "txt";

        }

        try {
            url = "/" + urlPath + "/" + URLEncoder.encode(title,"UTF-8") + '.' + ext;


        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "Couldn't encode title",e);
            return null;
        }

        return url;

    }

    public static String getTitleFileName (Media media) {
        String filename = null;

        String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(media.getMimeType());

        if (TextUtils.isEmpty(ext))
        {
            if (media.getMimeType().startsWith("image"))
                ext = "jpg";
            else if (media.getMimeType().startsWith("video"))
                ext = "mp4";
            else if (media.getMimeType().startsWith("audio"))
                ext = "m4a";
            else
                ext = "txt";

        }

        try {
            filename = URLEncoder.encode(media.getTitle(),"UTF-8") + '.' + ext;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return filename;
    }

    @Override
    public boolean delete(Space space, String title, String mediaFile) {
        Log.d(TAG, "Upload file: Entering upload");

        /**
         *
         o DELETE normally deletes a single file, additionally all the
         derivatives and originals related to a file can be
         automatically deleted by specifying a header with the DELETE
         like so:
         x-archive-cascade-delete:1
         */

        // FIXME we are putting a random 4 char string in the bucket name for collision avoidance, we might want to do this differently?

        String mediaUrl = null;

        try {
            mediaUrl = ARCHIVE_API_ENDPOINT + '/' + URLEncoder.encode(title,"UTF-8") + '/' + mediaFile;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return false;
        }

        Log.d(TAG, "deleting url media item: " + mediaUrl);


        Request.Builder builder = new Request.Builder()
                .delete()
                .url(mediaUrl)
                .addHeader("Accept", "*/*")
                .addHeader("x-archive-cascade-delete", "1")
                .addHeader("x-archive-keep-old-version", "0")
                .addHeader("authorization", "LOW " + space.getUsername() + ":" + space.getPassword());

        Request request = builder.build();

        ArchiveServerTask deleteFileTask = new ArchiveServerTask(client, request);
        deleteFileTask.execute();

        return true;
    }

    @Override
    public ArrayList<File> getFolders(Space space, String path) throws IOException {
        return null;
    }

    /**
	@Override
	public boolean upload(Account account, Media media, HashMap<String, String> valueMap) {

	    //do nothing for now
        String result = uploadNew(media, account, valueMap);

		return (result != null);
	}**/


	class ArchiveServerTask extends AsyncTask<String, String, String> {
		private OkHttpClient client;
		private Request request;
		private Response response;

		public ArchiveServerTask(OkHttpClient client, Request request) {
			this.client = client;
			this.request = request;
		}

		@Override
		protected String doInBackground(String... params) {
			Log.d(TAG, "Begin Upload");

			try {
				/**
			    int timeout = 60 * 1000 * 2; //2 minute timeout!

				client.setConnectTimeout(timeout, TimeUnit.MILLISECONDS);
				client.setWriteTimeout(timeout, TimeUnit.MILLISECONDS);
                client.setReadTimeout(timeout, TimeUnit.MILLISECONDS);

				 	**/

				response = client.newCall(request).execute();
                Log.d(TAG, "response: " + response + ", body: " + response.body().string());
				if (!response.isSuccessful()) {
					jobFailed(null, 4000001, "Archive upload failed: Unexpected Response Code: " + "response: " + response.code() + ": message=" + response.message());
				} else {	
				    jobSucceeded(response.request().toString());
				}
			} catch (IOException e) {
				jobFailed(e, 4000002, "Archive upload failed: IOException");
				if (response != null && response.body() != null) {
					try {
						Log.d(TAG, response.body().string());
					} catch (IOException e1) {
						Log.d(TAG, "exception: " + e1.getLocalizedMessage() + ", stacktrace: " + e1.getStackTrace());
					}
				}
				else
				{



				}
			}

			return "-1";
		}
	}

    @Override
    public void startMetadataActivity(Intent intent) {
//        get the intent extras and launch the new intent with them
    }

    public static String getSlug(String title) {
        return title.replaceAll("[^A-Za-z0-9]", "-");
    }

    public static HashMap<String, String> getMediaMetadata(Context context, Media mMedia) {

        HashMap<String, String> valueMap = new HashMap<String, String>();
        SharedPreferences sharedPref = context.getSharedPreferences(Globals.PREF_FILE_KEY, Context.MODE_PRIVATE);

        valueMap.put(SiteController.VALUE_KEY_MEDIA_PATH, mMedia.getOriginalFilePath());
        valueMap.put(SiteController.VALUE_KEY_MIME_TYPE, mMedia.getMimeType());
        valueMap.put(SiteController.VALUE_KEY_SLUG, getSlug(mMedia.getTitle()));
        valueMap.put(SiteController.VALUE_KEY_TITLE, mMedia.getTitle());

        if (!TextUtils.isEmpty(mMedia.getLicenseUrl()))
            valueMap.put(SiteController.VALUE_KEY_LICENSE_URL, mMedia.getLicenseUrl());
        else
            valueMap.put(SiteController.VALUE_KEY_LICENSE_URL, "https://creativecommons.org/licenses/by/4.0/");

        if (!TextUtils.isEmpty(mMedia.getTags())) {
            String tags = context.getString(R.string.default_tags) + ";" + mMedia.getTags(); // FIXME are keywords/tags separated by spaces or commas?
            valueMap.put(SiteController.VALUE_KEY_TAGS, tags);
        }

        if (!TextUtils.isEmpty(mMedia.getAuthor()))
            valueMap.put(SiteController.VALUE_KEY_AUTHOR, mMedia.getAuthor());

        if (!TextUtils.isEmpty(mMedia.getLocation()))
            valueMap.put(SiteController.VALUE_KEY_LOCATION_NAME, mMedia.getLocation()); // TODO

        if (!TextUtils.isEmpty(mMedia.getDescription()))
            valueMap.put(SiteController.VALUE_KEY_BODY, mMedia.getDescription());

        return valueMap;
    }
}
