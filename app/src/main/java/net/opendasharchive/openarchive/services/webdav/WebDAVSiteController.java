package net.opendasharchive.openarchive.services.webdav;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.SardineListener;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Project;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.Prefs;

import org.witness.proofmode.ProofMode;
import org.witness.proofmode.crypto.PgpUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import info.guardianproject.netcipher.client.StrongBuilder;
import info.guardianproject.netcipher.client.StrongOkHttpClientBuilder;
import info.guardianproject.netcipher.proxy.OrbotHelper;
import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SiteControllerListener;
import okhttp3.OkHttpClient;

public class WebDAVSiteController extends SiteController {

    private Sardine sardine;
    private String server;

    public static final String SITE_NAME = "WebDAV";
    public static final String SITE_KEY = "webdav";

    private final static String TAG = "WebDAVSC";

    private boolean mContinueUpload = true;
    private SimpleDateFormat dateFormat;

    public WebDAVSiteController (Context context, SiteControllerListener listener, String jobId) throws Exception {
        super(context, listener, jobId);

        dateFormat = new SimpleDateFormat(Globals.FOLDER_DATETIME_FORMAT);

        if (Prefs.getUseTor() && OrbotHelper.isOrbotInstalled(context)) {

            StrongOkHttpClientBuilder builder = new StrongOkHttpClientBuilder(context);
            builder.withBestProxy().build(new StrongBuilder.Callback<OkHttpClient>() {
                @Override
                public void onConnected(OkHttpClient okHttpClient) {

                    sardine = new OkHttpSardine(okHttpClient);
                }

                @Override
                public void onConnectionException(Exception e) {

                    Message msg = new Message();
                    msg.getData().putInt(SiteController.MESSAGE_KEY_CODE, 500);
                    msg.getData().putString(SiteController.MESSAGE_KEY_MESSAGE, "Unable to connect to Orbot/Tor: " + e.getMessage());
                    listener.failure(msg);
                }

                @Override
                public void onTimeout() {
                    Message msg = new Message();
                    msg.getData().putInt(SiteController.MESSAGE_KEY_CODE, 500);
                    msg.getData().putString(SiteController.MESSAGE_KEY_MESSAGE, "Unable to connect to Orbot/Tor: TIMEOUT");
                    listener.failure(msg);
                }

                @Override
                public void onInvalid() {
                    Message msg = new Message();
                    msg.getData().putInt(SiteController.MESSAGE_KEY_CODE, 500);
                    msg.getData().putString(SiteController.MESSAGE_KEY_MESSAGE, "Unable to connect to Orbot/Tor: INVALID");
                    listener.failure(msg);
                }
            });

            while (sardine == null) {
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                }
                Log.d(TAG,"waiting for Tor-enabled Sardine to init");
            }

        }
        else
        {
            sardine = new OkHttpSardine();
        }

    }

    @Override
    public void startRegistration(Space space) {

    }

    @Override
    public void startAuthentication(Space space) {

        if (sardine != null) {
            sardine.setCredentials(space.username, space.password);
            server = space.host;
        }
    }

    @Override
    public void startMetadataActivity(Intent intent) {

    }

    private void listFolders (String url) throws IOException {
        if (sardine != null) {
            List<DavResource> listFiles = sardine.list(url);

            for (DavResource resource : listFiles) {
                Log.d(TAG, "resource: " + resource.getName() + ":" + resource.getPath());
            }
        }
        else
        {
            throw new IOException("client not init'd");
        }
    }

    @Override
    public void cancel() {

        mContinueUpload = false;
    }

    @Override
    public boolean upload(Space space, final Media media, HashMap<String, String> valueMap) throws IOException {

        if (sardine == null)
            throw new IOException(("client not init'd"));

        if (Prefs.useNextcloudChunking())
            return uploadUsingChunking(space, media, valueMap);
        else {
            startAuthentication(space);

            Uri mediaUri = Uri.parse(valueMap.get(VALUE_KEY_MEDIA_PATH));

            String basePath = media.getServerUrl();

            String folderName = dateFormat.format(media.updateDate);
            String fileName = getUploadFileName(media.getTitle(), media.getMimeType());

            StringBuffer projectFolderBuilder = new StringBuffer();//server + '/' + basePath;
            projectFolderBuilder.append(server.replace("webdav", "dav"));

            if (!server.endsWith("/"))
                projectFolderBuilder.append('/');
            projectFolderBuilder.append("files/");
            projectFolderBuilder.append(space.username).append('/');
            projectFolderBuilder.append(basePath);

            String projectFolderPath = projectFolderBuilder.toString();

            if (media.contentLength == 0) {
                File fileMedia = new File(mediaUri.getPath());
                if (fileMedia.exists())
                    media.contentLength = fileMedia.length();
            }

            if (media.mediaHash == null) {

            }

            String finalMediaPath = null;

            try {
                if (!sardine.exists(projectFolderPath))
                    sardine.createDirectory(projectFolderPath);

                projectFolderPath += '/' + folderName;
                if (!sardine.exists(projectFolderPath))
                    sardine.createDirectory(projectFolderPath);

                finalMediaPath = projectFolderPath + '/' + fileName;

                if (!sardine.exists(finalMediaPath)) {
                    sardine.put(mContext.getContentResolver(), finalMediaPath, mediaUri, media.contentLength, media.getMimeType(), false,
                            new SardineListener() {

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
                            });

                    media.setServerUrl(finalMediaPath);
                    jobSucceeded(finalMediaPath);

                    uploadMetadata(media, projectFolderPath, fileName);
                    uploadProof(media, projectFolderPath);

                } else {
                    media.setServerUrl(finalMediaPath);
                    jobSucceeded(finalMediaPath);

                }

                return true;
            } catch (IOException e) {
                Log.w(TAG, "Failed primary media upload: " + finalMediaPath + ": " + e.getMessage());
                jobFailed(e, -1, finalMediaPath);
                return false;
            }
        }
    }

    int chunkStartIdx = 0;


    public boolean uploadUsingChunking (Space space, final Media media, HashMap<String, String> valueMap) throws IOException {


        if (sardine == null)
            throw new IOException(("client not init'd"));

        startAuthentication(space);

        Uri mediaUri = Uri.parse(valueMap.get(VALUE_KEY_MEDIA_PATH));
        String fileName = getUploadFileName(media.getTitle(),media.getMimeType());
        String folderName = dateFormat.format(media.updateDate);

        String chunkFolderPath = media.getServerUrl() + "-" + UrlEscapers.urlFragmentEscaper().escape(fileName);

        StringBuffer projectFolderBuilder = new StringBuffer();//server + '/' + basePath;
        projectFolderBuilder.append(server.replace("webdav","dav"));
        if (!server.endsWith("/"))
            projectFolderBuilder.append('/');
        projectFolderBuilder.append("uploads/");
        projectFolderBuilder.append(space.username).append('/');
        projectFolderBuilder.append(chunkFolderPath);

        String projectFolderPath = projectFolderBuilder.toString();

        if (media.contentLength == 0)
        {
            File fileMedia = new File(mediaUri.getPath());
            if (fileMedia.exists())
                media.contentLength = fileMedia.length();
        }


        String tmpMediaPath = projectFolderPath;

        try {
            if (!sardine.exists(projectFolderPath))
                sardine.createDirectory(projectFolderPath);

            //create chunks and start uploads; look for existing chunks, and skip if done; start with the last chunk and reupload

            int chunkSize = 1024 * 2000;
            int bufferSize = 1024 * 4;
            byte[] buffer = new byte[bufferSize];
            chunkStartIdx = 0;

            InputStream is = mContext.getContentResolver().openInputStream(mediaUri);

            while (chunkStartIdx < media.contentLength) {

                ByteArrayOutputStream baos = new ByteArrayOutputStream();

                int i = is.read(buffer);

                int totalBytes = chunkStartIdx + i;

                while (i != -1) {
                    baos.write(buffer);

                    if (baos.size() > chunkSize)
                        break;

                    i = is.read(buffer);
                    if (i != -1)
                        totalBytes += i;
                }

                String chunkPath = tmpMediaPath + "/" + "chunk-" + chunkStartIdx + "-" + totalBytes;

                boolean chunkExists = sardine.exists(chunkPath);
                boolean chunkLengthMatches = false;

                if (chunkExists)
                {
                    List<DavResource> listDav = sardine.list(chunkPath);
                    chunkLengthMatches = listDav.get(0).getContentLength() >= chunkSize;
                }

                if ((!chunkExists)||(!chunkLengthMatches)) {
                    sardine.put(chunkPath, baos.toByteArray(), media.getMimeType(), new SardineListener() {

                        @Override
                        public void transferred(long bytes) {
                                jobProgress((long)chunkStartIdx+bytes, null);
                        }

                        @Override
                        public boolean continueUpload() {
                            return mContinueUpload;
                        }
                    });
                }

                jobProgress(totalBytes,null);

                chunkStartIdx = totalBytes + 1;
            }

            fileName = getUploadFileName(media.getTitle(),media.getMimeType());

            projectFolderBuilder = new StringBuffer();//server + '/' + basePath;
            projectFolderBuilder.append(server.replace("webdav","dav"));

            if (!server.endsWith("/"))
                projectFolderBuilder.append('/');
            projectFolderBuilder.append("files/");
            projectFolderBuilder.append(UrlEscapers.urlFragmentEscaper().escape(space.username)).append('/');
            projectFolderBuilder.append(UrlEscapers.urlFragmentEscaper().escape(media.getServerUrl()));

            projectFolderPath = projectFolderBuilder.toString();

            if (!sardine.exists(projectFolderPath))
                sardine.createDirectory(projectFolderPath);

            projectFolderPath += '/' + folderName;
            if (!sardine.exists(projectFolderPath))
                sardine.createDirectory(projectFolderPath);

            //UrlEscapers.urlFragmentEscaper().escape(inputString);

            String finalMediaPath = projectFolderPath + '/' + fileName;

            sardine.move(tmpMediaPath + "/.file",finalMediaPath);

            media.setServerUrl(finalMediaPath);
            jobSucceeded(finalMediaPath);

            uploadMetadata (media, projectFolderPath, fileName);

            if (Prefs.getUseProofMode())
                uploadProof(media, projectFolderPath);


            return true;
        } catch (IOException e) {

            sardine.delete(tmpMediaPath);

            Log.w(TAG, "Failed primary media upload: " + tmpMediaPath + ": " + e.getMessage());
            jobFailed(e,-1,tmpMediaPath);
            return false;
        }

    }


    private boolean uploadMetadata (final Media media, String basePath, String fileName)
    {

        //update to the latest project license
        Project project = Project.getById(media.getProjectId());
        media.setLicenseUrl(project.getLicenseUrl());

        String urlMeta = basePath + '/' + fileName + ".meta.json";
        Gson gson = new Gson();
        String json = gson.toJson(media,Media.class);

        try {

            File fileMetaData = new File(mContext.getFilesDir(),fileName+".meta.json");
            FileOutputStream fos = new FileOutputStream(fileMetaData);
            fos.write(json.getBytes());
            fos.flush();
            fos.close();
            sardine.put(urlMeta, fileMetaData, "text/plain", false, null);

            if (Prefs.getUseProofMode()) {
                Prefs.putBoolean(ProofMode.PREF_OPTION_LOCATION, false);
                Prefs.putBoolean(ProofMode.PREF_OPTION_NETWORK, false);

                String metaMediaHash = ProofMode.generateProof(mContext, Uri.fromFile(fileMetaData));
                File fileProofDir = ProofMode.getProofDir(metaMediaHash);
                if (fileProofDir != null && fileProofDir.exists()) {
                    File[] filesProof = fileProofDir.listFiles();
                    for (File fileProof : filesProof) {
                        sardine.put(basePath + '/' + fileProof.getName(), fileProof, "text/plain", false, null);
                    }

                }
            }

            return true;
        }
        catch (IOException e)
        {
            Log.e(TAG, "Failed primary media upload: " + urlMeta,e);
            jobFailed(e,-1,urlMeta);

        }

        return false;

    }


    private boolean uploadProof (Media media, String basePath)
    {
        String lastUrl = null;

        try {

            if (media.getMediaHash() != null) {
                String mediaHash = new String(media.getMediaHash());
                if (!TextUtils.isEmpty(mediaHash)) {
                    File fileProofDir = ProofMode.getProofDir(mediaHash);
                    if (fileProofDir != null && fileProofDir.exists()) {
                        File[] filesProof = fileProofDir.listFiles();
                        for (File fileProof : filesProof) {
                            lastUrl = basePath + fileProof.getName();
                            sardine.put(lastUrl, fileProof, "text/plain", false, null);
                        }

                    }

                    PgpUtils mPgpUtils = PgpUtils.getInstance(mContext,PgpUtils.DEFAULT_PASSWORD);
                    String pubKey = mPgpUtils.getPublicKey();
                    String keyPath =  basePath + "/proofmode.pubkey";
                    sardine.put(keyPath,pubKey.getBytes(),"text/plain",null);
                }

                return true;
            }
        }
        catch (Exception e)
        {
            //proof upload failed
            Log.e(TAG, "Failed proof upload: " + lastUrl,e);
        }

        return false;
    }

    @Override
    public boolean delete(Space space, String bucketName, String mediaFile) {

        String url = bucketName;
        try {
            sardine.delete(url);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getUploadFileName (String title, String mimeType)
    {
        StringBuffer result = new StringBuffer();
        String ext;

    //    String randomString = new Util.RandomString(4).nextString();
     //   result.append(randomString).append('-');
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

        result.append(title);

        if (!title.endsWith(ext))
            result.append('.').append(ext);


        return result.toString();

    }

    private final static String FILE_BASE = "files/";

    public ArrayList<File> getFolders (Space space, String path) throws IOException
    {
        startAuthentication(space);

        ArrayList<File> listFiles = new ArrayList<>();

        path = path.replace("webdav", "dav");

        StringBuffer sbFolderPath = new StringBuffer();
        sbFolderPath.append(path);
        sbFolderPath.append(FILE_BASE);
        sbFolderPath.append(space.username).append('/');

        String baseFolderPath = sbFolderPath.toString();
        List<DavResource> listFolders = sardine.list(baseFolderPath);

        for (DavResource folder : listFolders)
        {
            if (folder.isDirectory()) {

                String folderPath = folder.getPath();

                if (baseFolderPath.endsWith(folderPath))
                    continue; //this is the root folder... don't include it in the list

                File fileFolder = new File(folderPath);


                Date folderMod = folder.getModified();

                if (folderMod != null)
                    fileFolder.setLastModified(folderMod.getTime());
                else
                    fileFolder.setLastModified(new Date().getTime());

                listFiles.add(fileFolder);
            }
        }


        return listFiles;


    }

}
