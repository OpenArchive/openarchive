package net.opendasharchive.openarchive.services.dropbox;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.google.common.net.UrlEscapers;
import com.google.gson.Gson;

import net.opendasharchive.openarchive.db.Media;
import net.opendasharchive.openarchive.db.Space;
import net.opendasharchive.openarchive.util.Globals;
import net.opendasharchive.openarchive.util.Prefs;

import org.witness.proofmode.ProofMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import io.scal.secureshareui.controller.SiteController;
import io.scal.secureshareui.controller.SiteControllerListener;

public class DropboxSiteController extends SiteController {


    public static final String SITE_NAME = "Dropbox";
    public static final String SITE_KEY = "dropbox";

    private final static String TAG = "dbx";

    private boolean mContinueUpload = true;

    private DropboxClientFactory dbClient = null;
    private UploadFileTask uTask;

    private SimpleDateFormat dateFormat;


    public DropboxSiteController(Context context, SiteControllerListener listener, String jobId) {
        super(context, listener, jobId);
        dateFormat = new SimpleDateFormat(Globals.FOLDER_DATETIME_FORMAT);

    }

    @Override
    public void startRegistration(Space space) {

        //not supported
    }

    @Override
    public void startAuthentication(Space space) {

        space.host = "dropbox.com";

        String accessToken = space.password;

        if (!TextUtils.isEmpty(accessToken)) {
            dbClient = new DropboxClientFactory();
            dbClient.init(mContext, accessToken);
        }

    }

    @Override
    public void startMetadataActivity(Intent intent) {

    }


    @Override
    public void cancel() {

        mContinueUpload = false;

        if (uTask != null)
            uTask.cancel();
    }

    @Override
    public boolean upload(Space space, final Media media, HashMap<String, String> valueMap) throws IOException {

        startAuthentication(space);

        Uri mediaUri = Uri.parse(valueMap.get(VALUE_KEY_MEDIA_PATH));

        String projectName = media.getServerUrl();
        String folderName = dateFormat.format(media.updateDate);
        String fileName = getUploadFileName(media.getTitle(), media.getMimeType());

        if (media.contentLength == 0) {
            File fileMedia = new File(mediaUri.getPath());
            if (fileMedia.exists())
                media.contentLength = fileMedia.length();
        }

        try {

            uTask = new UploadFileTask(mContext, dbClient.getClient(), new UploadFileTask.Callback() {
                @Override
                public void onUploadComplete(FileMetadata result) {

                    if (result != null) {
                        String finalMediaPath = result.getPathDisplay();

                        media.setServerUrl(finalMediaPath);
                        media.save();
                        jobSucceeded(finalMediaPath);

                        uploadMetadata(media, projectName, folderName, fileName);

                        if (Prefs.getUseProofMode())
                            uploadProof(media, projectName, folderName);
                    }
                }

                @Override
                public void onError(Exception e) {

                    if (e != null)
                        jobFailed(e, -1, e.getMessage());
                    else
                        jobFailed(new Exception("unknown error"),-1,"unknown error");
                }

                @Override
                public void onProgress(long progress) {
                    jobProgress(progress,"");
                }
            });

            uTask.upload(mediaUri.toString(),fileName,folderName,projectName);


            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed primary media upload: " + e.getMessage());
            jobFailed(e, -1, "Failed primary media upload");
            return false;
        }

    }


    private boolean uploadMetadata (final Media media, String projectName, String folderName, String fileName)
    {
        String metadataFileName = fileName + ".meta.json";
        //String urlMeta = basePath + '/' + metadataFileName;

        Gson gson = new Gson();
        String json = gson.toJson(media,Media.class);

        try {

            File fileMetaData = new File(mContext.getFilesDir(),metadataFileName);
            FileOutputStream fos = new FileOutputStream(fileMetaData);
            fos.write(json.getBytes());
            fos.flush();
            fos.close();

            UploadFileTask uTask = new UploadFileTask(mContext, dbClient.getClient(), new UploadFileTask.Callback() {
                @Override
                public void onUploadComplete(FileMetadata result) {

                }

                @Override
                public void onError(Exception e) {

                }

                @Override
                public void onProgress(long progress) {

                }


            });

            uTask.upload(Uri.fromFile(fileMetaData).toString(),metadataFileName, folderName, projectName);

            if (Prefs.getUseProofMode()) {
                Prefs.putBoolean(ProofMode.PREF_OPTION_LOCATION, false);
                Prefs.putBoolean(ProofMode.PREF_OPTION_NETWORK, false);

                String metaMediaHash = ProofMode.generateProof(mContext, Uri.fromFile(fileMetaData));
                File fileProofDir = ProofMode.getProofDir(metaMediaHash);
                if (fileProofDir != null && fileProofDir.exists()) {
                    File[] filesProof = fileProofDir.listFiles();
                    for (File fileProof : filesProof) {
                        uTask = new UploadFileTask(mContext, dbClient.getClient(), new UploadFileTask.Callback() {
                            @Override
                            public void onUploadComplete(FileMetadata result) {

                            }

                            @Override
                            public void onError(Exception e) {

                            }

                            @Override
                            public void onProgress(long progress) {

                            }
                        });

                        uTask.upload(Uri.fromFile(fileProof).toString(), fileProof.getName(), folderName, projectName);
                    }

                }
            }

            return true;
        }
        catch (IOException e)
        {
            Log.e(TAG, "Failed primary media upload: " + metadataFileName,e);
            jobFailed(e,-1,metadataFileName);

        }

        return false;

    }


    private boolean uploadProof (Media media, String projectName, String folderName)
    {

        try {

            UploadFileTask  uTask = new UploadFileTask(mContext, dbClient.getClient(), new UploadFileTask.Callback() {
                @Override
                public void onUploadComplete(FileMetadata result) {

                }

                @Override
                public void onError(Exception e) {

                }

                @Override
                public void onProgress(long progress) {

                }
            });

            if (media.getMediaHash() != null) {
                String mediaHash = new String(media.getMediaHash());
                if (!TextUtils.isEmpty(mediaHash)) {
                    File fileProofDir = ProofMode.getProofDir(mediaHash);
                    if (fileProofDir != null && fileProofDir.exists()) {
                        File[] filesProof = fileProofDir.listFiles();
                        for (File fileProof : filesProof)
                            uTask.upload(Uri.fromFile(fileProof).toString(), fileProof.getName(), folderName, projectName);

                    }
                }

                return true;
            }
        }
        catch (Exception e)
        {
            //proof upload failed
            Log.e(TAG, "Failed proof upload: " + media,e);
        }

        return false;
    }

    @Override
    public boolean delete(Space space, String bucketName, String mediaFile) {

        String url = bucketName;
        try {
           // sardine.delete(url);
            return true;
        } catch (Exception e) {
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

        result.append(UrlEscapers.urlFragmentEscaper().escape(title));

        if (!title.endsWith(ext))
            result.append('.').append(ext);

        return result.toString();

    }

    private final static String FILE_BASE = "files/";

    public ArrayList<File> getFolders (Space space, String path) throws IOException
    {
        startAuthentication(space);

        ArrayList<File> listFiles = new ArrayList<>();

        try {
            ListFolderResult result = dbClient.getClient().files().listFolder("");

            for (Metadata md : result.getEntries())
            {
                String fileOrFolder = md.getPathLower();
                if(!fileOrFolder.contains(".")) {
                    listFiles.add(new File(fileOrFolder));
                }
            }

        } catch (DbxException e) {
            e.printStackTrace();
        }


        return listFiles;


    }

}
